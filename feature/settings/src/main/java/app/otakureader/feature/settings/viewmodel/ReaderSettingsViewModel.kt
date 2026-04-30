package app.otakureader.feature.settings.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.feature.settings.SettingsEffect
import app.otakureader.feature.settings.SettingsEvent
import app.otakureader.feature.settings.SettingsState
import app.otakureader.feature.settings.delegate.ReaderSettingsDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Focused ViewModel for the Reader settings section (reading mode, display, interaction,
 * Webtoon / e-ink specific options, preloading, tap zones, volume keys).
 *
 * Owns only the [ReaderSettingsDelegate] so the reader preferences screen can be unit-tested
 * and navigated to independently of the rest of the settings surface.
 *
 * Part of the work to split the previously monolithic `SettingsViewModel` into per-section
 * ViewModels aligned with the settings sections.
 */
@HiltViewModel
class ReaderSettingsViewModel @Inject constructor(
    private val readerDelegate: ReaderSettingsDelegate,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        readerDelegate.startObserving(viewModelScope) { reducer -> _state.update(reducer) }
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            val handled = readerDelegate.handleEvent(event) { _effect.send(it) }
            if (!handled) {
                Log.w(TAG, "Unhandled event in ReaderSettingsViewModel: $event")
            }
        }
    }

    companion object {
        private const val TAG = "ReaderSettingsViewModel"
    }
}
