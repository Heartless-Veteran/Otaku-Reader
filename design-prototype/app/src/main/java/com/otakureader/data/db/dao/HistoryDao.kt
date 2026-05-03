package com.otakureader.data.db.dao

import androidx.room.*
import com.otakureader.data.db.entities.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY readAt DESC LIMIT 100")
    fun getRecent(): Flow<List<HistoryEntity>>

    @Query("SELECT COUNT(*) FROM history")
    fun getTotalChaptersRead(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)
}
