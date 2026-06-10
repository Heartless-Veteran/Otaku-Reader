package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.otakureader.core.database.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the [SyncQueueEntity] table.
 *
 * All reads that need to react to queue changes are exposed as [Flow]; one-shot
 * reads and writes are plain suspend functions.
 */
@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncQueueEntity): Long

    /** Observe all pending items ordered oldest-first (for UI badge). */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    fun observePending(): Flow<List<SyncQueueEntity>>

    /**
     * Observe up to [limit] pending items ordered oldest-first, skipping items that have
     * already exceeded [maxAttempts] to avoid re-queuing permanently failed entries.
     */
    @Query("SELECT * FROM sync_queue WHERE attempts < :maxAttempts ORDER BY createdAt ASC LIMIT :limit")
    fun getPending(
        limit: Int = DEFAULT_BATCH_SIZE,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    ): Flow<List<SyncQueueEntity>>

    companion object {
        const val DEFAULT_BATCH_SIZE = 50
        const val DEFAULT_MAX_ATTEMPTS = 5
    }

    @Delete
    suspend fun delete(entity: SyncQueueEntity)

    /** Increment attempt count and record the latest error message for an item. */
    @Query("UPDATE sync_queue SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun recordAttemptFailure(id: Long, error: String?)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Removes items whose attempt count reached [maxAttempts]. [getPending] already skips
     * them, so without this they would accumulate in the table forever.
     */
    @Query("DELETE FROM sync_queue WHERE attempts >= :maxAttempts")
    suspend fun pruneExhausted(maxAttempts: Int = DEFAULT_MAX_ATTEMPTS)

    /** Observe the total number of items waiting to be synced (drives UI badge). */
    @Query("SELECT COUNT(*) FROM sync_queue")
    fun observeQueueSize(): Flow<Int>
}
