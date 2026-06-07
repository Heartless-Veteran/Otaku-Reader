package app.otakureader.data.repository

import app.otakureader.core.database.dao.UpdateRunSummaryDao
import app.otakureader.core.database.entity.UpdateRunSummaryEntity
import app.otakureader.domain.model.UpdateRunSummary
import app.otakureader.domain.repository.UpdateRunSummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRunSummaryRepositoryImpl @Inject constructor(
    private val dao: UpdateRunSummaryDao,
) : UpdateRunSummaryRepository {

    override fun getRecentRuns(): Flow<List<UpdateRunSummary>> =
        dao.getRecentRuns().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insert(summary: UpdateRunSummary) {
        dao.insert(summary.toEntity())
    }

    override suspend fun deleteOlderThan(cutoffMs: Long) {
        dao.deleteOlderThan(cutoffMs)
    }

    private fun UpdateRunSummaryEntity.toDomain() = UpdateRunSummary(
        id = id,
        timestamp = timestamp,
        checkedCount = checkedCount,
        newChaptersCount = newChaptersCount,
        skippedCount = skippedCount,
        failedCount = failedCount,
        durationMs = durationMs,
    )

    private fun UpdateRunSummary.toEntity() = UpdateRunSummaryEntity(
        id = id,
        timestamp = timestamp,
        checkedCount = checkedCount,
        newChaptersCount = newChaptersCount,
        skippedCount = skippedCount,
        failedCount = failedCount,
        durationMs = durationMs,
    )
}
