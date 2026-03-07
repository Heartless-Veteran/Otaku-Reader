package app.otakureader.feature.updates

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.MangaUpdate

data class UpdatesState(
    val isLoading: Boolean = false,
    val updates: List<MangaUpdate> = emptyList(),
    val error: String? = null
) : UiState

sealed interface UpdatesEvent : UiEvent {
    data object Refresh : UpdatesEvent
    data class OnChapterClick(val mangaId: Long, val chapterId: Long) : UpdatesEvent
}

sealed interface UpdatesEffect : UiEffect {
    data class NavigateToReader(val mangaId: Long, val chapterId: Long) : UpdatesEffect
}
