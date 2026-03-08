package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.otakureader.core.database.entity.ChapterEntity
import app.otakureader.core.database.entity.ReadingHistoryEntity
import kotlinx.coroutines.flow.Flow

data class ChapterWithHistoryEntity(
    @Embedded val chapter: ChapterEntity,
    @Embedded(prefix = "h_") val history: ReadingHistoryEntity
)

@Dao
interface ReadingHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: ReadingHistoryEntity)

    @Query("SELECT * FROM reading_history ORDER BY read_at DESC")
    fun observeHistory(): Flow<List<ReadingHistoryEntity>>

    @Query(
        """
        SELECT chapters.*,
               reading_history.id          AS h_id,
               reading_history.chapter_id  AS h_chapter_id,
               reading_history.read_at     AS h_read_at,
               reading_history.read_duration_ms AS h_read_duration_ms
        FROM reading_history
        INNER JOIN chapters ON chapters.id = reading_history.chapter_id
        ORDER BY reading_history.read_at DESC
        """
    )
    fun observeChaptersWithHistory(): Flow<List<ChapterWithHistoryEntity>>

    @Query("DELETE FROM reading_history WHERE read_at < :timestamp")
    suspend fun deleteHistoryBefore(timestamp: Long)

    @Query("DELETE FROM reading_history WHERE chapter_id = :chapterId")
    suspend fun deleteHistoryForChapter(chapterId: Long)

    @Query("DELETE FROM reading_history")
    suspend fun deleteAll()
}
