package app.otakureader.feature.library.readinglist

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.ReadingList
import app.otakureader.domain.model.ReadingListMangaItem

data class ReadingListsState(
    val lists: List<ReadingList> = emptyList(),
    val isLoading: Boolean = true,
) : UiState

sealed interface ReadingListsEvent : UiEvent {
    data class CreateList(val name: String, val description: String?) : ReadingListsEvent
    data class RenameList(val listId: Long, val name: String, val description: String?) : ReadingListsEvent
    data class DeleteList(val listId: Long) : ReadingListsEvent
    data class OpenList(val listId: Long) : ReadingListsEvent
}

sealed interface ReadingListsEffect : UiEffect {
    data class ShowSnackbar(val message: String) : ReadingListsEffect
    data class NavigateToListDetail(val listId: Long) : ReadingListsEffect
}

/** Detail-screen state: the list's metadata plus its manga grid. */
data class ReadingListDetailState(
    val list: ReadingList? = null,
    val manga: List<ReadingListMangaItem> = emptyList(),
    val isLoading: Boolean = true,
) : UiState

sealed interface ReadingListDetailEvent : UiEvent {
    data class RemoveManga(val mangaId: Long) : ReadingListDetailEvent
    data class OpenManga(val mangaId: Long) : ReadingListDetailEvent
}

sealed interface ReadingListDetailEffect : UiEffect {
    data class ShowSnackbar(val message: String) : ReadingListDetailEffect
    data class NavigateToManga(val mangaId: Long) : ReadingListDetailEffect
}
