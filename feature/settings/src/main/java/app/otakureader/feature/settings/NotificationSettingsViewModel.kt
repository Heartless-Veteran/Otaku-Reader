package app.otakureader.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsState(
    val smartBatchingEnabled: Boolean = true,
    val perMangaCooldownMinutes: Int = 60,
    val maxIndividualNotifications: Int = 3,
    val respectQuietHours: Boolean = true,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 8,
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val prefs: NotificationPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationSettingsState())
    val state: StateFlow<NotificationSettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.smartBatchingEnabled,
                prefs.perMangaCooldownMinutes,
                prefs.maxIndividualNotifications,
                prefs.respectQuietHours,
            ) { batching, cooldown, max, quiet ->
                _state.update {
                    it.copy(
                        smartBatchingEnabled = batching,
                        perMangaCooldownMinutes = cooldown,
                        maxIndividualNotifications = max,
                        respectQuietHours = quiet,
                    )
                }
            }.collect { }
        }
        viewModelScope.launch {
            combine(prefs.quietHoursStart, prefs.quietHoursEnd) { start, end ->
                _state.update { it.copy(quietHoursStart = start, quietHoursEnd = end) }
            }.collect { }
        }
    }

    fun setSmartBatching(enabled: Boolean) = viewModelScope.launch { prefs.setSmartBatching(enabled) }
    fun setCooldown(minutes: Int) = viewModelScope.launch { prefs.setPerMangaCooldownMinutes(minutes) }
    fun setMaxIndividual(count: Int) = viewModelScope.launch { prefs.setMaxIndividualNotifications(count) }
    fun setRespectQuietHours(enabled: Boolean) = viewModelScope.launch { prefs.setRespectQuietHours(enabled) }

    fun setQuietHoursStart(hour: Int) = viewModelScope.launch {
        prefs.setQuietHours(hour, prefs.quietHoursEnd.first())
    }

    fun setQuietHoursEnd(hour: Int) = viewModelScope.launch {
        prefs.setQuietHours(prefs.quietHoursStart.first(), hour)
    }
}
