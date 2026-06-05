package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.otakureader.core.database.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE definitionKey = :key LIMIT 1")
    suspend fun getByKey(key: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<AchievementEntity>)

    @Query("UPDATE achievements SET unlockedAt = :timestamp WHERE definitionKey = :key AND unlockedAt = 0")
    suspend fun unlock(key: String, timestamp: Long)

    @Query("UPDATE achievements SET progress = :progress WHERE definitionKey = :key")
    suspend fun updateProgress(key: String, progress: Int)
}
