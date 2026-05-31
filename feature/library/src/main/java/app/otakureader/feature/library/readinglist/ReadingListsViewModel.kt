package app.otakureader.feature.library.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.model.ReadingList
import app.otakureader.domain.repository.ReadingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReadingListsViewModel @Inject constructor(
    private val readingListRepository: ReadingListRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReadingListsState())
    val state: StateFlow<ReadingListsState> = _state.asStateFlow()

    private val _effect = Channel<ReadingListsEffect>(Channel.BUFFERED)
    val effect: Flow<ReadingListsEffect> = _effect.receiveAsFlow()

    init {
        readingListRepository.getAllLists()
            .flatMapLatest { lists ->
                if (lists.isEmpty()) {
                    flowOf(emptyList<ReadingList>())
                } else {
                    // Combine each list with its item count so the badge stays reactive.
                    combine(lists.map { list -> readingListRepository.getItemCount(list.id) }) { counts ->
                        lists.mapIndexed { i, list -> list.copy(itemCount = counts[i]) }
                    }
                }
            }
            .onEach { enriched ->
                _state.update { it.copy(lists = enriched, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ReadingListsEvent) {
        when (event) {
            is ReadingListsEvent.CreateList -> createList(event.name, event.description)
            is ReadingListsEvent.RenameList -> renameList(event.listId, event.name, event.description)
            is ReadingListsEvent.DeleteList -> deleteList(event.listId)
            is ReadingListsEvent.OpenList -> openList(event.listId)
        }
    }

    private fun createList(name: String, description: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _effect.send(ReadingListsEffect.ShowSnackbar("Name cannot be empty")) }
            return
        }
        viewModelScope.launch {
            try {
                readingListRepository.createList(name = trimmed, description = description?.takeIf { it.isNotBlank() }, color = null)
                _effect.send(ReadingListsEffect.ShowSnackbar("List created"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ReadingListsEffect.ShowSnackbar("Failed to create list: ${e.message}"))
            }
        }
    }

    private fun renameList(listId: Long, name: String, description: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _effect.send(ReadingListsEffect.ShowSnackbar("Name cannot be empty")) }
            return
        }
        viewModelScope.launch {
            try {
                val current = readingListRepository.getListById(listId) ?: return@launch
                readingListRepository.updateList(
                    current.copy(name = trimmed, description = description?.takeIf { it.isNotBlank() })
                )
                _effect.send(ReadingListsEffect.ShowSnackbar("List updated"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ReadingListsEffect.ShowSnackbar("Failed to update list: ${e.message}"))
            }
        }
    }

    private fun deleteList(listId: Long) {
        viewModelScope.launch {
            try {
                readingListRepository.deleteList(listId)
                _effect.send(ReadingListsEffect.ShowSnackbar("List deleted"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ReadingListsEffect.ShowSnackbar("Failed to delete list: ${e.message}"))
            }
        }
    }

    private fun openList(listId: Long) {
        viewModelScope.launch { _effect.send(ReadingListsEffect.NavigateToListDetail(listId)) }
    }
}
