package app.otakureader.feature.history

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.ChapterWithHistory

data class HistoryState(
    val isLoading: Boolean = false,
    val history: List<ChapterWithHistory> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val selectedItems: Set<Long> = emptySet(),
    /** Start of the active date filter (epoch-ms, inclusive), or null if no filter set. */
    val dateFilterStart: Long? = null,
    /** End of the active date filter (epoch-ms, inclusive), or null if no filter set. */
    val dateFilterEnd: Long? = null,
) : UiState

sealed interface HistoryEvent : UiEvent {
    data class OnChapterClick(val mangaId: Long, val chapterId: Long) : HistoryEvent
    data class OnChapterLongClick(val chapterId: Long) : HistoryEvent
    data object ClearHistory : HistoryEvent
    data object ClearSelection : HistoryEvent
    data object SelectAll : HistoryEvent
    data class OnSearchQueryChange(val query: String) : HistoryEvent
    data class RemoveFromHistory(val chapterId: Long) : HistoryEvent
    /** Cancel the pending swipe-delete; carries chapterId so the ViewModel knows which timer to cancel. */
    data class UndoRemoveFromHistory(val chapterId: Long) : HistoryEvent
    data object RemoveSelectedFromHistory : HistoryEvent
    data object MarkSelectedAsRead : HistoryEvent
    /** Apply an explicit date range filter. Pass null for either bound to leave it open-ended. */
    data class SetDateFilter(val start: Long?, val end: Long?) : HistoryEvent
    /** Remove the active date range filter and show all history entries. */
    data object ClearDateFilter : HistoryEvent
}

sealed interface HistoryEffect : UiEffect {
    data class NavigateToReader(val mangaId: Long, val chapterId: Long) : HistoryEffect
    data class ShowSnackbar(val messageRes: Int, val formatArgs: List<Any> = emptyList()) : HistoryEffect
    /**
     * Snackbar with an Undo action for swipe-delete. Carries the chapterId so the screen can
     * pass it back in [HistoryEvent.UndoRemoveFromHistory] without keeping extra UI state.
     * The ViewModel auto-commits after [HistoryViewModel.UNDO_TIMEOUT_MS] regardless of UI state.
     */
    data class ShowUndoSnackbar(val messageRes: Int, val chapterId: Long) : HistoryEffect
}
