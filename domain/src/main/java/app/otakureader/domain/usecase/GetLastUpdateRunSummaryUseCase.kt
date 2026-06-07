package app.otakureader.domain.usecase

import app.otakureader.domain.model.UpdateRunSummary
import app.otakureader.domain.repository.UpdateRunSummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Returns a [Flow] emitting the most recent [UpdateRunSummary], or null if no run has been
 * recorded yet. The Updates screen uses this to display a diagnostics card after each check.
 */
class GetLastUpdateRunSummaryUseCase @Inject constructor(
    private val repository: UpdateRunSummaryRepository,
) {
    operator fun invoke(): Flow<UpdateRunSummary?> =
        repository.getRecentRuns().map { it.firstOrNull() }
}
