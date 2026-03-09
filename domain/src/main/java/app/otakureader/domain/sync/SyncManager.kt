package app.otakureader.domain.sync

import app.otakureader.domain.model.SyncResult
import app.otakureader.domain.model.SyncSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Manages cross-device synchronization of library data.
 *
 * The SyncManager coordinates pushing local changes to cloud storage and pulling
 * remote changes to merge with local state. It handles conflict resolution,
 * incremental sync, and sync state tracking.
 */
interface SyncManager {

    /**
     * Observable flow of the current sync status.
     */
    val syncStatus: Flow<SyncStatus>

    /**
     * Observable flow indicating whether sync is enabled.
     */
    val isSyncEnabled: Flow<Boolean>

    /**
     * Enable sync with the specified provider.
     *
     * @param providerId Unique identifier for the sync provider (e.g., "google_drive")
     * @return Result indicating success or failure with error details
     */
    suspend fun enableSync(providerId: String): Result<Unit>

    /**
     * Disable sync and optionally clear local sync metadata.
     *
     * @param clearMetadata If true, clears local tracking of last sync timestamps
     */
    suspend fun disableSync(clearMetadata: Boolean = false)

    /**
     * Trigger a manual sync operation.
     *
     * This will:
     * 1. Create a snapshot of local data
     * 2. Upload to the cloud provider
     * 3. Download the latest remote snapshot
     * 4. Merge changes with conflict resolution
     * 5. Apply merged changes to local database
     *
     * @return Result containing sync statistics or error details
     */
    suspend fun sync(): Result<SyncResult>

    /**
     * Upload current local state to cloud storage without downloading/merging.
     *
     * @return Result indicating success or failure
     */
    suspend fun pushToCloud(): Result<Unit>

    /**
     * Download latest remote state and merge with local data.
     *
     * @return Result containing sync statistics or error details
     */
    suspend fun pullFromCloud(): Result<SyncResult>

    /**
     * Get the last sync timestamp.
     *
     * @return Timestamp in milliseconds since epoch, or null if never synced
     */
    suspend fun getLastSyncTime(): Long?

    /**
     * Create a snapshot of the current local state.
     *
     * This is a lightweight version of a full backup, containing only data
     * necessary for cross-device sync (no preferences, focused on library state).
     *
     * @return SyncSnapshot containing current library state
     */
    suspend fun createSnapshot(): SyncSnapshot

    /**
     * Apply a sync snapshot to the local database.
     *
     * @param snapshot The snapshot to apply
     * @param strategy Conflict resolution strategy to use
     * @return Result containing details of applied changes
     */
    suspend fun applySnapshot(
        snapshot: SyncSnapshot,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.PREFER_NEWER
    ): Result<SyncResult>
}

/**
 * Represents the current state of synchronization.
 */
sealed class SyncStatus {
    /** Sync is not configured or disabled. */
    data object Disabled : SyncStatus()

    /** Sync is idle and ready. */
    data object Idle : SyncStatus()

    /** Currently syncing. */
    data class Syncing(val progress: Int = 0) : SyncStatus()

    /** Sync completed successfully. */
    data class Success(val result: SyncResult) : SyncStatus()

    /** Sync failed with an error. */
    data class Error(val message: String, val throwable: Throwable? = null) : SyncStatus()
}

/**
 * Strategy for resolving conflicts when the same data has been modified
 * on multiple devices.
 */
enum class ConflictResolutionStrategy {
    /** Prefer the most recently modified version based on timestamp. */
    PREFER_NEWER,

    /** Prefer the local version in case of conflict. */
    PREFER_LOCAL,

    /** Prefer the remote version in case of conflict. */
    PREFER_REMOTE,

    /** Attempt to merge changes intelligently (e.g., union of favorites). */
    MERGE
}
