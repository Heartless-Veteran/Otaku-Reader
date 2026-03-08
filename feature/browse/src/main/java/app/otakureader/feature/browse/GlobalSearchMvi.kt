package app.otakureader.feature.browse

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.sourceapi.SourceManga

/**
 * Holds the search result (or loading/error state) for a single source.
 */
data class SourceSearchResult(
    val sourceId: String,
    val sourceName: String,
    val results: List<SourceManga> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class GlobalSearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val sourceResults: List<SourceSearchResult> = emptyList()
) : UiState

sealed interface GlobalSearchEvent : UiEvent {
    data class OnQueryChange(val query: String) : GlobalSearchEvent
    data object Search : GlobalSearchEvent
    data class OnMangaClick(val sourceId: String, val manga: SourceManga) : GlobalSearchEvent
}

sealed interface GlobalSearchEffect : UiEffect {
    data class NavigateToMangaDetail(val sourceId: String, val mangaUrl: String) : GlobalSearchEffect
    data class ShowSnackbar(val message: String) : GlobalSearchEffect
}
