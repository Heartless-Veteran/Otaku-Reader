package app.otakureader.data.repository

import app.otakureader.core.database.dao.AchievementDao
import app.otakureader.core.database.entity.AchievementEntity
import app.otakureader.domain.model.Achievement
import app.otakureader.domain.model.AchievementDefinition
import app.otakureader.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val dao: AchievementDao
) : AchievementRepository {

    override fun observeAll(): Flow<List<Achievement>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun unlock(key: AchievementDefinition) {
        dao.unlock(key.name, System.currentTimeMillis())
    }

    override suspend fun updateProgress(key: AchievementDefinition, progress: Int) {
        dao.updateProgress(key.name, progress)
    }

    override suspend fun initializeDefaults() {
        AchievementDefinition.entries.forEach { def ->
            dao.insertIfAbsent(
                AchievementEntity(
                    definitionKey = def.name,
                    target = def.target
                )
            )
        }
    }

    private fun AchievementEntity.toDomain(): Achievement {
        val def = runCatching { AchievementDefinition.valueOf(definitionKey) }.getOrNull()
            ?: AchievementDefinition.FIRST_CHAPTER
        return Achievement(
            id = id,
            definition = def,
            unlockedAt = unlockedAt,
            progress = progress,
            target = target.coerceAtLeast(def.target)
        )
    }
}
