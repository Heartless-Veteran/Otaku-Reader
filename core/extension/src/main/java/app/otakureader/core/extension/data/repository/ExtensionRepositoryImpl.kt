package app.otakureader.core.extension.data.repository

import app.otakureader.core.extension.data.local.ExtensionDao
import app.otakureader.core.extension.data.local.ExtensionMapper
import app.otakureader.core.extension.data.remote.ExtensionRemoteDataSource
import app.otakureader.core.extension.domain.model.Extension
import app.otakureader.core.extension.domain.model.InstallStatus
import app.otakureader.core.extension.domain.repository.ExtensionRepository
import app.otakureader.core.extension.loader.ExtensionLoadResult
import app.otakureader.core.extension.loader.ExtensionLoader
import app.otakureader.core.extension.blocklist.ExtensionBlocklistStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException

/**
 * Implementation of ExtensionRepository.
 * Coordinates between local database and remote API.
 */
class ExtensionRepositoryImpl(
    private val localDataSource: ExtensionDao,
    private val remoteDataSource: ExtensionRemoteDataSource,
    private val extensionLoader: ExtensionLoader,
    private val blocklistStore: ExtensionBlocklistStore? = null,
    private val mapper: ExtensionMapper = ExtensionMapper()
) : ExtensionRepository {
    
    override fun getInstalledExtensions(): Flow<List<Extension>> {
        return localDataSource.getInstalledExtensions()
            .map { entities ->
                entities.map { mapper.toDomain(it) }
            }
    }
    
    override fun getAvailableExtensions(): Flow<List<Extension>> {
        val available = localDataSource.getAvailableExtensions()
            .map { entities ->
                entities.map { mapper.toDomain(it) }
            }
        // Blocklist enforcement (#1018): blocked packages never appear as installable.
        val store = blocklistStore ?: return available
        return combine(available, store.blockedPackages) { extensions, blocked ->
            extensions.filter { it.pkgName !in blocked }
        }
    }
    
    override fun getExtensionsWithUpdates(): Flow<List<Extension>> {
        return localDataSource.getExtensionsWithUpdates()
            .map { entities ->
                entities.map { mapper.toDomain(it) }
            }
    }
    
    override suspend fun getExtension(pkgName: String): Extension? {
        return localDataSource.getExtensionByPkgName(pkgName)?.let {
            mapper.toDomain(it)
        }
    }
    
    override suspend fun getExtensionById(id: Long): Extension? {
        return localDataSource.getExtensionById(id)?.let {
            mapper.toDomain(it)
        }
    }
    
    override suspend fun installExtension(pkgName: String, apkPath: String): Result<Extension> =
        installExtensionInternal(pkgName, apkPath, fallback = null)

    override suspend fun installExtension(extension: Extension, apkPath: String): Result<Extension> =
        installExtensionInternal(extension.pkgName, apkPath, fallback = extension)

    /**
     * Shared install logic. Prefers the existing database row (it carries repo-sourced
     * metadata like apkUrl, iconUrl, and first-seen repo provenance); when no row exists
     * and [fallback] is provided, a new row is created from the loaded extension instead
     * of failing — this covers installs from a just-added repository whose refresh hasn't
     * synced to the database yet.
     */
    private suspend fun installExtensionInternal(
        pkgName: String,
        apkPath: String,
        fallback: Extension?,
    ): Result<Extension> {
        return try {
            // Blocklist enforcement (#1018): refuse install even if the caller bypassed
            // the filtered available list (e.g. a stale UI snapshot).
            val blockedReason = blocklistStore?.blockedPackages?.first()?.get(pkgName)
            if (blockedReason != null) {
                localDataSource.updateStatus(pkgName, InstallStatus.AVAILABLE.name)
                return Result.failure(
                    SecurityException("Extension $pkgName is blocklisted: $blockedReason")
                )
            }
            // Update status to installing
            localDataSource.updateStatus(pkgName, InstallStatus.INSTALLING.name)

            val existing = localDataSource.getExtensionByPkgName(pkgName)
            val extension = when {
                existing != null -> existing.copy(
                    status = InstallStatus.INSTALLED.name,
                    apkPath = apkPath,
                    installDate = System.currentTimeMillis(),
                    // Backfill provenance for rows created before the column existed (#1019)
                    // so the cross-repo replacement guard is active immediately.
                    sourceRepoUrl = existing.sourceRepoUrl ?: fallback?.repoUrl
                )
                fallback != null -> mapper.toEntity(
                    fallback.copy(
                        status = InstallStatus.INSTALLED,
                        apkPath = apkPath,
                        installDate = System.currentTimeMillis()
                    )
                )
                else -> return Result.failure(Exception("Extension not found: $pkgName"))
            }

            localDataSource.insertExtension(extension)
            Result.success(mapper.toDomain(extension))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            localDataSource.updateStatus(pkgName, InstallStatus.ERROR.name)
            Result.failure(e)
        }
    }
    
    override suspend fun uninstallExtension(pkgName: String): Result<Unit> {
        return try {
            localDataSource.updateStatus(pkgName, InstallStatus.UNINSTALLING.name)
            localDataSource.deleteByPkgName(pkgName)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateExtension(pkgName: String, apkPath: String): Result<Extension> {
        return try {
            localDataSource.updateStatus(pkgName, InstallStatus.UPDATING.name)
            
            val existing = localDataSource.getExtensionByPkgName(pkgName)
                ?: return Result.failure(Exception("Extension not found: $pkgName"))
            
            val updated = existing.copy(
                status = InstallStatus.INSTALLED.name,
                apkPath = apkPath,
                versionCode = existing.remoteVersionCode ?: existing.versionCode
            )
            
            localDataSource.insertExtension(updated)
            Result.success(mapper.toDomain(updated))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            localDataSource.updateStatus(pkgName, InstallStatus.ERROR.name)
            Result.failure(e)
        }
    }
    
    override suspend fun checkForUpdates(): Int {
        return try {
            val remoteResult = remoteDataSource.fetchAvailableExtensions()
            if (remoteResult.isFailure) return 0
            
            val remoteExtensions = remoteResult.getOrThrow()
            val installed = localDataSource.getInstalledExtensions().first()
            
            var updateCount = 0
            
            installed.forEach { localExt ->
                val candidates = remoteExtensions.filter { it.pkgName == localExt.pkgName }
                // Provenance guard (#1019): prefer the listing from the repository this
                // extension was originally installed from. A different repository offering
                // the same package name must not silently become the update source — that
                // is the cross-repo replacement attack this guard exists to stop.
                val sameRepo = candidates.firstOrNull {
                    localExt.sourceRepoUrl != null && it.repoUrl == localExt.sourceRepoUrl
                }
                val remoteExt = sameRepo
                    ?: candidates.firstOrNull().takeIf { localExt.sourceRepoUrl == null }
                if (remoteExt == null) {
                    if (candidates.isNotEmpty()) {
                        android.util.Log.w(
                            "ExtensionRepository",
                            "Skipping update for ${localExt.pkgName}: offered only by " +
                                "${candidates.mapNotNull { it.repoUrl }} but installed from " +
                                "${localExt.sourceRepoUrl}"
                        )
                    }
                    return@forEach
                }
                // First sighting of a pre-provenance install: adopt this repo as baseline.
                remoteExt.repoUrl?.let { localDataSource.backfillSourceRepoUrl(localExt.pkgName, it) }
                if (remoteExt.versionCode > localExt.versionCode) {
                    localDataSource.updateRemoteVersion(localExt.pkgName, remoteExt.versionCode)
                    localDataSource.updateStatus(localExt.pkgName, InstallStatus.HAS_UPDATE.name)
                    updateCount++
                }
            }
            
            updateCount
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            0
        }
    }
    
    override suspend fun setExtensionStatus(pkgName: String, status: InstallStatus) {
        localDataSource.updateStatus(pkgName, status.name)
    }

    override suspend fun setExtensionEnabled(pkgName: String, enabled: Boolean) {
        localDataSource.updateEnabled(pkgName, enabled)
    }

    override suspend fun trustExtension(pkgName: String): Result<Unit> = runCatching {
        val ext = localDataSource.getExtensionByPkgName(pkgName)
            ?: error("Extension $pkgName not found")
        val hash = ext.signatureHash ?: run {
            // Hash not yet in DB (e.g. installed from a minified-index repo that doesn't
            // include the signing-cert hash). Load the installed APK to compute it.
            val apkPath = ext.apkPath
                ?: error("Extension $pkgName has no installed APK — install it before trusting")
            val loadResult = extensionLoader.loadExtension(apkPath)
            when (loadResult) {
                is ExtensionLoadResult.Untrusted -> loadResult.extension.signatureHash
                is ExtensionLoadResult.Success -> loadResult.extension.signatureHash
                is ExtensionLoadResult.Error ->
                    error("Cannot read extension to determine its signature: ${loadResult.message}")
            } ?: error("Extension $pkgName has no signature hash — cannot trust")
        }
        extensionLoader.trustExtension(hash)
        // Persist the hash so isTrusted reflects the new state immediately and future
        // trust/revoke calls don't need to re-parse the APK.
        if (ext.signatureHash == null) {
            localDataSource.updateSignatureHash(pkgName, hash)
        }
    }

    override suspend fun revokeExtensionTrust(pkgName: String): Result<Unit> = runCatching {
        val ext = localDataSource.getExtensionByPkgName(pkgName)
            ?: error("Extension $pkgName not found")
        val hash = ext.signatureHash
            ?: error("Extension $pkgName has no signature hash — cannot revoke")
        extensionLoader.revokeExtensionTrust(hash)
    }

    override suspend fun refreshAvailableExtensions(): Result<Unit> {
        return try {
            val remoteResult = remoteDataSource.fetchAvailableExtensions()
            if (remoteResult.isFailure) {
                return Result.failure(remoteResult.exceptionOrNull() ?: Exception("Unknown error fetching extensions"))
            }
            
            val remoteExtensions = remoteResult.getOrThrow()
            val installed = localDataSource.getInstalledExtensions().first()
            // ERROR rows are failed installs, not installed extensions — they must NOT be
            // filtered out here, or the extension would vanish from the Available tab and
            // become un-reinstallable. The refresh below replaces them with a fresh
            // AVAILABLE row.
            val installedPkgNames = installed
                .filter { it.status != InstallStatus.ERROR.name }
                .map { it.pkgName }
                .toSet()

            // Filter out already installed extensions, mark as AVAILABLE
            val availableExtensions = remoteExtensions
                .filter { it.pkgName !in installedPkgNames }
                .map {
                    mapper.toEntity(it.copy(status = InstallStatus.AVAILABLE))
                }

            // Atomically clear old available extensions and insert the fresh list.
            // A plain upsert would leave stale entries behind forever when a repository
            // is removed or stops offering an extension.
            localDataSource.replaceAllAvailable(availableExtensions)
            
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun searchExtensions(query: String): Flow<List<Extension>> {
        return localDataSource.searchExtensions(query)
            .map { entities ->
                entities.map { mapper.toDomain(it) }
            }
    }
}
