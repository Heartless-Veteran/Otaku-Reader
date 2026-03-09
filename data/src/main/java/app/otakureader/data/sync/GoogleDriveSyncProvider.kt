package app.otakureader.data.sync

import app.otakureader.domain.model.SyncSnapshot
import app.otakureader.domain.sync.SyncProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive implementation of SyncProvider using the REST API.
 *
 * This is a prototype implementation demonstrating how to integrate with
 * Google Drive for cloud sync. It uses:
 * - OAuth 2.0 for authentication
 * - Google Drive REST API v3
 * - JSON serialization for snapshots
 * - Single file storage in app data folder
 *
 * Production implementation should add:
 * - Token refresh handling
 * - Retry logic with exponential backoff
 * - Proper error handling and user feedback
 * - Version history/conflict detection
 * - Compression for large snapshots
 */
@Singleton
class GoogleDriveSyncProvider @Inject constructor(
    private val httpClient: OkHttpClient,
    private val googleDriveAuth: GoogleDriveAuthenticator
) : SyncProvider {

    override val id: String = PROVIDER_ID
    override val name: String = "Google Drive"

    override val isAuthenticated: Boolean
        get() = googleDriveAuth.isAuthenticated()

    private val json = Json {
        prettyPrint = false // Minimize size for network transfer
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun authenticate(): Result<Unit> {
        return try {
            googleDriveAuth.authenticate()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        googleDriveAuth.clearCredentials()
    }

    override suspend fun uploadSnapshot(snapshot: SyncSnapshot): Result<Unit> {
        return try {
            if (!isAuthenticated) {
                return Result.failure(IllegalStateException("Not authenticated"))
            }

            val token = googleDriveAuth.getAccessToken()
                ?: return Result.failure(IllegalStateException("No access token"))

            // Serialize snapshot to JSON
            val jsonContent = json.encodeToString(snapshot)
            val fileId = getOrCreateSnapshotFileId(token)

            if (fileId != null) {
                // Update existing file
                updateFile(token, fileId, jsonContent)
            } else {
                // Create new file
                createFile(token, jsonContent)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadSnapshot(): Result<SyncSnapshot?> {
        return try {
            if (!isAuthenticated) {
                return Result.failure(IllegalStateException("Not authenticated"))
            }

            val token = googleDriveAuth.getAccessToken()
                ?: return Result.failure(IllegalStateException("No access token"))

            val fileId = getOrCreateSnapshotFileId(token)
                ?: return Result.success(null) // No snapshot exists yet

            val content = downloadFile(token, fileId)
            val snapshot = json.decodeFromString<SyncSnapshot>(content)

            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastSnapshotTime(): Long? {
        return try {
            if (!isAuthenticated) return null

            val token = googleDriveAuth.getAccessToken() ?: return null
            val fileId = getOrCreateSnapshotFileId(token) ?: return null

            // Get file metadata to check modification time
            val request = Request.Builder()
                .url("$DRIVE_API_BASE/files/$fileId?fields=modifiedTime")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val body = response.body?.string() ?: return null
                // Parse ISO 8601 timestamp from response
                // For prototype, return null - production would parse this properly
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteAllData(): Result<Unit> {
        return try {
            if (!isAuthenticated) {
                return Result.failure(IllegalStateException("Not authenticated"))
            }

            val token = googleDriveAuth.getAccessToken()
                ?: return Result.failure(IllegalStateException("No access token"))

            val fileId = getOrCreateSnapshotFileId(token)
            if (fileId != null) {
                deleteFile(token, fileId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableSpace(): Long? {
        // Google Drive API can query storage quota
        // For prototype, return null (assume unlimited/sufficient)
        return null
    }

    // -------------------------------------------------------------------------
    // Private helper methods for Google Drive REST API
    // -------------------------------------------------------------------------

    /**
     * Find the sync snapshot file in app data folder, or return null if it doesn't exist.
     * File is stored in application data folder (hidden from user).
     */
    private fun getOrCreateSnapshotFileId(token: String): String? {
        val request = Request.Builder()
            .url("$DRIVE_API_BASE/files?q=name='$SNAPSHOT_FILENAME'+and+trashed=false&spaces=appDataFolder")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            // Parse JSON response to extract file ID
            // For prototype: simplified parsing (production should use proper JSON parsing)
            val idMatch = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)
            return idMatch?.groupValues?.get(1)
        }
    }

    /**
     * Create a new file in app data folder with the snapshot content.
     */
    private fun createFile(token: String, content: String) {
        val metadata = """
            {
                "name": "$SNAPSHOT_FILENAME",
                "parents": ["appDataFolder"],
                "mimeType": "$JSON_MIME_TYPE"
            }
        """.trimIndent()

        // Multipart upload for file with metadata
        val boundary = "otaku_reader_boundary"
        val multipartBody = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: $JSON_MIME_TYPE\r\n\r\n")
            append(content)
            append("\r\n--$boundary--")
        }

        val requestBody = multipartBody.toRequestBody(
            "multipart/related; boundary=$boundary".toMediaType()
        )

        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_BASE/files?uploadType=multipart")
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to create file: ${response.code}")
            }
        }
    }

    /**
     * Update existing file content.
     */
    private fun updateFile(token: String, fileId: String, content: String) {
        val requestBody = content.toRequestBody(JSON_MIME_TYPE.toMediaType())

        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_BASE/files/$fileId?uploadType=media")
            .header("Authorization", "Bearer $token")
            .patch(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to update file: ${response.code}")
            }
        }
    }

    /**
     * Download file content.
     */
    private fun downloadFile(token: String, fileId: String): String {
        val request = Request.Builder()
            .url("$DRIVE_API_BASE/files/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download file: ${response.code}")
            }
            return response.body?.string()
                ?: throw Exception("Empty response body")
        }
    }

    /**
     * Delete file from Drive.
     */
    private fun deleteFile(token: String, fileId: String) {
        val request = Request.Builder()
            .url("$DRIVE_API_BASE/files/$fileId")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to delete file: ${response.code}")
            }
        }
    }

    companion object {
        const val PROVIDER_ID = "google_drive"
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
        private const val SNAPSHOT_FILENAME = "otaku_reader_sync.json"
        private const val JSON_MIME_TYPE = "application/json"
    }
}
