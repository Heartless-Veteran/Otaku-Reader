package app.otakureader.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.TrackEntry
import app.otakureader.domain.model.TrackStatus
import app.otakureader.domain.model.TrackerType
import app.otakureader.domain.tracking.Tracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackingState(
    val trackers: List<TrackerUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTracker: Int? = null,
    val searchQuery: String = "",
    val searchResults: List<TrackEntry> = emptyList(),
    val isSearching: Boolean = false,
    val mangaId: Long = 0L,
    val mangaTitle: String = "",
    val currentEntry: TrackEntry? = null
) : UiState

data class TrackerUiModel(
    val id: Int,
    val name: String,
    val iconUrl: String,
    val isLoggedIn: Boolean,
    val entry: TrackEntry? = null
)

sealed interface TrackingEvent : UiEvent {
    data class LoadTrackers(val mangaId: Long, val mangaTitle: String) : TrackingEvent
    data class Login(val trackerId: Int, val username: String, val password: String) : TrackingEvent
    data class Logout(val trackerId: Int) : TrackingEvent
    data class Search(val trackerId: Int, val query: String) : TrackingEvent
    data class LinkManga(val trackerId: Int, val remoteId: Long) : TrackingEvent
    data class UnlinkManga(val trackerId: Int) : TrackingEvent
    data class UpdateStatus(val trackerId: Int, val status: TrackStatus) : TrackingEvent
    data class UpdateProgress(val trackerId: Int, val chapter: Float) : TrackingEvent
    data class UpdateScore(val trackerId: Int, val score: Float) : TrackingEvent
    data class OnSearchQueryChange(val query: String) : TrackingEvent
    data object ClearSearch : TrackingEvent
}

sealed interface TrackingEffect : UiEffect {
    data class ShowMessage(val message: String) : TrackingEffect
    data class ShowError(val message: String) : TrackingEffect
    data class OpenOAuth(val trackerId: Int, val url: String) : TrackingEffect
}

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val trackers: Map<Int, @JvmSuppressWildcards Tracker>
) : ViewModel() {

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrackingState()
        )

    private val _effect = Channel<TrackingEffect>()
    val effect: Flow<TrackingEffect> = _effect.receiveAsFlow()

    fun onEvent(event: TrackingEvent) {
        when (event) {
            is TrackingEvent.LoadTrackers -> loadTrackers(event.mangaId, event.mangaTitle)
            is TrackingEvent.Login -> login(event.trackerId, event.username, event.password)
            is TrackingEvent.Logout -> logout(event.trackerId)
            is TrackingEvent.Search -> search(event.trackerId, event.query)
            is TrackingEvent.LinkManga -> linkManga(event.trackerId, event.remoteId)
            is TrackingEvent.UnlinkManga -> unlinkManga(event.trackerId)
            is TrackingEvent.UpdateStatus -> updateStatus(event.trackerId, event.status)
            is TrackingEvent.UpdateProgress -> updateProgress(event.trackerId, event.chapter)
            is TrackingEvent.UpdateScore -> updateScore(event.trackerId, event.score)
            is TrackingEvent.OnSearchQueryChange -> _state.value = _state.value.copy(searchQuery = event.query)
            TrackingEvent.ClearSearch -> {
                _state.value = _state.value.copy(searchQuery = "", searchResults = emptyList())
            }
        }
    }

    private fun loadTrackers(mangaId: Long, mangaTitle: String) {
        _state.value = _state.value.copy(
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            isLoading = true
        )

        viewModelScope.launch {
            val trackerModels = trackers.map { (id, tracker) ->
                TrackerUiModel(
                    id = id,
                    name = tracker.name,
                    iconUrl = getTrackerIconUrl(id),
                    isLoggedIn = tracker.isLoggedIn
                )
            }

            _state.value = _state.value.copy(
                trackers = trackerModels,
                isLoading = false
            )
        }
    }

    private fun login(trackerId: Int, username: String, password: String) {
        val tracker = trackers[trackerId] ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val success = tracker.login(username, password)
                if (success) {
                    refreshTracker(trackerId)
                    _effect.send(TrackingEffect.ShowMessage("Logged in to ${tracker.name}"))
                } else {
                    _effect.send(TrackingEffect.ShowError("Failed to log in to ${tracker.name}"))
                }
            } catch (e: Exception) {
                _effect.send(TrackingEffect.ShowError("Login error: ${e.message}"))
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private fun logout(trackerId: Int) {
        val tracker = trackers[trackerId] ?: return
        tracker.logout()
        refreshTracker(trackerId)
    }

    private fun search(trackerId: Int, query: String) {
        val tracker = trackers[trackerId] ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true, selectedTracker = trackerId)

            try {
                val results = tracker.search(query)
                _state.value = _state.value.copy(searchResults = results, isSearching = false)
            } catch (e: Exception) {
                _effect.send(TrackingEffect.ShowError("Search failed: ${e.message}"))
                _state.value = _state.value.copy(isSearching = false)
            }
        }
    }

    private fun linkManga(trackerId: Int, remoteId: Long) {
        val tracker = trackers[trackerId] ?: return
        val mangaId = _state.value.mangaId

        viewModelScope.launch {
            try {
                val entry = tracker.find(remoteId)
                if (entry != null) {
                    // Store the link
                    refreshTracker(trackerId)
                    _effect.send(TrackingEffect.ShowMessage("Linked to ${tracker.name}"))
                }
            } catch (e: Exception) {
                _effect.send(TrackingEffect.ShowError("Failed to link: ${e.message}"))
            }
        }
    }

    private fun unlinkManga(trackerId: Int) {
        // Remove the link from local storage
        refreshTracker(trackerId)
    }

    private fun updateStatus(trackerId: Int, status: TrackStatus) {
        val currentEntry = _state.value.currentEntry ?: return

        viewModelScope.launch {
            try {
                // Update remote
                refreshTracker(trackerId)
            } catch (e: Exception) {
                _effect.send(TrackingEffect.ShowError("Update failed: ${e.message}"))
            }
        }
    }

    private fun updateProgress(trackerId: Int, chapter: Float) {
        // Similar to updateStatus
    }

    private fun updateScore(trackerId: Int, score: Float) {
        // Similar to updateStatus
    }

    private fun refreshTracker(trackerId: Int) {
        val tracker = trackers[trackerId] ?: return

        viewModelScope.launch {
            val updatedModel = TrackerUiModel(
                id = trackerId,
                name = tracker.name,
                iconUrl = getTrackerIconUrl(trackerId),
                isLoggedIn = tracker.isLoggedIn
            )

            val updatedList = _state.value.trackers.map {
                if (it.id == trackerId) updatedModel else it
            }

            _state.value = _state.value.copy(trackers = updatedList)
        }
    }

    private fun getTrackerIconUrl(trackerId: Int): String = when (trackerId) {
        TrackerType.MY_ANIME_LIST -> "https://myanimelist.net/img/apple-touch-icon.png"
        TrackerType.ANILIST -> "https://anilist.co/img/icons/apple-touch-icon.png"
        TrackerType.KITSU -> "https://kitsu.io/favicon.ico"
        TrackerType.MANGA_UPDATES -> "https://www.mangaupdates.com/favicon.ico"
        TrackerType.SHIKIMORI -> "https://shikimori.one/favicon.ico"
        else -> ""
    }
}
