package app.otakureader.feature.library

import app.otakureader.domain.model.ContinueReadingItem
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.model.ReadingGoal
import app.otakureader.domain.model.SavedLibraryView

enum class LibrarySortMode {
    ALPHABETICAL,
    LAST_READ,
    DATE_ADDED,
    UNREAD_COUNT,
    SOURCE
}

enum class LibraryFilterMode {
    ALL,
    DOWNLOADED,
    UNREAD,
    COMPLETED,
    DROPPED,
    TRACKING,
    READING_LIST
}

enum class LibraryBottomSheetTab {
    DISPLAY,
    SORT,
    FILTER,
    GROUP
}

/**
 * Top-level layout for the library grid.
 *
 * - [GRID]: cover-centric grid (the [LibraryState.isStaggeredGrid] flag further selects
 *   uniform vs. waterfall columns).
 * - [LIST]: compact single-column rows with a small cover thumbnail, title, and badges.
 *
 * Persisted as an ordinal via `LibraryPreferences.libraryDisplayMode`.
 */
enum class LibraryDisplayMode {
    GRID,
    LIST,
}

data class LibraryState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val mangaList: List<LibraryMangaItem> = emptyList(),
    val selectedManga: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val showSearchBar: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val categories: List<CategoryItem> = emptyList(),
    val selectedCategory: Long? = null,
    val gridSize: Int = 3,
    val showBadges: Boolean = true,
    val showDownloadBadge: Boolean = true,
    val downloadCountByManga: Map<Long, Int> = emptyMap(),
    val isStaggeredGrid: Boolean = false,
    val displayMode: LibraryDisplayMode = LibraryDisplayMode.GRID,
    val visualEffectsEnabled: Boolean = true,
    val filterHasNotes: Boolean = false,
    val sortMode: LibrarySortMode = LibrarySortMode.ALPHABETICAL,
    val filterMode: LibraryFilterMode = LibraryFilterMode.ALL,
    val filterSourceId: Long? = null,
    val showNsfw: Boolean = false,
    val newUpdatesCount: Int = 0,
    val incognitoMode: Boolean = false,
    val categoryFilterMangaIds: Set<Long> = emptySet(), // Manga IDs in selected category
    // Reading list filter
    val readingLists: List<ReadingListFilterItem> = emptyList(),
    val filterReadingListId: Long? = null,
    val readingListMangaIds: Set<Long> = emptySet(),
    // Continue Reading
    val continueReadingItems: List<ContinueReadingItem> = emptyList(),
    // Daily reading goal progress (shown in header when a goal is set)
    val readingGoal: ReadingGoal = ReadingGoal(),
    // Advanced filtering
    val filterGenres: Set<String> = emptySet(),
    val sortAscending: Boolean = true,
    val availableGenres: List<String> = emptyList(),
    val showBottomSheet: Boolean = false,
    val bottomSheetTab: LibraryBottomSheetTab = LibraryBottomSheetTab.DISPLAY,
    val groupByCategory: Boolean = false,
    // Recommendations carousel
    val recommendations: List<LibraryMangaItem> = emptyList(),
    val showRecommendations: Boolean = true,
    // Advanced search sheet
    val showAdvancedSearch: Boolean = false,
    // Saved views (#1039)
    val savedViews: List<SavedLibraryView> = emptyList(),
    val showSaveViewDialog: Boolean = false,
    val saveViewName: String = "",
    // L2/L3: long-press context menu — holds the id of the manga whose popup is open
    val contextMenuMangaId: Long? = null,
)

data class LibraryMangaItem(
    val id: Long,
    val title: String,
    val thumbnailUrl: String?,
    val unreadCount: Int,
    val isFavorite: Boolean,
    val hasNote: Boolean = false,
    val sourceId: Long = 0,
    val isDownloaded: Boolean = false,
    val hasTracking: Boolean = false,
    val isNsfw: Boolean = false,
    val lastRead: Long? = null,
    val dateAdded: Long = 0L,
    val status: MangaStatus = MangaStatus.UNKNOWN,
    val totalChapterCount: Int = 0,
    val userCompleted: Boolean = false,
    val userDropped: Boolean = false,
    val genres: List<String> = emptyList(),
)

data class CategoryItem(
    val id: Long,
    val name: String,
    val count: Int
)

data class ReadingListFilterItem(
    val id: Long,
    val name: String,
    val count: Int
)

