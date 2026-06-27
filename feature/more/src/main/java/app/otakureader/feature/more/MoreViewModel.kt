package app.otakureader.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.core.preferences.ReadingGoalPreferences
import app.otakureader.domain.repository.ReaderSettingsRepository
import app.otakureader.domain.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoreState(
    val currentStreak: Int = 0,
    val todayChaptersRead: Int = 0,
    val dailyGoal: Int = 0,
    val incognitoMode: Boolean = false,
    val downloadedOnly: Boolean = false,
)

sealed interface MoreEvent {
    data object ToggleIncognitoMode : MoreEvent
    data object ToggleDownloadedOnly : MoreEvent
}

private data class MoreParams(
    val dailyGoal: Int,
    val weeklyGoal: Int,
    val incognitoMode: Boolean,
    val downloadedOnly: Boolean,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val readingGoalPreferences: ReadingGoalPreferences,
    private val readerSettingsRepository: ReaderSettingsRepository,
    private val generalPreferences: GeneralPreferences,
) : ViewModel() {

    companion object {
        private const val SUBSCRIBE_STOP_TIMEOUT_MS = 5_000L
    }

    val state: StateFlow<MoreState> =
        combine(
            readingGoalPreferences.dailyChapterGoal,
            readingGoalPreferences.weeklyChapterGoal,
            readerSettingsRepository.incognitoMode,
            generalPreferences.downloadedOnly,
        ) { dailyGoal, weeklyGoal, incognito, downloadedOnly ->
            MoreParams(dailyGoal, weeklyGoal, incognito, downloadedOnly)
        }
            .distinctUntilChanged()
            .flatMapLatest { params ->
                statisticsRepository.getReadingGoalProgress(params.dailyGoal, params.weeklyGoal)
                    .map { goalProgress ->
                        MoreState(
                            currentStreak = goalProgress.currentStreak,
                            todayChaptersRead = goalProgress.dailyProgress,
                            dailyGoal = goalProgress.dailyGoal,
                            incognitoMode = params.incognitoMode,
                            downloadedOnly = params.downloadedOnly,
                        )
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT_MS),
                initialValue = MoreState(),
            )

    fun onEvent(event: MoreEvent) {
        when (event) {
            MoreEvent.ToggleIncognitoMode -> {
                viewModelScope.launch {
                    readerSettingsRepository.setIncognitoMode(!state.value.incognitoMode)
                }
            }
            MoreEvent.ToggleDownloadedOnly -> {
                viewModelScope.launch {
                    generalPreferences.setDownloadedOnly(!state.value.downloadedOnly)
                }
            }
        }
    }
}
