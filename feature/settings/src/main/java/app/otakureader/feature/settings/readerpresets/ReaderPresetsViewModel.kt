package app.otakureader.feature.settings.readerpresets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.ReaderPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReaderPresetsViewModel @Inject constructor(
    private val readerPreferences: ReaderPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderPresetsState())
    val state: StateFlow<ReaderPresetsState> = _state.asStateFlow()

    private val _effect = Channel<ReaderPresetsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            readerPreferences.presets.collect { presets ->
                _state.update { it.copy(presets = presets) }
            }
        }
    }

    fun onEvent(event: ReaderPresetsEvent) {
        when (event) {
            ReaderPresetsEvent.ShowSaveDialog -> _state.update { it.copy(showSaveDialog = true, saveDialogName = "") }
            ReaderPresetsEvent.HideSaveDialog -> _state.update { it.copy(showSaveDialog = false) }
            is ReaderPresetsEvent.UpdateSaveName -> _state.update { it.copy(saveDialogName = event.name) }
            ReaderPresetsEvent.ConfirmSave -> confirmSave()
            is ReaderPresetsEvent.Apply -> applyPreset(event.preset)
            is ReaderPresetsEvent.Delete -> deletePreset(event.id)
        }
    }

    private fun confirmSave() {
        val name = _state.value.saveDialogName.trim()
        if (name.isBlank()) return
        _state.update { it.copy(showSaveDialog = false) }
        viewModelScope.launch {
            val preset = readerPreferences.captureCurrentAsPreset(name, UUID.randomUUID().toString())
            readerPreferences.savePreset(preset)
        }
    }

    private fun applyPreset(preset: app.otakureader.core.preferences.ReaderPreset) {
        viewModelScope.launch {
            readerPreferences.applyPreset(preset)
            _effect.send(ReaderPresetsEffect.ShowSnackbar("Applied \"${preset.name}\""))
        }
    }

    private fun deletePreset(id: String) {
        viewModelScope.launch { readerPreferences.deletePreset(id) }
    }
}
