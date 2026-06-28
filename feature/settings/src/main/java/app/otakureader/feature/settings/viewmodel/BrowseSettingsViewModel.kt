package app.otakureader.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.feature.settings.SettingsEvent
import app.otakureader.feature.settings.SettingsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseSettingsViewModel @Inject constructor(
    private val generalPreferences: GeneralPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            generalPreferences.showNsfwContent.collect { showNsfw ->
                _state.update { it.copy(showNsfwContent = showNsfw) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetShowNsfwContent -> viewModelScope.launch {
                generalPreferences.setShowNsfwContent(event.enabled)
            }
            else -> Unit
        }
    }
}