sealed class LibraryEvent {
    data object Refresh : LibraryEvent()
    data class OnMangaClick(val mangaId: Long) : LibraryEvent()
    data class OnMangaLongClick(val mangaId: Long) : LibraryEvent()
    data class OnSearchQueryChange(val query: String) : LibraryEvent()
    data object ToggleSearchBar : LibraryEvent()
    data class OnCategorySelected(val categoryId: Long?) : LibraryEvent()
    data object ClearSelection : LibraryEvent()
    data object SelectAllManga : LibraryEvent()
    data object InvertSelection : LibraryEvent()
    data class ToggleFavorite(val mangaId: Long) : LibraryEvent()
    data class FilterHasNotes(val enabled: Boolean) : LibraryEvent()
    data class SetSortMode(val mode: LibrarySortMode) : LibraryEvent()
    data class SetFilterMode(val mode: LibraryFilterMode) : LibraryEvent()
    data class SetFilterSource(val sourceId: Long?) : LibraryEvent()
    data class ToggleNsfw(val show: Boolean) : LibraryEvent()
    // New overflow menu events
    data object UpdateLibrary : LibraryEvent()
    data object UpdateCategory : LibraryEvent()
    data object OpenRandomEntry : LibraryEvent()
    data object ReindexDownloads : LibraryEvent()
    /** Push all pending tracker states to remote trackers. No-op in incognito mode. */
    data object SyncLibrary : LibraryEvent()
    // Bulk selection actions
    data object MarkSelectedAsRead : LibraryEvent()
    data object MarkSelectedAsUnread : LibraryEvent()
    data object RemoveSelectedFromLibrary : LibraryEvent()
    data object DownloadSelected : LibraryEvent()
    data object MarkSelectedAsCompleted : LibraryEvent()
    data object MarkSelectedAsDropped : LibraryEvent()
    // Continue Reading
    data class ContinueReadingClick(val mangaId: Long, val chapterId: Long) : LibraryEvent()
    // Reading list filter (null = clear/show all)
    data class SetFilterReadingList(val listId: Long?) : LibraryEvent()
    data object ToggleIncognito : LibraryEvent()
    data class SetGenreFilter(val genres: Set<String>) : LibraryEvent()
    data class SetSortAscending(val ascending: Boolean) : LibraryEvent()
    data object ClearAllFilters : LibraryEvent()
    data object ToggleBottomSheet : LibraryEvent()
    data class SetBottomSheetTab(val tab: LibraryBottomSheetTab) : LibraryEvent()
    data class SetGroupByCategory(val enabled: Boolean) : LibraryEvent()
    data class SetGridSize(val size: Int) : LibraryEvent()
    data class SetShowBadges(val enabled: Boolean) : LibraryEvent()
    data class SetShowDownloadBadge(val enabled: Boolean) : LibraryEvent()
    data class SetStaggeredGrid(val enabled: Boolean) : LibraryEvent()
    data class SetDisplayMode(val mode: LibraryDisplayMode) : LibraryEvent()
    data object ToggleFilterSheet : LibraryEvent()
    data class DismissRecommendation(val mangaId: Long) : LibraryEvent()
    data object ToggleAdvancedSearch : LibraryEvent()
    data class ApplyAdvancedSearch(val authorQuery: String, val tagQuery: String) : LibraryEvent()
    // Single-manga selection actions (#995, #996)
    data object ShareSelectedManga : LibraryEvent()
    data object ViewSelectedManga : LibraryEvent()
    // Saved views (#1039)
    data object ShowSaveViewDialog : LibraryEvent()
    data object HideSaveViewDialog : LibraryEvent()
    data class UpdateSaveViewName(val name: String) : LibraryEvent()
    data object ConfirmSaveView : LibraryEvent()
    data class ApplySavedView(val view: SavedLibraryView) : LibraryEvent()
    data class DeleteSavedView(val id: String) : LibraryEvent()
    // EH favorites sync (#1024)
    data object SyncEhFavorites : LibraryEvent()
    // L2/L3: long-press context menu
    data class ShowContextMenu(val mangaId: Long) : LibraryEvent()
    data object DismissContextMenu : LibraryEvent()
    data class ResumeFromContextMenu(val mangaId: Long) : LibraryEvent()
    data class MarkMangaAsReadFromMenu(val mangaId: Long) : LibraryEvent()
    data class ShareMangaFromMenu(val mangaId: Long) : LibraryEvent()
    data class MigrateMangaFromMenu(val mangaId: Long) : LibraryEvent()
    data class SelectMangaFromMenu(val mangaId: Long) : LibraryEvent()
}

sealed class LibraryEffect {
    data class NavigateToManga(val mangaId: Long) : LibraryEffect()
    data class NavigateToReader(val mangaId: Long, val chapterId: Long) : LibraryEffect()
    data class ShowError(val message: String) : LibraryEffect()
    data class ShowSnackbar(val messageRes: Int, val formatArgs: List<Any> = emptyList()) : LibraryEffect()
    data class NavigateToMigration(val selectedMangaIds: List<Long>) : LibraryEffect()
    data class ShareManga(val title: String, val url: String) : LibraryEffect()
}
