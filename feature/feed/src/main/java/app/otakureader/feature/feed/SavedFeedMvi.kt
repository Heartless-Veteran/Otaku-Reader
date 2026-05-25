package app.otakureader.feature.feed

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.FeedSource

data class SavedFeedState(
    val sources: List<FeedSource> = emptyList(),
    val isLoading: Boolean = false,
) : UiState

sealed interface SavedFeedEvent : UiEvent {
    data class AddSource(val sourceName: String) : SavedFeedEvent
    data class RemoveSource(val sourceId: Long) : SavedFeedEvent
    data class ToggleSource(val sourceId: Long, val enabled: Boolean) : SavedFeedEvent
}

sealed interface SavedFeedEffect : UiEffect {
    data class ShowSnackbar(val message: String) : SavedFeedEffect
}
