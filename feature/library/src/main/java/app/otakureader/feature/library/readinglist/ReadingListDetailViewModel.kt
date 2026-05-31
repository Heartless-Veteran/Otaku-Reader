package app.otakureader.feature.library.readinglist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.navigation.Route
import app.otakureader.domain.repository.ReadingListRepository
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
class ReadingListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readingListRepository: ReadingListRepository,
) : ViewModel() {

    private val listId: Long = savedStateHandle.toRoute<Route.ReadingListDetail>().listId

    private val _state = MutableStateFlow(ReadingListDetailState())
    val state: StateFlow<ReadingListDetailState> = _state.asStateFlow()

    private val _effect = Channel<ReadingListDetailEffect>(Channel.BUFFERED)
    val effect: Flow<ReadingListDetailEffect> = _effect.receiveAsFlow()

    init {
        readingListRepository.getListWithManga(listId)
            .onEach { (list, manga) ->
                _state.update { it.copy(list = list, manga = manga, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ReadingListDetailEvent) {
        when (event) {
            is ReadingListDetailEvent.RemoveManga -> removeManga(event.mangaId)
            is ReadingListDetailEvent.OpenManga ->
                viewModelScope.launch { _effect.send(ReadingListDetailEffect.NavigateToManga(event.mangaId)) }
        }
    }

    private fun removeManga(mangaId: Long) {
        viewModelScope.launch {
            try {
                readingListRepository.removeMangaFromList(listId, mangaId)
                _effect.send(ReadingListDetailEffect.ShowSnackbar("Removed from list"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ReadingListDetailEffect.ShowSnackbar("Failed to remove: ${e.message}"))
            }
        }
    }
}
