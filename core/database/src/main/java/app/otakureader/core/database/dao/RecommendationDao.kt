package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.otakureader.core.database.entity.ReadingPatternEntity
import app.otakureader.core.database.entity.RecommendationEntity
import app.otakureader.core.database.entity.RecommendationRefreshEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for recommendation-related database operations.
 */
@Dao
interface RecommendationDao {

    // --- Recommendations ---

    @Query("SELECT * FROM recommendations WHERE dismissed = 0 ORDER BY confidenceScore DESC, generatedAt DESC")
    fun observeRecommendations(): Flow<List<RecommendationEntity>>

    @Query("SELECT * FROM recommendations WHERE dismissed = 0 ORDER BY confidenceScore DESC, generatedAt DESC")
    suspend fun getRecommendations(): List<RecommendationEntity>

    @Query("SELECT * FROM recommendations WHERE id = :id")
    suspend fun getRecommendationById(id: String): RecommendationEntity?

    @Query("SELECT * FROM recommendations WHERE recommendationType = :type AND dismissed = 0 ORDER BY confidenceScore DESC")
    suspend fun getRecommendationsByType(type: String): List<RecommendationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(recommendation: RecommendationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: List<RecommendationEntity>)

    @Query("UPDATE recommendations SET viewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: String)

    @Query("UPDATE recommendations SET actioned = 1, actionedMangaId = :mangaId WHERE id = :id")
    suspend fun markAsActioned(id: String, mangaId: Long)

    @Query("UPDATE recommendations SET dismissed = 1 WHERE id = :id")
    suspend fun dismissRecommendation(id: String)

    @Query("DELETE FROM recommendations WHERE expiresAt < :currentTime OR dismissed = 1")
    suspend fun deleteExpiredAndDismissed(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM recommendations")
    suspend fun deleteAllRecommendations()

    @Query("SELECT COUNT(*) FROM recommendations WHERE dismissed = 0 AND expiresAt > :currentTime")
    suspend fun getValidRecommendationCount(currentTime: Long = System.currentTimeMillis()): Int

    // --- Reading Patterns ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingPattern(pattern: ReadingPatternEntity)

    @Query("SELECT * FROM reading_patterns ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatestReadingPattern(): ReadingPatternEntity?

    @Query("DELETE FROM reading_patterns WHERE generatedAt < :timestamp")
    suspend fun deleteOldPatterns(timestamp: Long)

    // --- Refresh History ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefreshRecord(record: RecommendationRefreshEntity)

    @Query("SELECT * FROM recommendation_refreshes WHERE success = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSuccessfulRefresh(): RecommendationRefreshEntity?

    @Query("SELECT COUNT(*) FROM recommendation_refreshes WHERE timestamp > :since AND success = 1")
    suspend fun getSuccessfulRefreshCountSince(since: Long): Int

    @Query("DELETE FROM recommendation_refreshes WHERE timestamp < :timestamp")
    suspend fun deleteOldRefreshRecords(timestamp: Long)

    // --- Cleanup ---

    @Transaction
    suspend fun clearAll() {
        deleteAllRecommendations()
    }
}
