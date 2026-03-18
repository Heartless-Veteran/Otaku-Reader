package app.otakureader.data.sync

import android.util.Base64
import app.otakureader.core.preferences.SyncPreferences
import app.otakureader.data.sync.remote.SelfHostedSyncApi
import app.otakureader.data.sync.remote.SelfHostedSyncApiFactory
import app.otakureader.domain.model.SyncSnapshot
import app.otakureader.domain.sync.SyncProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync provider for self-hosted Otaku Reader sync server.
 *
 * Stores/retrieves sync snapshots via HTTP API with Bearer token authentication.
 */
@Singleton
class SelfHostedSyncProvider @Inject constructor(
    private val apiFactory: SelfHostedSyncApiFactory,
    private val syncPreferences: SyncPreferences
) : SyncProvider {

    override val id: String = "self_hosted"
    override val name: String = "Self-Hosted Server"

    override val isAuthenticated: Boolean
        get() = syncPreferences.selfHostedServerUrl.isNotBlank() &&
                syncPreferences.selfHostedAuthToken.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true }

    private fun getApi(): SelfHostedSyncApi? {
        return apiFactory.createOrNull()
    }

    private fun getAuthHeader(): String {
        return "Bearer ${syncPreferences.selfHostedAuthToken}"
    }

    override suspend fun authenticate(): Result<Unit> {
        val url = syncPreferences.selfHostedServerUrl
        val token = syncPreferences.selfHostedAuthToken

        if (url.isBlank() || token.isBlank()) {
            return Result.failure(IllegalStateException("Server URL and auth token must be configured"))
        }

        val api = getApi() 
            ?: return Result.failure(IllegalStateException("Failed to create API client"))

        return try {
            val response = api.healthCheck()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Server health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        syncPreferences.selfHostedServerUrl = ""
        syncPreferences.selfHostedAuthToken = ""
    }

    override suspend fun uploadSnapshot(snapshot: SyncSnapshot): Result<Unit> {
        if (!isAuthenticated) {
            return Result.failure(IllegalStateException("Not authenticated"))
        }

        val api = getApi() 
            ?: return Result.failure(IllegalStateException("Server URL not configured"))

        return try {
            val snapshotJson = json.encodeToString(snapshot)
            val base64Data = Base64.encodeToString(snapshotJson.toByteArray(), Base64.NO_WRAP)
            val timestamp = System.currentTimeMillis()

            val response = api.uploadSnapshot(
                authHeader = getAuthHeader(),
                request = app.otakureader.data.sync.remote.UploadRequest(
                    data = base64Data,
                    timestamp = timestamp
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                syncPreferences.lastSyncTimestamp = timestamp
                Result.success(Unit)
            } else {
                Result.failure(
                    IllegalStateException(
                        "Upload failed: ${response.code()} - ${response.errorBody()?.string()}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadSnapshot(): Result<SyncSnapshot?> {
        if (!isAuthenticated) {
            return Result.failure(IllegalStateException("Not authenticated"))
        }

        val api = getApi() 
            ?: return Result.failure(IllegalStateException("Server URL not configured"))

        return try {
            val response = api.downloadSnapshot(getAuthHeader())

            if (!response.isSuccessful) {
                return Result.failure(
                    IllegalStateException(
                        "Download failed: ${response.code()} - ${response.errorBody()?.string()}"
                    )
                )
            }

            val body = response.body()
            if (body?.exists != true || body.data == null) {
                return Result.success(null)
            }

            val snapshotJson = String(Base64.decode(body.data, Base64.NO_WRAP))
            val snapshot = json.decodeFromString<SyncSnapshot>(snapshotJson)
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastSnapshotTime(): Long? {
        if (!isAuthenticated) return null

        val api = getApi() ?: return null

        return try {
            val response = api.getTimestamp(getAuthHeader())
            if (response.isSuccessful) {
                response.body()?.timestamp
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteAllData(): Result<Unit> {
        if (!isAuthenticated) {
            return Result.failure(IllegalStateException("Not authenticated"))
        }

        val api = getApi() 
            ?: return Result.failure(IllegalStateException("Server URL not configured"))

        return try {
            val response = api.deleteSnapshot(getAuthHeader())
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(
                    IllegalStateException(
                        "Delete failed: ${response.code()} - ${response.errorBody()?.string()}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableSpace(): Long? {
        // Not implemented for self-hosted (server-side storage)
        return null
    }
}
