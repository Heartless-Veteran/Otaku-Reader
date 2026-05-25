package app.otakureader.feature.feed

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.FeedItem
import app.otakureader.domain.model.FeedSource

data class FeedState(
    val isLoading: Boolean = false,
    val feedItems: List<FeedItem> = emptyList(),
    val feedSources: List<FeedSource> = emptyList(),
    val error: String? = null,
    /** IDs of manga currently in the library. Used to indicate favorite status on long-click. */
    val favoritedMangaIds: Set<Long> = emptySet(),
) : UiState

sealed interface FeedEvent : UiEvent {
    data object Refresh : FeedEvent
    data class OnFeedItemClick(val mangaId: Long, val chapterId: Long) : FeedEvent
    data class OnMarkAsRead(val feedItemId: Long) : FeedEvent
    data class OnToggleSource(val sourceId: Long, val enabled: Boolean) : FeedEvent
    data object ClearHistory : FeedEvent
    /** Long-click on a feed item: quickly add to or remove from the library. */
    data class LongClickManga(val mangaId: Long) : FeedEvent
    data object ManageSources : FeedEvent
}

sealed interface FeedEffect : UiEffect {
    data class NavigateToReader(val mangaId: Long, val chapterId: Long) : FeedEffect
    data class ShowSnackbar(val message: String) : FeedEffect
    data object NavigateToFeedManagement : FeedEffect
}
