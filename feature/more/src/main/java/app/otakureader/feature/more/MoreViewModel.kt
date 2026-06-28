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
    data class SetIncognitoMode(val enabled: Boolean) : MoreEvent
    data class SetDownloadedOnly(val enabled: Boolean) : MoreEvent
}

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

    private val goalProgress = combine(
        readingGoalPreferences.dailyChapterGoal,
        readingGoalPreferences.weeklyChapterGoal,
    ) { daily, weekly -> daily to weekly }
        .distinctUntilChanged()
        .flatMapLatest { (daily, weekly) ->
            statisticsRepository.getReadingGoalProgress(daily, weekly)
        }

    val state: StateFlow<MoreState> =
        combine(
            goalProgress,
            readerSettingsRepository.incognitoMode,
            generalPreferences.downloadedOnly,
        ) { goalProgress, incognito, downloadedOnly ->
            MoreState(
                currentStreak = goalProgress.currentStreak,
                todayChaptersRead = goalProgress.dailyProgress,
                dailyGoal = goalProgress.dailyGoal,
                incognitoMode = incognito,
                downloadedOnly = downloadedOnly,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT_MS),
                initialValue = MoreState(),
            )

    fun onEvent(event: MoreEvent) {
        when (event) {
            is MoreEvent.SetIncognitoMode -> viewModelScope.launch {
                readerSettingsRepository.setIncognitoMode(event.enabled)
            }
            is MoreEvent.SetDownloadedOnly -> viewModelScope.launch {
                generalPreferences.setDownloadedOnly(event.enabled)
            }
        }
    }
}
