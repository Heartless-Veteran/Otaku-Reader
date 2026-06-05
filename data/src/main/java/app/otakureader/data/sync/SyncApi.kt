package app.otakureader.data.sync

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for the self-hosted Otaku Reader sync server.
 *
 * The base URL is determined at runtime from [SyncSettingsStore.serverUrl] so
 * a fresh Retrofit instance is built per-call inside [SyncRepositoryImpl].
 */
interface SyncApi {

    /** Push one chapter-read progress event to the server. */
    @POST("progress/push")
    suspend fun pushProgress(@Body body: PushProgressRequest): PushProgressResponse

    /**
     * Fetch progress events that happened on other devices after [since].
     *
     * @param deviceId  The calling device's stable identifier.  The server uses
     *                  this to exclude events that originated here.
     * @param since     Epoch-millis lower bound (exclusive).
     */
    @GET("progress/pull")
    suspend fun pullProgress(
        @Query("deviceId") deviceId: String,
        @Query("since") since: Long,
    ): List<RemoteProgressEntry>
}

// ─── Request / Response models ────────────────────────────────────────────────

@Serializable
data class PushProgressRequest(
    val chapterId: Long,
    val mangaId: Long,
    val chapterNumber: Float,
    val deviceId: String,
    val readAt: Long,
)

@Serializable
data class PushProgressResponse(val accepted: Boolean)

@Serializable
data class RemoteProgressEntry(
    val chapterId: Long,
    val mangaId: Long,
    val chapterNumber: Float,
    val readAt: Long,
)
