package app.otakureader.domain.repository

import app.otakureader.domain.model.Achievement
import app.otakureader.domain.model.AchievementDefinition
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    fun observeAll(): Flow<List<Achievement>>
    suspend fun unlock(key: AchievementDefinition)
    suspend fun updateProgress(key: AchievementDefinition, progress: Int)
    suspend fun initializeDefaults()
}
