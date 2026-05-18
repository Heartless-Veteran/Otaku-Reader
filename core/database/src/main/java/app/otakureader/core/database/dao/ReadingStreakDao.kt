package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.otakureader.core.database.entity.ReadingStreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: ReadingStreakEntity)

    @Query("SELECT * FROM reading_streaks ORDER BY date DESC")
    fun getAllStreaks(): Flow<List<ReadingStreakEntity>>

    @Query("SELECT * FROM reading_streaks WHERE date = :date")
    suspend fun getStreakForDate(date: String): ReadingStreakEntity?

    @Query("SELECT * FROM reading_streaks ORDER BY date DESC LIMIT 1")
    suspend fun getLatestStreak(): ReadingStreakEntity?

    @Query("SELECT COUNT(*) FROM reading_streaks")
    suspend fun getTotalDaysRead(): Int

    @Query("DELETE FROM reading_streaks WHERE date < :beforeDate")
    suspend fun deleteOldStreaks(beforeDate: String)
}
