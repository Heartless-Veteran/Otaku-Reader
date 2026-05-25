package app.otakureader.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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

@HiltViewModel
class SavedFeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedFeedState())
    val state: StateFlow<SavedFeedState> = _state.asStateFlow()

    private val _effect = Channel<SavedFeedEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        feedRepository.getFeedSources()
            .onEach { sources -> _state.update { it.copy(sources = sources) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SavedFeedEvent) {
        when (event) {
            is SavedFeedEvent.AddSource -> addSource(event.sourceName)
            is SavedFeedEvent.RemoveSource -> removeSource(event.sourceId)
            is SavedFeedEvent.ToggleSource -> toggleSource(event.sourceId, event.enabled)
        }
    }

    private fun addSource(sourceName: String) {
        if (sourceName.isBlank()) return
        viewModelScope.launch {
            try {
                val sourceId = sourceName.hashCode().toLong().let { if (it < 0) -it else it }
                feedRepository.addFeedSource(sourceId, sourceName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(SavedFeedEffect.ShowSnackbar("Failed to add source: ${e.message}"))
            }
        }
    }

    private fun removeSource(sourceId: Long) {
        viewModelScope.launch {
            try {
                feedRepository.removeFeedSource(sourceId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(SavedFeedEffect.ShowSnackbar("Failed to remove source: ${e.message}"))
            }
        }
    }

    private fun toggleSource(sourceId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                feedRepository.toggleFeedSource(sourceId, enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(SavedFeedEffect.ShowSnackbar("Failed to update source: ${e.message}"))
            }
        }
    }
}
