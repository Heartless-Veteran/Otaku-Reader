package app.otakureader.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import app.otakureader.feature.history.R

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val _effect = Channel<HistoryEffect>(Channel.BUFFERED)
    val effect: Flow<HistoryEffect> = _effect.receiveAsFlow()

    private val searchQuery = MutableStateFlow("")

    /** Chapter ID currently staged for swipe-delete; filtered from the displayed list. */
    private val pendingDeleteId = MutableStateFlow<Long?>(null)

    init {
        _state.update { it.copy(isLoading = true) }
        combine(getHistoryUseCase(), searchQuery, pendingDeleteId) { allEntries, query, pendingId ->
            val filtered = if (query.isBlank()) allEntries
            else allEntries.filter { it.chapter.name.contains(query, ignoreCase = true) }
            if (pendingId != null) filtered.filter { it.chapter.id != pendingId } else filtered
        }
            .onEach { filtered ->
                _state.update { it.copy(isLoading = false, history = filtered, error = null) }
            }
            .catch { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.OnChapterClick -> onChapterClick(event.mangaId, event.chapterId)
            is HistoryEvent.OnChapterLongClick -> toggleSelection(event.chapterId)
            is HistoryEvent.ClearHistory -> clearHistory()
            is HistoryEvent.ClearSelection -> clearSelection()
            is HistoryEvent.SelectAll -> selectAll()
            is HistoryEvent.OnSearchQueryChange -> {
                searchQuery.value = event.query
                _state.update { it.copy(searchQuery = event.query) }
            }
            is HistoryEvent.RemoveFromHistory -> scheduleRemoveFromHistory(event.chapterId)
            is HistoryEvent.UndoRemoveFromHistory -> undoRemoveFromHistory()
            is HistoryEvent.ConfirmRemoveFromHistory -> confirmRemoveFromHistory()
            is HistoryEvent.RemoveSelectedFromHistory -> removeSelectedFromHistory()
            is HistoryEvent.MarkSelectedAsRead -> markSelectedAsRead()
        }
    }

    private fun onChapterClick(mangaId: Long, chapterId: Long) {
        if (_state.value.selectedItems.isNotEmpty()) {
            toggleSelection(chapterId)
        } else {
            navigateToReader(mangaId, chapterId)
        }
    }

    private fun toggleSelection(chapterId: Long) {
        _state.update { state ->
            val currentSelection = state.selectedItems
            val newSelection = if (currentSelection.contains(chapterId)) {
                currentSelection - chapterId
            } else {
                currentSelection + chapterId
            }
            state.copy(selectedItems = newSelection)
        }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedItems = emptySet()) }
    }

    private fun selectAll() {
        _state.update { state ->
            val allIds = state.history.map { it.chapter.id }.toSet()
            state.copy(selectedItems = allIds)
        }
    }

    /**
     * Buffer the chapter for deletion. If another chapter was already pending,
     * it is committed first so only one undo is active at a time.
     */
    private fun scheduleRemoveFromHistory(chapterId: Long) {
        val previousPending = pendingDeleteId.value
        if (previousPending != null && previousPending != chapterId) {
            viewModelScope.launch {
                try { chapterRepository.removeFromHistory(previousPending) } catch (_: Exception) { }
            }
        }
        pendingDeleteId.value = chapterId
        _state.update { it.copy(pendingDeleteChapterId = chapterId) }
        viewModelScope.launch {
            _effect.send(HistoryEffect.ShowUndoSnackbar(R.string.history_removed_undo))
        }
    }

    private fun undoRemoveFromHistory() {
        pendingDeleteId.value = null
        _state.update { it.copy(pendingDeleteChapterId = null) }
    }

    private fun confirmRemoveFromHistory() {
        val chapterId = _state.value.pendingDeleteChapterId ?: return
        pendingDeleteId.value = null
        _state.update { it.copy(pendingDeleteChapterId = null) }
        viewModelScope.launch {
            try {
                chapterRepository.removeFromHistory(chapterId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(HistoryEffect.ShowSnackbar(R.string.history_remove_failed))
            }
        }
    }

    private fun markSelectedAsRead() {
        val selected = _state.value.selectedItems
        if (selected.isEmpty()) return
        val count = selected.size
        viewModelScope.launch {
            try {
                chapterRepository.updateChapterProgress(selected, read = true, lastPageRead = 0)
                _state.update { it.copy(selectedItems = it.selectedItems - selected) }
                _effect.send(HistoryEffect.ShowSnackbar(R.string.history_marked_read_count, listOf(count)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(HistoryEffect.ShowSnackbar(R.string.history_mark_read_failed))
            }
        }
    }

    private fun removeSelectedFromHistory() {
        viewModelScope.launch {
            try {
                val selectedIds = _state.value.selectedItems
                if (selectedIds.isNotEmpty()) {
                    selectedIds.forEach { chapterId ->
                        chapterRepository.removeFromHistory(chapterId)
                    }
                    clearSelection()
                    _effect.send(
                        HistoryEffect.ShowSnackbar(
                            R.string.history_removed_count,
                            formatArgs = listOf(selectedIds.size),
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(HistoryEffect.ShowSnackbar(R.string.history_remove_failed))
            }
        }
    }

    private fun navigateToReader(mangaId: Long, chapterId: Long) {
        viewModelScope.launch {
            _effect.send(HistoryEffect.NavigateToReader(mangaId, chapterId))
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            try {
                chapterRepository.clearAllHistory()
                _effect.send(HistoryEffect.ShowSnackbar(R.string.history_cleared))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(HistoryEffect.ShowSnackbar(R.string.history_clear_failed))
            }
        }
    }
}
