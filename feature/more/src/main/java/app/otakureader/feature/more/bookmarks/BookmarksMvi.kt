package app.otakureader.feature.more.bookmarks

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState

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
) : UiState {

    /** Flat list filtered by [searchQuery]. Computed once at construction time. */
    val filteredBookmarks: List<BookmarkItem> = if (searchQuery.isBlank()) bookmarks
    else bookmarks.filter { bm ->
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
}

// ─── Effect ───────────────────────────────────────────────────────────────────

sealed interface BookmarksEffect : UiEffect {
    data class NavigateToReader(val mangaId: Long, val chapterId: Long) : BookmarksEffect
    data class ShowSnackbar(val message: String) : BookmarksEffect
}
