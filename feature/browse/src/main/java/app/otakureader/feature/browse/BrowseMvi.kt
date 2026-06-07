package app.otakureader.feature.browse

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.FeedSavedSearch
import app.otakureader.sourceapi.FilterList
import app.otakureader.sourceapi.SourceManga

enum class BrowseSearchScope { SOURCES, LIBRARY }

data class BrowseState(
    val isLoading: Boolean = false,
    val sources: List<String> = emptyList(),
    val currentSourceId: String? = null,
    val popularManga: List<SourceManga> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SourceManga> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearchResults: Boolean = false,
    val error: String? = null,
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val availableFilters: FilterList = FilterList(),
    val activeFilters: FilterList = FilterList(),
    val showFilterSheet: Boolean = false,
    /** Currently selected manga URLs for bulk favorite. */
    val selectedManga: Set<String> = emptySet(),
    /** True when bulk selection mode is active. */
    val isBulkSelectionMode: Boolean = false,
    /** Saved searches for the current source, loaded from the database. */
    val savedSearches: List<FeedSavedSearch> = emptyList(),
    /** URLs of manga that are currently in the library (favorited). Used for long-click toggle. */
    val favoritedMangaUrls: Set<String> = emptySet(),
    /** Scope for the search bar: SOURCES searches the selected source, LIBRARY searches the local library. */
    val searchScope: BrowseSearchScope = BrowseSearchScope.SOURCES,
    /** Recent search history (last 10 queries). */
    val searchHistory: List<String> = emptyList(),
    /** Source IDs pinned to the top of the source list in Browse. */
    val pinnedSourceIds: Set<Long> = emptySet(),
    /** User-defined category label per source ID. */
    val sourceCategoryMap: Map<Long, String> = emptyMap(),
    /** Controls visibility of the "Set category" dialog. */
    val showSetCategoryDialog: Boolean = false,
    /** Source ID being configured via the category dialog (stored as Long for preferences). */
    val categoryDialogSourceId: Long? = null,
    /** Current text in the "Set category" dialog input field. */
    val categoryDialogText: String = "",
) : UiState

sealed interface BrowseEvent : UiEvent {
    data class SelectSource(val sourceId: String) : BrowseEvent
    data class OnSearchQueryChange(val query: String) : BrowseEvent
    data object Search : BrowseEvent
    data class OnMangaClick(val manga: SourceManga) : BrowseEvent
    data object LoadNextPage : BrowseEvent
    data object RefreshSources : BrowseEvent
    data object LoadLatest : BrowseEvent
    data object ToggleFilterSheet : BrowseEvent
    data class UpdateFilter(val index: Int, val filter: app.otakureader.sourceapi.Filter<*>) : BrowseEvent
    data object ResetFilters : BrowseEvent
    data object ApplyFilters : BrowseEvent

    // Bulk favorite events
    data class OnMangaLongClick(val manga: SourceManga) : BrowseEvent
    data class ToggleMangaSelection(val manga: SourceManga) : BrowseEvent
    data object ClearSelection : BrowseEvent
    data object AddSelectedToLibrary : BrowseEvent
    data object ExitBulkSelectionMode : BrowseEvent

    /**
     * Long-click on a manga card in Browse: quickly add to or remove from the library
     * without entering bulk selection mode.
     */
    data class LongClickManga(val manga: SourceManga) : BrowseEvent

    /** Saves the current search query + filters for the active source. */
    data object SaveCurrentSearch : BrowseEvent
    /** Deletes the saved search with the given id. */
    data class DeleteSavedSearch(val searchId: Long) : BrowseEvent
    /** Applies a previously saved search (restores query, filters, runs search). */
    data class ApplySavedSearch(val search: app.otakureader.domain.model.FeedSavedSearch) : BrowseEvent
    data class SetSearchScope(val scope: BrowseSearchScope) : BrowseEvent
    data object ClearSearchHistory : BrowseEvent
    /** Removes a single recent-search entry. */
    data class DeleteSearchHistoryItem(val query: String) : BrowseEvent

    // --- Source pinning & categories ---
    /** Toggles the pinned state for the source identified by its numeric ID string. */
    data class TogglePinSource(val sourceId: Long) : BrowseEvent
    /** Opens the "Set category" dialog pre-populated with the current label. */
    data class OpenSetCategoryDialog(val sourceId: Long, val currentCategory: String) : BrowseEvent
    /** User typed in the category dialog text field. */
    data class UpdateCategoryDialogText(val text: String) : BrowseEvent
    /** User confirmed the category dialog — persists the category. */
    data object ConfirmSetCategory : BrowseEvent
    /** User dismissed the category dialog without saving. */
    data object DismissSetCategoryDialog : BrowseEvent
}

sealed interface BrowseEffect : UiEffect {
    data class NavigateToMangaDetail(val sourceId: String, val mangaUrl: String) : BrowseEffect
    data class ShowSnackbar(val message: String) : BrowseEffect
    /** Navigate to library after bulk add to show newly added manga. */
    data object NavigateToLibrary : BrowseEffect
}
