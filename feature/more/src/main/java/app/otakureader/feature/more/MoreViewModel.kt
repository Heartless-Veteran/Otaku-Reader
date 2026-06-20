package app.otakureader.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.ReadingGoalPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import app.otakureader.domain.repository.StatisticsRepository
import javax.inject.Inject

data class MoreState(
    val currentStreak: Int = 0,
    val todayChaptersRead: Int = 0,
    val dailyGoal: Int = 0,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val readingGoalPreferences: ReadingGoalPreferences,
) : ViewModel() {

    companion object {
        private const val SUBSCRIBE_STOP_TIMEOUT_MS = 5_000L
    }

    /**
     * Single readingHistoryDao.observeHistory() subscription for the More screen.
     *
     * Previously, GetReadingStatsUseCase and getReadingGoalProgress() each subscribed to the
     * history DAO independently, causing two full history scans per update. Now that ReadingGoal
     * includes currentStreak (computed in the same scan as dailyProgress/weeklyProgress),
     * GetReadingStatsUseCase is no longer needed here.
     *
     * flatMapLatest: when the user changes their daily/weekly goal in Settings, the inner flow
     * is cancelled and restarted with the updated goals so the widget stays in sync.
     */
    val state: StateFlow<MoreState> =
        combine(
            readingGoalPreferences.dailyChapterGoal,
            readingGoalPreferences.weeklyChapterGoal,
        ) { daily, weekly -> Pair(daily, weekly) }
            .distinctUntilChanged()
            .flatMapLatest { (dailyGoal, weeklyGoal) ->
                statisticsRepository.getReadingGoalProgress(dailyGoal, weeklyGoal)
            }
            .map { goalProgress ->
                MoreState(
                    currentStreak = goalProgress.currentStreak,
                    todayChaptersRead = goalProgress.dailyProgress,
                    dailyGoal = goalProgress.dailyGoal,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT_MS),
                initialValue = MoreState(),
            )
}
