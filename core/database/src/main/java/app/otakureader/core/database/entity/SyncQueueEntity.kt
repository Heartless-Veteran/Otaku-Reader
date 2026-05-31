package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a queued reader-progress sync operation.
 *
 * Each row holds a serialized [payload] (JSON) for a single chapter-read event that
 * has not yet been successfully pushed to the self-hosted sync server.  The
 * [attempts] counter and [lastError] fields allow the drain loop to apply
 * exponential-back-off and surface error details for debugging.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val mangaId: Long,
    val payload: String,
    val attempts: Int = 0,
    val createdAt: Long,
    val lastError: String? = null,
)
