package app.otakureader.feature.more.bookmarks

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.BookmarkCollection

// ─── Domain display model ─────────────────────────────────────────────────────

/** A single page bookmark enriched with display data for the Bookmarks screen. */
data class BookmarkItem(
    val id: Long,
    val mangaId: Long,
    val chapterId: Long,
    val pageIndex: Int,
    val note: String?,
    val mangaTitle: String,
    val chapterName: String,
    /** Cover URL from the parent manga — used for the thumbnail in the list row. */
    val mangaCoverUrl: String?,
    val collectionId: Long? = null,
)

/** Per-manga group shown in the list: header + expandable chapter sub-groups. */
data class BookmarkGroup(
    val mangaTitle: String,
    val mangaId: Long,
    val mangaCoverUrl: String?,
    val isExpanded: Boolean = true,
    val chapters: List<ChapterGroup>,
)

/** Sub-group of bookmarks belonging to a single chapter within a manga group. */
data class ChapterGroup(
    val chapterId: Long,
    val chapterName: String,
    val bookmarks: List<BookmarkItem>,
)

// ─── State ────────────────────────────────────────────────────────────────────

data class BookmarksState(
    val isLoading: Boolean = true,
    val bookmarks: List<BookmarkItem> = emptyList(),
    val searchQuery: String = "",
    /** Set of mangaIds whose groups are currently collapsed. Default: all expanded. */
    val collapsedManga: Set<Long> = emptySet(),
    val error: String? = null,
    // Collections
    val collections: List<BookmarkCollection> = emptyList(),
    val selectedCollectionId: Long? = null,
    val isManageCollectionsVisible: Boolean = false,
    // Multi-select (Part D)
    val selectedBookmarkIds: Set<Long> = emptySet(),
) : UiState {

    val isSelectionMode: Boolean get() = selectedBookmarkIds.isNotEmpty()

    /** Source list: either filtered by collection or all bookmarks. */
    private val collectionFiltered: List<BookmarkItem> = if (selectedCollectionId == null) bookmarks
    else bookmarks.filter { it.collectionId == selectedCollectionId }

    /** Flat list filtered by [searchQuery]. Computed once at construction time. */
    val filteredBookmarks: List<BookmarkItem> = if (searchQuery.isBlank()) collectionFiltered
    else collectionFiltered.filter { bm ->
        bm.mangaTitle.contains(searchQuery, ignoreCase = true) ||
            bm.chapterName.contains(searchQuery, ignoreCase = true) ||
            bm.note?.contains(searchQuery, ignoreCase = true) == true
    }

    /** Grouped structure consumed by the list UI. Computed once at construction time. */
    val grouped: List<BookmarkGroup> = filteredBookmarks
        .groupBy { it.mangaId }
        .map { (mangaId, items) ->
            val first = items.first()
            val chapters = items
                .groupBy { it.chapterId }
                .map { (chapterId, chItems) ->
                    ChapterGroup(
                        chapterId = chapterId,
                        chapterName = chItems.first().chapterName,
                        bookmarks = chItems.sortedBy { it.pageIndex },
                    )
                }
                .sortedBy { it.chapterName }
            BookmarkGroup(
                mangaTitle = first.mangaTitle,
                mangaId = mangaId,
                mangaCoverUrl = first.mangaCoverUrl,
                isExpanded = mangaId !in collapsedManga,
                chapters = chapters,
            )
        }
        .sortedBy { it.mangaTitle }

    val isEmpty: Boolean = !isLoading && filteredBookmarks.isEmpty()
    val hasBookmarks: Boolean = !isLoading && bookmarks.isNotEmpty()
}

// ─── Intent ───────────────────────────────────────────────────────────────────

sealed interface BookmarksIntent : UiEvent {
    data class SearchQueryChanged(val query: String) : BookmarksIntent
    data class ToggleMangaExpanded(val mangaId: Long) : BookmarksIntent
    data class DeleteBookmark(val item: BookmarkItem) : BookmarksIntent
    data class OpenBookmark(val mangaId: Long, val chapterId: Long) : BookmarksIntent
    // Collections
    data class SelectCollection(val collectionId: Long?) : BookmarksIntent
    data class CreateCollection(val name: String) : BookmarksIntent
    data class RenameCollection(val id: Long, val name: String) : BookmarksIntent
    data class DeleteCollection(val id: Long) : BookmarksIntent
    data object ShowManageCollections : BookmarksIntent
    data object HideManageCollections : BookmarksIntent
    // Multi-select (Part D)
    data class ToggleBookmarkSelection(val id: Long) : BookmarksIntent
    data object SelectAllBookmarks : BookmarksIntent
    data object ClearSelection : BookmarksIntent
    data object ExportSelected : BookmarksIntent
    data object ShareSelected : BookmarksIntent
}

// ─── Effect ───────────────────────────────────────────────────────────────────

sealed interface BookmarksEffect : UiEffect {
    data class NavigateToReader(val mangaId: Long, val chapterId: Long) : BookmarksEffect
    data class ShowSnackbar(val message: String) : BookmarksEffect
    /** Signals the Screen to perform the actual MediaStore export for the given bookmark IDs. */
    data class RequestExport(val bookmarkIds: Set<Long>) : BookmarksEffect
    data class ExportComplete(val savedCount: Int) : BookmarksEffect
    /** Signals the Screen to launch the Android Sharesheet with the resolved bookmark items. */
    data class ShareSelected(val items: List<BookmarkItem>) : BookmarksEffect
}
