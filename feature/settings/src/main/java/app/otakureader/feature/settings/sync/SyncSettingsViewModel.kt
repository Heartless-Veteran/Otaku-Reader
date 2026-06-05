package app.otakureader.feature.settings.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.SyncSettingsStore
import app.otakureader.domain.repository.SyncRepository
import app.otakureader.domain.scheduler.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Reader Sync settings screen.
 *
 * Follows the project's MVI pattern:
 * - Exposes immutable [SyncSettingsState] via [uiState].
 * - Accepts [SyncSettingsEvent] intents via [onEvent].
 * - Emits one-shot [SyncSettingsEffect] side-effects via [effect].
 *
 * Uses the domain-layer [SyncScheduler] interface so this ViewModel has no
 * dependency on WorkManager or any data-layer class directly.  This is
 * consistent with how [TrackerSyncScheduler] is used from the Tracking settings.
 */
@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val syncSettingsStore: SyncSettingsStore,
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncSettingsState())
    val uiState: StateFlow<SyncSettingsState> = _uiState.asStateFlow()

    private val _effect = Channel<SyncSettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        // Observe each persistent value independently so that transient UI flags
        // (isSyncing, lastSyncResult) are never overwritten by a preference emission.
        syncSettingsStore.serverUrl
            .onEach { v -> _uiState.update { it.copy(serverUrl = v) } }
            .launchIn(viewModelScope)
        syncSettingsStore.bearerToken
            .onEach { v -> _uiState.update { it.copy(bearerToken = v) } }
            .launchIn(viewModelScope)
        syncSettingsStore.deviceId
            .onEach { v -> _uiState.update { it.copy(deviceId = v) } }
            .launchIn(viewModelScope)
        syncRepository.observeQueueSize()
            .onEach { v -> _uiState.update { it.copy(queueSize = v) } }
            .launchIn(viewModelScope)

        // Ensure a stable device ID is generated on first launch.
        viewModelScope.launch { syncSettingsStore.ensureDeviceId() }
    }

    fun onEvent(event: SyncSettingsEvent) {
        when (event) {
            is SyncSettingsEvent.SetServerUrl -> {
                _uiState.update { it.copy(serverUrl = event.url) }
            }

            is SyncSettingsEvent.SetBearerToken -> {
                _uiState.update { it.copy(bearerToken = event.token) }
            }

            SyncSettingsEvent.SaveSettings -> viewModelScope.launch {
                syncSettingsStore.setServerUrl(_uiState.value.serverUrl)
                syncSettingsStore.setBearerToken(_uiState.value.bearerToken)
                _effect.send(SyncSettingsEffect.ShowSnackbar("Sync settings saved"))
                // Reschedule the periodic worker with the (possibly new) server URL.
                if (_uiState.value.serverUrl.isNotBlank()) {
                    syncScheduler.schedulePeriodicSync()
                }
            }

            SyncSettingsEvent.SyncNow -> viewModelScope.launch {
                if (_uiState.value.serverUrl.isBlank()) {
                    _effect.send(SyncSettingsEffect.ShowSnackbar("No sync server configured"))
                    return@launch
                }
                _uiState.update { it.copy(isSyncing = true) }
                syncScheduler.enqueueSingleSync()
                _uiState.update { it.copy(isSyncing = false, lastSyncResult = "Sync enqueued") }
            }

            SyncSettingsEvent.TestConnection -> viewModelScope.launch {
                val url = _uiState.value.serverUrl
                if (url.isBlank()) {
                    _effect.send(SyncSettingsEffect.ShowSnackbar("Enter a server URL first"))
                    return@launch
                }
                // A real connection test would make a HEAD request; for now we
                // simply report the URL that would be used so the user can verify it.
                _effect.send(SyncSettingsEffect.ShowSnackbar("Will connect to: $url"))
            }
        }
    }
}
