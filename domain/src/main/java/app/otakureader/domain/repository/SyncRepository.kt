package app.otakureader.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for reader-progress sync with a self-hosted sync server.
 *
 * Implementations must be safe to call from any coroutine context and should
 * never throw for network errors — failed items remain in the queue for the
 * next drain cycle.
 */
interface SyncRepository {

    /**
     * Serialise a chapter-read event and add it to the local sync queue.
     *
     * This is a local-only operation and always succeeds; actual network
     * transmission happens in [drainQueue].
     */
    suspend fun enqueueChapterRead(chapterId: Long, mangaId: Long, chapterNumber: Float)

    /**
     * Attempt to push all queued items to the server, removing each item on
     * success and incrementing its attempt counter on failure.
     */
    suspend fun drainQueue()

    /**
     * Pull progress events from the server that occurred after [since] (epoch
     * millis) and apply them to the local database.
     *
     * @param deviceId  Stable identifier for this installation (used as the
     *                  "source" filter so the server can exclude events that
     *                  originated on this device).
     * @param since     Only fetch events newer than this epoch-millis timestamp.
     */
    suspend fun pullAndApply(deviceId: String, since: Long)

    /** Observe the number of items currently waiting in the local sync queue. */
    fun observeQueueSize(): Flow<Int>
}
