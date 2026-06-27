package app.otakureader.feature.browse

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.sourceapi.SourceManga

data class SourceSearchResult(
    val sourceId: String,
    val sourceName: String,
    val sourceLanguage: String = "",
    val results: List<SourceManga> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class GlobalSearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val sourceResults: List<SourceSearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val onlyShowHasResults: Boolean = false
) : UiState {
    val searchProgress: Int get() = sourceResults.count { !it.isLoading }
    val searchTotal: Int get() = sourceResults.size

    val filteredSourceResults: List<SourceSearchResult>
        get() = if (onlyShowHasResults) {
            sourceResults.filter { !it.isLoading && it.error == null && it.results.isNotEmpty() }
        } else {
            sourceResults
        }
}

sealed interface GlobalSearchEvent : UiEvent {
    data class OnQueryChange(val query: String) : GlobalSearchEvent
    data object Search : GlobalSearchEvent
    data class OnMangaClick(val sourceId: String, val manga: SourceManga) : GlobalSearchEvent
    data class OnHistoryItemClick(val query: String) : GlobalSearchEvent
    data object OnClearHistory : GlobalSearchEvent
    data class OnRemoveHistoryItem(val query: String) : GlobalSearchEvent
    data object OnToggleOnlyResults : GlobalSearchEvent
}

sealed interface GlobalSearchEffect : UiEffect {
    data class NavigateToMangaDetail(val sourceId: String, val mangaUrl: String) : GlobalSearchEffect
    data class ShowSnackbar(val message: String) : GlobalSearchEffect
}
