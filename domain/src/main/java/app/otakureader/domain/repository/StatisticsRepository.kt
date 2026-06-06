package app.otakureader.domain.repository

import app.otakureader.domain.model.ReadingGoal
import app.otakureader.domain.model.ReadingStats
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun getReadingStats(sinceMs: Long? = null): Flow<ReadingStats>
    fun getReadingGoalProgress(dailyGoal: Int, weeklyGoal: Int): Flow<ReadingGoal>

    /**
     * Average per-chapter reading duration in milliseconds derived from the user's history.
     * Emits the user's running average; emits the fallback (5 min) when no history exists.
     */
    fun getAverageChapterDurationMs(): Flow<Long>
}
