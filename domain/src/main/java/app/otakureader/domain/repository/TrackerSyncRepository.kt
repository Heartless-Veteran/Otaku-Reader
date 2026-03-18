package app.otakureader.domain.repository

import app.otakureader.domain.model.SyncConfiguration
import app.otakureader.domain.model.TrackerSyncState
import kotlinx.coroutines.flow.Flow

/**
 * Repository for tracker 2-way synchronization.
 * Manages bidirectional sync between local library and external trackers.
 */
interface TrackerSyncRepository {
    // Sync Configuration
    fun getSyncConfigurations(): Flow<List<SyncConfiguration>>
    suspend fun updateSyncConfiguration(config: SyncConfiguration)
    suspend fun enableTrackerSync(trackerId: Int, enabled: Boolean)

    // Sync State
    fun getSyncStateForManga(mangaId: Long): Flow<List<TrackerSyncState>>
    fun getPendingSyncs(): Flow<List<TrackerSyncState>>
    suspend fun recordLocalChange(
        mangaId: Long,
        trackerId: Int,
        chapterRead: Float,
        status: app.otakureader.domain.model.MangaStatus
    )

    // Sync Operations
    suspend fun syncManga(mangaId: Long, trackerId: Int): SyncResult
    suspend fun syncAllPending(): SyncSummary
    suspend fun resolveConflict(
        mangaId: Long,
        trackerId: Int,
        useLocal: Boolean
    )

    // Manual operations
    suspend fun pushToTracker(mangaId: Long, trackerId: Int): SyncResult
    suspend fun pullFromTracker(mangaId: Long, trackerId: Int): SyncResult

    data class SyncResult(
        val success: Boolean,
        val message: String,
        val hasConflict: Boolean = false
    )

    data class SyncSummary(
        val attempted: Int,
        val successful: Int,
        val failed: Int,
        val conflicts: Int
    )
}
