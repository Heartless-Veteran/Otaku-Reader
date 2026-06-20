package app.otakureader.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.ReadingGoalPreferences
import app.otakureader.domain.usecase.GetReadingStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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
    private val getReadingStatsUseCase: GetReadingStatsUseCase,
    private val statisticsRepository: StatisticsRepository,
    private val readingGoalPreferences: ReadingGoalPreferences,
) : ViewModel() {

    companion object {
        private const val SUBSCRIBE_STOP_TIMEOUT_MS = 5_000L
    }

    /**
     * Combines:
     *  - [GetReadingStatsUseCase] → provides [currentStreak] (all-time, no date filter)
     *  - [ReadingGoalPreferences.dailyChapterGoal] → user's configured daily target
     *  - [StatisticsRepository.getReadingGoalProgress] → today's actual progress
     *
     * Why flatMapLatest here?  When the user changes their daily goal in Settings, the
     * goal preference emits a new value.  flatMapLatest cancels the previous inner
     * combine (which was fetching progress against the old goal) and immediately starts
     * a new one with the updated goal — so the widget always reflects the live setting
     * without any stale data.
     */
    val state: StateFlow<MoreState> =
        combine(
            readingGoalPreferences.dailyChapterGoal,
            readingGoalPreferences.weeklyChapterGoal,
        ) { daily, weekly -> Pair(daily, weekly) }
            .distinctUntilChanged()  // prevent inner flow restarts from unrelated DataStore edits
            .flatMapLatest { (dailyGoal, weeklyGoal) ->
                combine(
                    getReadingStatsUseCase(),
                    statisticsRepository.getReadingGoalProgress(dailyGoal, weeklyGoal),
                ) { stats, goalProgress ->
                    MoreState(
                        currentStreak = stats.currentStreak,
                        todayChaptersRead = goalProgress.dailyProgress,
                        dailyGoal = goalProgress.dailyGoal,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT_MS),
                initialValue = MoreState(),
            )
}
