package app.otakureader.core.extension.data.repository

import app.otakureader.core.extension.data.local.ExtensionDao
import app.otakureader.core.extension.data.local.ExtensionMapper
import app.otakureader.core.extension.data.remote.ExtensionRemoteDataSource
import app.otakureader.core.extension.domain.model.Extension
import app.otakureader.core.extension.domain.model.InstallStatus
import app.otakureader.core.extension.domain.repository.ExtensionRepository
import app.otakureader.core.extension.loader.ExtensionLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    private val mapper: ExtensionMapper = ExtensionMapper()
) : ExtensionRepository {
    
    override fun getInstalledExtensions(): Flow<List<Extension>> {
        return localDataSource.getInstalledExtensions()
            .map { entities ->
                entities.map { mapper.toDomain(it) }
            }
    }
    
    override fun getAvailableExtensions(): Flow<List<Extension>> {
        return localDataSource.getAvailableExtensions()
            .map { entities ->
                entities.map { mapper.toDomain(it) }
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
    
    override suspend fun installExtension(pkgName: String, apkPath: String): Result<Extension> {
        return try {
            // Update status to installing
            localDataSource.updateStatus(pkgName, InstallStatus.INSTALLING.name)
            
            // Get existing or create new extension record
            val existing = localDataSource.getExtensionByPkgName(pkgName)
            val extension = existing?.copy(
                status = InstallStatus.INSTALLED.name,
                apkPath = apkPath,
                installDate = System.currentTimeMillis()
            ) ?: return Result.failure(Exception("Extension not found: $pkgName"))
            
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
        val hash = ext.signatureHash
            ?: error("Extension $pkgName has no signature hash — cannot trust")
        extensionLoader.trustExtension(hash)
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
            val installedPkgNames = installed.map { it.pkgName }.toSet()
            
            // Filter out already installed extensions, mark as AVAILABLE
            val availableExtensions = remoteExtensions
                .filter { it.pkgName !in installedPkgNames }
                .map { 
                    mapper.toEntity(it.copy(status = InstallStatus.AVAILABLE))
                }
            
            // Clear old available extensions and insert new ones
            localDataSource.insertExtensions(availableExtensions)
            
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
