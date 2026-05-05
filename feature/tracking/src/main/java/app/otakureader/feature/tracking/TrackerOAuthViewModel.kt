package app.otakureader.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.data.tracking.TrackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [TrackerOAuthScreen].
 *
 * Exchanges the OAuth authorization code for an access token via [TrackManager].
 */
@HiltViewModel
class TrackerOAuthViewModel @Inject constructor(
    private val trackManager: TrackManager,
) : ViewModel() {

    private val _state = MutableStateFlow(TrackerOAuthState())
    val state: StateFlow<TrackerOAuthState> = _state

    /**
     * Exchange the OAuth [code] for an access token for the given [tracker].
     *
     * @param tracker Tracker ID (e.g., "anilist", "mal", "kitsu", "shikimori").
     * @param code Authorization code from the OAuth redirect.
     */
    fun exchangeCode(tracker: String, code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, success = false) }

            try {
                val result = trackManager.login(tracker, code)
                if (result) {
                    _state.update { it.copy(isLoading = false, success = true) }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Authentication failed. The authorization code may have expired or been reused."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred during authentication."
                    )
                }
            }
        }
    }
}

/**
 * UI state for [TrackerOAuthScreen].
 */
data class TrackerOAuthState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
)
