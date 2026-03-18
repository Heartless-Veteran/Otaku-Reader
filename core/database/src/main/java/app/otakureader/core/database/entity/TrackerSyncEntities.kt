package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Entity for tracker sync state - manages 2-way sync with external trackers.
 */
@Entity(
    tableName = "tracker_sync_state",
    indices = [
        Index(value = ["mangaId", "trackerId"], unique = true),
        Index(value = ["syncStatus"])
    ]
)
data class TrackerSyncStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mangaId: Long,
    val trackerId: Int,
    val remoteId: String,
    
    // Local state
    val localLastChapterRead: Float,
    val localTotalChapters: Int,
    val localStatus: Int, // MangaStatus ordinal
    val localLastModified: Instant,
    
    // Remote state
    val remoteLastChapterRead: Float,
    val remoteTotalChapters: Int,
    val remoteStatus: Int,
    val remoteLastModified: Instant?,
    
    // Sync state
    val syncStatus: Int, // SyncStatus ordinal
    val lastSyncAttempt: Instant?,
    val lastSuccessfulSync: Instant?,
    val syncError: String?
)

@Entity(
    tableName = "sync_configuration",
    indices = [Index(value = ["trackerId"], unique = true)]
)
data class SyncConfigurationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerId: Int,
    val enabled: Boolean = true,
    val syncDirection: Int, // SyncDirection ordinal
    val conflictResolution: Int, // ConflictResolution ordinal
    val autoSyncInterval: Long = 300_000,
    val syncOnChapterRead: Boolean = true,
    val syncOnMarkComplete: Boolean = true
)
