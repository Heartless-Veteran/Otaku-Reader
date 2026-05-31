package app.otakureader.data.sync

import android.util.Log
import app.otakureader.core.database.dao.ChapterDao
import app.otakureader.core.database.dao.SyncQueueDao
import app.otakureader.core.database.entity.SyncQueueEntity
import app.otakureader.core.preferences.SyncSettingsStore
import app.otakureader.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data-layer implementation of [SyncRepository].
 *
 * **Why we inject [ChapterDao] instead of [ChapterRepository]:**
 * [ChapterRepositoryImpl] depends on [SyncRepository] (to enqueue read events),
 * so injecting the full [ChapterRepository] interface here would create a
 * circular Hilt dependency.  Accessing the DAO directly is safe because
 * [SyncRepositoryImpl] only needs a simple `UPDATE chapters SET read = 1`
 * operation for pulled progress — no domain mapping is required.
 *
 * **Why we build Retrofit lazily per-call:** the sync server's base URL comes
 * from DataStore and can change at any time via Settings.  Injecting the
 * shared [Retrofit] singleton (which has a fixed `https://api.otakureader.app/`
 * base URL) would silently send progress events to the wrong host.  Instead,
 * we inject [OkHttpClient] (the shared, configured client) and [Json] (the
 * shared serialisation config) and construct a lightweight `Retrofit` instance
 * on every drain/pull call using the current URL from DataStore.
 *
 * This is intentional — Retrofit instances are cheap to create and the sync
 * operations already have network I/O overhead that dwarfs object allocation.
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val syncSettingsStore: SyncSettingsStore,
    private val chapterDao: ChapterDao,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) : SyncRepository {

    // ─── Public API ───────────────────────────────────────────────────────────

    override suspend fun enqueueChapterRead(chapterId: Long, mangaId: Long, chapterNumber: Float) {
        val deviceId = syncSettingsStore.ensureDeviceId()
        val payload = json.encodeToString(
            PushProgressRequest(
                chapterId = chapterId,
                mangaId = mangaId,
                chapterNumber = chapterNumber,
                deviceId = deviceId,
                readAt = System.currentTimeMillis(),
            )
        )
        syncQueueDao.insert(
            SyncQueueEntity(
                chapterId = chapterId,
                mangaId = mangaId,
                payload = payload,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun drainQueue() {
        val serverUrl = syncSettingsStore.serverUrl.first()
        if (serverUrl.isBlank()) return
        val api = buildApi(serverUrl)
        val pending = syncQueueDao.getPending()
        for (item in pending) {
            try {
                val request = json.decodeFromString<PushProgressRequest>(item.payload)
                api.pushProgress(request)
                syncQueueDao.deleteById(item.id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push sync item ${item.id}", e)
                syncQueueDao.recordAttemptFailure(item.id, e.message)
            }
        }
    }

    override suspend fun pullAndApply(deviceId: String, since: Long) {
        val serverUrl = syncSettingsStore.serverUrl.first()
        if (serverUrl.isBlank()) return
        try {
            val api = buildApi(serverUrl)
            val entries = api.pullProgress(deviceId, since)
            for (entry in entries) {
                // Mark the chapter as read via the DAO directly to avoid a circular
                // dependency with ChapterRepositoryImpl (which depends on SyncRepository).
                // lastPageRead = 0: we sync "read/not-read" state, not exact page position.
                chapterDao.updateChapterProgress(
                    chapterId = entry.chapterId,
                    read = true,
                    lastPageRead = 0,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pull failed", e)
        }
    }

    override fun observeQueueSize(): Flow<Int> = syncQueueDao.observeQueueSize()

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Builds a minimal Retrofit instance pointed at [serverUrl].
     *
     * We reuse the shared [OkHttpClient] (timeouts, logging, Brotli) and [Json]
     * (lenient, ignores unknown keys) from the network module.
     */
    private fun buildApi(serverUrl: String): SyncApi {
        // Ensure the URL ends with "/" so that Retrofit resolves relative paths correctly.
        val normalised = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return Retrofit.Builder()
            .baseUrl(normalised)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SyncApi::class.java)
    }

    companion object {
        private const val TAG = "SyncRepository"
    }
}
