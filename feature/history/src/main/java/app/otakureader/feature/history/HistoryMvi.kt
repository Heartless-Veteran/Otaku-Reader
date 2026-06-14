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
    /** Chapter ID buffered for swipe-delete undo; null when no deletion is pending. */
    val pendingDeleteChapterId: Long? = null,
) : UiState

sealed interface HistoryEvent : UiEvent {
    data class OnChapterClick(val mangaId: Long, val chapterId: Long) : HistoryEvent
    data class OnChapterLongClick(val chapterId: Long) : HistoryEvent
    data object ClearHistory : HistoryEvent
    data object ClearSelection : HistoryEvent
    data object SelectAll : HistoryEvent
    data class OnSearchQueryChange(val query: String) : HistoryEvent
    data class RemoveFromHistory(val chapterId: Long) : HistoryEvent
    /** Commit the pending swipe-delete — called when the undo snackbar is dismissed. */
    data object ConfirmRemoveFromHistory : HistoryEvent
    /** Cancel the pending swipe-delete — called when the user taps Undo. */
    data object UndoRemoveFromHistory : HistoryEvent
    data object RemoveSelectedFromHistory : HistoryEvent
    data object MarkSelectedAsRead : HistoryEvent
}

sealed interface HistoryEffect : UiEffect {
    data class NavigateToReader(val mangaId: Long, val chapterId: Long) : HistoryEffect
    data class ShowSnackbar(val messageRes: Int, val formatArgs: List<Any> = emptyList()) : HistoryEffect
    /** Snackbar with an Undo action for swipe-delete. Awaited inline by the screen. */
    data class ShowUndoSnackbar(val messageRes: Int) : HistoryEffect
}
