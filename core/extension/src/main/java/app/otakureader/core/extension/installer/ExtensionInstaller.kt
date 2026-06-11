package app.otakureader.core.extension.installer

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import app.otakureader.core.extension.data.remote.ExtensionRemoteDataSource
import app.otakureader.core.extension.domain.model.Extension
import app.otakureader.core.extension.domain.model.InstallStatus
import app.otakureader.core.extension.domain.repository.ExtensionRepository
import app.otakureader.core.extension.loader.ExtensionLoadResult
import app.otakureader.core.extension.loader.ExtensionLoader
import app.otakureader.core.extension.receiver.ExtensionInstallReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Installation state for tracking progress.
 */
sealed class InstallationState {
    data object Idle : InstallationState()
    data class Downloading(val progress: Int) : InstallationState()
    data object Verifying : InstallationState()
    data object Installing : InstallationState()
    data class Success(val extension: Extension) : InstallationState()
    data class Error(val message: String, val throwable: Throwable? = null) : InstallationState()
}

/**
 * Handles APK installation, update, and removal for extensions.
 */
class ExtensionInstaller(
    private val context: Context,
    private val repository: ExtensionRepository,
    private val loader: ExtensionLoader,
    private val remoteDataSource: ExtensionRemoteDataSource
) {
    
    companion object {
        private const val EXTENSIONS_DIR = "exts"
        private const val DOWNLOADS_DIR = "extension_downloads"
    }
    
    private val _installationState = MutableStateFlow<InstallationState>(InstallationState.Idle)
    val installationState: Flow<InstallationState> = _installationState.asStateFlow()
    
    private val extensionsDir: File by lazy {
        File(context.filesDir, EXTENSIONS_DIR).apply { mkdirs() }
    }
    
    private val downloadsDir: File by lazy {
        File(context.filesDir, DOWNLOADS_DIR).apply { mkdirs() }
    }

    /**
     * Download and install an extension from its APK URL.
     *
     * **Security contract**: Only HTTPS URLs are accepted. The caller is responsible for
     * displaying a user-facing confirmation dialog before invoking this method when the
     * extension originates from an untrusted or user-supplied URL (i.e.
     * [extension.signatureHash] is null). This method enforces HTTPS at the transport
     * layer but cannot verify the trustworthiness of the remote host.
     *
     * @param extension The extension to install (must have apkUrl)
     * @return Result containing the installed Extension
     */
    suspend fun downloadAndInstall(extension: Extension): Result<Extension> = withContext(Dispatchers.IO) {
        try {
            val apkUrl = extension.apkUrl
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Extension has no APK URL")
                )

            // C-3: Reject non-HTTPS URLs to prevent man-in-the-middle attacks.
            if (!apkUrl.startsWith("https://")) {
                return@withContext Result.failure(
                    SecurityException(
                        "Extension APK URL must use HTTPS. Insecure URL rejected: $apkUrl"
                    )
                )
            }

            _installationState.value = InstallationState.Downloading(0)

            // Generate a unique filename for the download
            val downloadFile = File(downloadsDir, "${UUID.randomUUID()}.apk")

            // Download the APK
            val downloadResult = remoteDataSource.downloadApk(apkUrl, downloadFile)
            if (downloadResult.isFailure) {
                _installationState.value = InstallationState.Error(
                    "Download failed: ${downloadResult.exceptionOrNull()?.message}",
                    downloadResult.exceptionOrNull()
                )
                return@withContext Result.failure(
                    downloadResult.exceptionOrNull() ?: Exception("Download failed")
                )
            }

            // Verify signature if available
            if (extension.signatureHash != null) {
                val isValid = verifySignature(downloadFile, extension.signatureHash)
                if (!isValid) {
                    downloadFile.delete()
                    _installationState.value = InstallationState.Error(
                        "Signature verification failed"
                    )
                    return@withContext Result.failure(
                        SecurityException("APK signature does not match expected hash")
                    )
                }
            }

            // Register the extension inside the app, passing the repo-verified hash so the
            // universal trust gate inside ExtensionLoader can be satisfied without a separate
            // user confirmation step. The repo URL rides along so a fallback-created DB row
            // still records provenance (#1019).
            val result = install(
                downloadFile,
                trustedHash = extension.signatureHash,
                repoUrl = extension.repoUrl,
            )

            result.onSuccess { installed ->
                installed.apkPath?.let { path ->
                    maybeLaunchSystemInstaller(File(path))
                }
            }

            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _installationState.value = InstallationState.Error(
                "Installation failed: ${e.message}",
                e
            )
            Result.failure(e)
        }
    }

    /**
     * Atomically replace [targetFile] with [tempFile].
     *
     * Tries API 26+ [java.nio.file.Files.move] with [ATOMIC_MOVE] + [REPLACE_EXISTING]
     * first. On older Android or when the atomic move fails, falls back to a
     * backup-then-rename strategy that preserves the original file if anything
     * goes wrong. Last resort is copy-overwrite.
     *
     * @return true on success. On failure [targetFile] is left untouched if possible.
     */
    private fun replaceAtomically(tempFile: File, targetFile: File): Boolean {
        if (!tempFile.exists()) return false

        // API 26+: true atomic move with replace
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
                return true
            } catch (_: Exception) {
                // Fall through to fallback
            }
        }

        // Pre-API 26: try renameTo first (fast, atomic on same filesystem).
        // renameTo does not overwrite an existing file on Android.
        if (!targetFile.exists()) {
            if (tempFile.renameTo(targetFile)) {
                return true
            }
        } else {
            // Backup the existing file, then rename temp into place.
            val backupFile = File(targetFile.parentFile, "${targetFile.name}.bak")
            if (targetFile.renameTo(backupFile)) {
                if (tempFile.renameTo(targetFile)) {
                    backupFile.delete() // Commit: discard backup
                    return true
                }
                // Rollback: restore original file
                backupFile.renameTo(targetFile)
                return false
            }
        }

        // Last resort: copy temp over target, then delete temp.
        // Not atomic, but keeps target valid if the copy throws.
        return try {
            tempFile.copyTo(targetFile, overwrite = true)
            targetFile.setReadOnly()
            tempFile.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Install an extension from a downloaded APK file.
     *
     * **Transactional contract**: writes to a temp file (`$pkgName.ext.tmp`), loads and
     * validates the extension from that temp file, and only after every check passes
     * atomically renames the temp file to the permanent location (`$pkgName.ext`).
     * If any step fails the original extension (if any) is left untouched and the temp
     * file is deleted.
     *
     * @param apkFile The downloaded APK file.
     * @param trustedHash When non-null this hash was already verified by the caller against
     *   a repository-sourced expected hash. The extension's actual loaded hash is compared
     *   against this value; the trust store is only updated **after** the extension has been
     *   successfully loaded and moved to its permanent location.
     * @param repoUrl The repository this APK was downloaded from, when known. Recorded as
     *   install provenance (#1019) — without it, a fallback-created DB row would have null
     *   sourceRepoUrl and the cross-repo replacement guard would be inactive until the next
     *   update check backfilled it.
     */
    suspend fun install(
        apkFile: File,
        trustedHash: String? = null,
        repoUrl: String? = null,
    ): Result<Extension> =
        withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                _installationState.value = InstallationState.Verifying

                val packageInfo = parseApkInfo(apkFile)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Failed to parse APK: ${apkFile.absolutePath}")
                    )

                val pkgName = packageInfo.packageName
                val destFile = File(extensionsDir, "$pkgName.ext")
                tempFile = File(extensionsDir, "$pkgName.ext.tmp")

                apkFile.copyTo(tempFile, overwrite = true)
                tempFile.setReadOnly()

                val loadResult = loader.loadExtension(tempFile.absolutePath)
                val extension = resolveLoadResult(loadResult, trustedHash)
                    .getOrElse { return@withContext Result.failure(it) }

                if (!replaceAtomically(tempFile, destFile)) {
                    return@withContext Result.failure(
                        IllegalStateException("Failed to move extension to permanent location: ${destFile.absolutePath}")
                    )
                }
                tempFile = null

                extension.signatureHash?.let { loader.trustExtension(it) }

                _installationState.value = InstallationState.Installing
                val finalExtension = extension.copy(
                    apkPath = destFile.absolutePath,
                    repoUrl = extension.repoUrl ?: repoUrl,
                )
                // Pass the fully-loaded extension so the install succeeds even when the
                // database has no row yet (e.g. repo was just added and the available-list
                // refresh hasn't synced).
                val result = repository.installExtension(finalExtension, destFile.absolutePath)
                result.onSuccess { ext ->
                    _installationState.value = InstallationState.Success(ext)
                    ExtensionInstallReceiver.notifyAdded(context, finalExtension.pkgName)
                }.onFailure { error ->
                    _installationState.value = InstallationState.Error("Failed to save extension: ${error.message}", error)
                }
                result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _installationState.value = InstallationState.Error("Installation failed", e)
                Result.failure(e)
            } finally {
                tempFile?.delete()
                if (apkFile.exists() && apkFile.parentFile == downloadsDir) apkFile.delete()
            }
        }

    private fun resolveLoadResult(
        loadResult: ExtensionLoadResult,
        trustedHash: String?
    ): Result<Extension> = when (loadResult) {
        is ExtensionLoadResult.Success -> {
            val ext = loadResult.extension
            if (trustedHash != null && ext.signatureHash != trustedHash) {
                _installationState.value = InstallationState.Error("Extension trust hash mismatch", null)
                Result.failure(SecurityException("Trust hash mismatch for ${ext.pkgName}"))
            } else {
                Result.success(ext)
            }
        }
        is ExtensionLoadResult.Untrusted -> {
            val ext = loadResult.extension
            if (trustedHash != null && ext.signatureHash == trustedHash) {
                Result.success(ext)
            } else {
                _installationState.value = InstallationState.Error(
                    "Extension is not trusted. Please verify its signature before installing.", null
                )
                Result.failure(IllegalStateException("Untrusted extension: ${ext.pkgName}"))
            }
        }
        is ExtensionLoadResult.Error -> {
            _installationState.value = InstallationState.Error(loadResult.message, loadResult.throwable)
            Result.failure(loadResult.throwable ?: IllegalStateException(loadResult.message))
        }
    }
    
    /**
     * Update an existing extension.
     *
     * **Transactional contract**: writes the new APK to a temp file (`$pkgName.ext.tmp`),
     * parses it, loads the extension from the temp file, and verifies signer continuity
     * against the currently installed version. Only after every validation step passes
     * is the temp file atomically promoted to the permanent location (`$pkgName.ext`).
     * If any step fails the original extension remains untouched and the temp file is
     * cleaned up.
     *
     * @param pkgName Package name of the extension to update
     * @param newApkFile The new APK file
     * @return Result containing the updated Extension
     */
    @Suppress("LongMethod")
    suspend fun update(pkgName: String, newApkFile: File): Result<Extension> =
        withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                _installationState.value = InstallationState.Verifying

                val newPackageInfo = parseApkInfo(newApkFile)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Failed to parse update APK: ${newApkFile.absolutePath}")
                    )

                if (newPackageInfo.packageName != pkgName) {
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            "Package name mismatch: expected $pkgName, got ${newPackageInfo.packageName}"
                        )
                    )
                }

                val destFile = File(extensionsDir, "$pkgName.ext")
                tempFile = File(extensionsDir, "$pkgName.ext.tmp")

                // 1. Write to temp file — preserve the working artifact until validation passes.
                newApkFile.copyTo(tempFile, overwrite = true)
                tempFile.setReadOnly()

                // 2. Load and validate from the temp file.
                val loadResult = loader.loadExtension(tempFile.absolutePath)

                val extension = when (loadResult) {
                    is ExtensionLoadResult.Success -> loadResult.extension
                    is ExtensionLoadResult.Untrusted -> {
                        _installationState.value = InstallationState.Error(
                            "Extension is not trusted. Please verify its signature before updating.",
                            null
                        )
                        return@withContext Result.failure(
                            IllegalStateException("Untrusted extension: ${loadResult.extension.pkgName}")
                        )
                    }
                    is ExtensionLoadResult.Error -> {
                        _installationState.value = InstallationState.Error(
                            loadResult.message, loadResult.throwable
                        )
                        return@withContext Result.failure(
                            loadResult.throwable ?: IllegalStateException(loadResult.message)
                        )
                    }
                }
                // 3. Signer continuity: reject if signing certificate changed — a compromised
                // repository could swap a trusted extension for a differently-signed one.
                val oldExtension = repository.getExtension(pkgName)
                val newHash = extension.signatureHash
                if (oldExtension?.signatureHash != null && newHash != oldExtension.signatureHash) {
                    return@withContext Result.failure(
                        SecurityException(
                            "Extension update rejected: signing certificate changed for $pkgName"
                        )
                    )
                }

                // 4. Atomic rename temp → permanent.
                if (!replaceAtomically(tempFile, destFile)) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "Failed to move extension to permanent location: ${destFile.absolutePath}"
                        )
                    )
                }
                tempFile = null // Ownership transferred to destFile

                // 5. Update repository state. The old file has already been replaced atomically.
                _installationState.value = InstallationState.Installing
                val result = repository.updateExtension(pkgName, destFile.absolutePath)
                result.onSuccess { ext ->
                    _installationState.value = InstallationState.Success(ext)
                    ExtensionInstallReceiver.notifyReplaced(context, pkgName)
                }.onFailure { error ->
                    _installationState.value = InstallationState.Error(
                        "Failed to update extension: ${error.message}", error
                    )
                }
                result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _installationState.value = InstallationState.Error("Update failed", e)
                Result.failure(e)
            } finally {
                // Clean up temp file if the atomic rename never happened.
                tempFile?.delete()
                // Clean up the download artifact.
                if (newApkFile.exists() && newApkFile.parentFile == downloadsDir) newApkFile.delete()
            }
        }
    
    /**
     * Uninstall an extension.
     *
     * Distinguishes two cases:
     * - **System-installed (shared) extensions**: the package is registered with the
     *   Android PackageManager. Launching [Intent.ACTION_DELETE] triggers the system
     *   uninstaller dialog (requires [android.permission.REQUEST_DELETE_PACKAGES]).
     *   On user confirmation the system broadcasts [Intent.ACTION_PACKAGE_REMOVED],
     *   which [ExtensionInstallReceiver] receives to clean up the database entry.
     *   Any locally cached private APK copy is also removed immediately.
     * - **Private/sideloaded extensions**: stored only in the app's internal files dir
     *   and not registered with PackageManager. The local APK and database entry are
     *   deleted directly and a local removal broadcast is sent.
     *
     * @param pkgName Package name to uninstall
     * @return Result indicating success or failure
     */
    suspend fun uninstall(pkgName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            repository.setExtensionStatus(pkgName, InstallStatus.UNINSTALLING)

            if (isSystemInstalled(pkgName)) {
                // Trigger the system uninstaller dialog for shared/installed extensions.
                // The system will broadcast ACTION_PACKAGE_REMOVED on confirmation,
                // which ExtensionInstallReceiver handles to remove the DB entry.
                val deleteIntent = Intent(
                    Intent.ACTION_DELETE,
                    "package:$pkgName".toUri()
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(deleteIntent)

                // Remove any locally cached private APK copy for this package.
                File(extensionsDir, "$pkgName.ext").takeIf { it.exists() }?.delete()

                Result.success(Unit)
            } else {
                // Private/sideloaded extension: delete local APK and remove from DB.
                File(extensionsDir, "$pkgName.ext").takeIf { it.exists() }?.delete()

                // Remove from repository and notify the receiver.
                repository.uninstallExtension(pkgName).also {
                    ExtensionInstallReceiver.notifyRemoved(context, pkgName)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns true when [pkgName] is currently installed as a shared system package
     * discoverable via PackageManager. Private/sideloaded extensions stored only in
     * the app's internal files dir will return false.
     */
    private fun isSystemInstalled(pkgName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    pkgName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(pkgName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun parseApkInfo(apkFile: File): PackageInfo? {
        return context.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_META_DATA or PackageManager.GET_CONFIGURATIONS
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES or PackageManager.GET_META_DATA or PackageManager.GET_CONFIGURATIONS
            }
        )
    }

    /**
     * Verify APK signature against expected hash.
     * @param apkFile The APK file to verify
     * @param expectedHash Expected signature hash (optional, for trusted repos)
     * @return true if signature is valid or no hash provided
     */
    suspend fun verifySignature(apkFile: File, expectedHash: String?): Boolean = 
        withContext(Dispatchers.IO) {
            if (expectedHash == null) return@withContext true
            
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.packageManager.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        PackageManager.GET_SIGNATURES
                    )
                }
                
                val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.signatures
                }
                
                val actualHash = signatures?.firstOrNull()?.toByteArray()?.let {
                    computeHash(it)
                }
                
                actualHash == expectedHash
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        }
    
    /**
     * Get download directory for APKs.
     */
    fun getDownloadsDirectory(): File = downloadsDir
    
    /**
     * Get installation directory for extensions.
     */
    fun getExtensionsDirectory(): File = extensionsDir
    
    /**
     * Clear installation state.
     */
    fun resetState() {
        _installationState.value = InstallationState.Idle
    }

    /**
     * Optionally trigger the system package installer so extensions are registered
     * as shared packages. Falls back to internal loading if the permission is not
     * granted; opens the settings screen to request it.
     */
    private fun maybeLaunchSystemInstaller(apkFile: File) {
        // Make the file read-only so the system installer accepts it.
        apkFile.setReadOnly()

        // C-4: Request unknown-sources permission if not already granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            // Note: The user must manually return and retry the install after granting.
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }
    
    private fun computeHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
