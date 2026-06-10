package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.otakureader.core.database.entity.UpdateRunSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateRunSummaryDao {
    @Query("SELECT * FROM update_run_summary ORDER BY timestamp DESC LIMIT 20")
    fun getRecentRuns(): Flow<List<UpdateRunSummaryEntity>>

    @Insert
    suspend fun insert(summary: UpdateRunSummaryEntity): Long

    @Query("DELETE FROM update_run_summary WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
