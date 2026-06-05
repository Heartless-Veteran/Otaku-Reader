package app.otakureader.core.preferences

import java.io.File

/**
 * Abstraction for uploading a backup file to a remote cloud destination.
 *
 * Implementations live in the data layer (e.g. [WebDavUploader]) and are bound
 * via Hilt so that the feature layer can depend only on this interface without
 * requiring a direct dependency on the data module.
 *
 * [configure] must be called before [upload] or [testConnection] to set the
 * destination URL and credentials. It is intentionally part of the interface so
 * that the feature layer can reconfigure the uploader when the user changes their
 * WebDAV settings, without needing to reference concrete implementation classes.
 */
interface CloudBackupUploader {
    fun configure(url: String, username: String, password: String)
    suspend fun upload(file: File): Result<Unit>
    suspend fun testConnection(): Result<Unit>
}
