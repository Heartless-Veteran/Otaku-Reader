package app.otakureader.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.usecase.GetHistoryUseCase
import app.otakureader.domain.usecase.ToggleFavoriteMangaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private val chapterRepository: ChapterRepository,
    private val toggleFavoriteManga: ToggleFavoriteMangaUseCase,
) : ViewModel() {

    companion object {
        const val UNDO_TIMEOUT_MS = 4_000L
    }

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val _effect = Channel<HistoryEffect>(Channel.BUFFERED)
    val effect: Flow<HistoryEffect> = _effect.receiveAsFlow()

    private val searchQuery = MutableStateFlow("")

    /** Chapters currently staged for swipe-delete; filtered from the displayed list. */
    private val pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())

    /** The one chapter whose undo timer is currently running. */
    private var pendingDeleteJob: Job? = null
    private var pendingDeleteJobChapterId: Long? = null
    /** Batch delete job — separate from the single-swipe timer. */
    private var pendingBatchDeleteJob: Job? = null
    private var pendingBatchDeleteIds: Set<Long>? = null

    /** Active date filter: (start, end) epoch-ms. Null means unset (open-ended). */
    private val dateFilter = MutableStateFlow(Pair<Long?, Long?>(null, null))

    init {
        _state.update { it.copy(isLoading = true) }
        combine(getHistoryUseCase(), searchQuery, pendingDeleteIds, dateFilter) { allEntries, query, pendingIds, (start, end) ->
            var filtered = if (query.isBlank()) allEntries
            else allEntries.filter { it.chapter.name.contains(query, ignoreCase = true) }
            if (start != null) filtered = filtered.filter { it.readAt >= start }
            if (end != null) filtered = filtered.filter { it.readAt <= end }
            if (pendingIds.isEmpty()) filtered else filtered.filter { it.chapter.id !in pendingIds }
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
            is HistoryEvent.InvertSelection -> invertSelection()
            is HistoryEvent.OnSearchQueryChange -> {
                searchQuery.value = event.query
                _state.update { it.copy(searchQuery = event.query) }
            }
            is HistoryEvent.RemoveFromHistory -> scheduleRemoveFromHistory(event.chapterId)
            is HistoryEvent.UndoRemoveFromHistory -> undoRemoveFromHistory(event.chapterId)
            is HistoryEvent.RemoveSelectedFromHistory -> removeSelectedFromHistory()
            is HistoryEvent.UndoBatchRemoveFromHistory -> undoBatchRemoveFromHistory(event.chapterIds)
            is HistoryEvent.MarkSelectedAsRead -> markSelectedAsRead()
            is HistoryEvent.SetDateFilter -> {
                dateFilter.value = Pair(event.start, event.end)
                _state.update { it.copy(dateFilterStart = event.start, dateFilterEnd = event.end) }
            }
            HistoryEvent.ClearDateFilter -> {
                dateFilter.value = Pair(null, null)
                _state.update { it.copy(dateFilterStart = null, dateFilterEnd = null) }
            }
            HistoryEvent.RefreshHistory -> {
                _state.update { it.copy(isPullRefreshing = true) }
                viewModelScope.launch {
                    delay(1_000L)
                    _state.update { it.copy(isPullRefreshing = false) }
                }
            }
            is HistoryEvent.ToggleMangaFavorite -> toggleFavorite(event.mangaId)
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
     * Selects every visible history entry not currently selected and deselects the rest
     * (Mihon/Komikku parity). [HistoryState.history] is already the filtered/searched list, so
     * inverting against it never touches entries hidden by an active search or date filter.
     */
    private fun invertSelection() {
        _state.update { state ->
            val allIds = state.history.map { it.chapter.id }.toSet()
            state.copy(selectedItems = allIds - state.selectedItems)
        }
    }

    /**
     * Buffer the chapter for deletion. If a different chapter was already pending,
     * commit it immediately so only one undo timer is active at a time.
     * The auto-commit fires after [UNDO_TIMEOUT_MS] unless the user taps Undo first.
     */
    private fun scheduleRemoveFromHistory(chapterId: Long) {
        val previousId = pendingDeleteJobChapterId
        if (previousId != null && previousId != chapterId) {
            pendingDeleteJob?.cancel()
            viewModelScope.launch {
                try { chapterRepository.removeFromHistory(previousId) } catch (_: Exception) {}
                pendingDeleteIds.update { it - previousId }
            }
        }
        pendingDeleteIds.update { it + chapterId }
        pendingDeleteJobChapterId = chapterId
        pendingDeleteJob = viewModelScope.launch {
            _effect.send(HistoryEffect.ShowUndoSnackbar(R.string.history_removed_undo, chapterId))
            delay(UNDO_TIMEOUT_MS)
            doRemoveFromHistory(chapterId)
        }
    }

    private fun undoRemoveFromHistory(chapterId: Long) {
        if (pendingDeleteJobChapterId != chapterId) return
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        pendingDeleteJobChapterId = null
        pendingDeleteIds.update { it - chapterId }
    }

    private suspend fun doRemoveFromHistory(chapterId: Long) {
        try {
            chapterRepository.removeFromHistory(chapterId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            pendingDeleteIds.update { it - chapterId }
            _effect.send(HistoryEffect.ShowSnackbar(R.string.history_remove_failed))
            return
        }
        pendingDeleteIds.update { it - chapterId }
        if (pendingDeleteJobChapterId == chapterId) pendingDeleteJobChapterId = null
    }

    override fun onCleared() {
        super.onCleared()
        val chapterId = pendingDeleteJobChapterId
        pendingDeleteJobChapterId = null
        val batchIds = pendingBatchDeleteIds
        pendingBatchDeleteJob?.cancel()
        pendingBatchDeleteIds = null
        viewModelScope.launch {
            if (chapterId != null) {
                try { chapterRepository.removeFromHistory(chapterId) } catch (_: Exception) {}
                pendingDeleteIds.update { it - chapterId }
            }
            batchIds?.forEach { id ->
                try { chapterRepository.removeFromHistory(id) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) {}
            }
            if (batchIds != null) pendingDeleteIds.update { it - batchIds }
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
            } catch (_: Exception) {
                _effect.send(HistoryEffect.ShowSnackbar(R.string.history_mark_read_failed))
            }
        }
    }

    private fun removeSelectedFromHistory() {
        val selectedIds = _state.value.selectedItems
        if (selectedIds.isEmpty()) return
        clearSelection()

        // Commit any previous pending batch immediately before starting a new one
        val previousIds = pendingBatchDeleteIds
        if (previousIds != null) {
            pendingBatchDeleteJob?.cancel()
            viewModelScope.launch {
                previousIds.forEach { chapterId ->
                    try { chapterRepository.removeFromHistory(chapterId) }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { }
                }
                pendingDeleteIds.update { it - previousIds }
            }
        }

        pendingBatchDeleteIds = selectedIds
        pendingDeleteIds.update { it + selectedIds }
        pendingBatchDeleteJob = viewModelScope.launch {
            _effect.send(HistoryEffect.ShowUndoBatchSnackbar(
                messageRes = R.string.history_removed_count,
                count = selectedIds.size,
                chapterIds = selectedIds,
            ))
            delay(UNDO_TIMEOUT_MS)
            selectedIds.forEach { chapterId ->
                try { chapterRepository.removeFromHistory(chapterId) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { }
            }
            pendingDeleteIds.update { it - selectedIds }
            if (pendingBatchDeleteIds == selectedIds) {
                pendingBatchDeleteIds = null
                pendingBatchDeleteJob = null
            }
        }
    }

    private fun undoBatchRemoveFromHistory(chapterIds: Set<Long>) {
        // Only undo the active batch — ignore stale snackbar actions
        if (pendingBatchDeleteIds != chapterIds) return
        pendingBatchDeleteJob?.cancel()
        pendingBatchDeleteJob = null
        pendingBatchDeleteIds = null
        pendingDeleteIds.update { it - chapterIds }
    }

    private fun toggleFavorite(mangaId: Long) {
        viewModelScope.launch {
            try {
                toggleFavoriteManga(mangaId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
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
            } catch (_: Exception) {
                _effect.send(HistoryEffect.ShowSnackbar(R.string.history_clear_failed))
            }
        }
    }
}
