package app.otakureader.feature.library

import app.otakureader.domain.model.ContentRating
import app.otakureader.domain.model.Manga

internal fun applyFiltersAndSort(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
    val filtered = items
        .let { applyCategoryFilter(it, params) }
        .let { applyNsfwFilter(it, params) }
        .let { applySearchFilter(it, params) }
        .let { applyHasNotesFilter(it, params) }
        .let { applySourceFilter(it, params) }
        .let { applyReadingListFilter(it, params) }
        .let { applyGenreFilter(it, params.filterGenres) }
        // Legacy single-select filter: drives the quick-access chips in the search bar.
        .let { applyFilterMode(it, params.filterMode) }
        // Independent tristate filters: drives the per-attribute toggles in the filter sheet.
        .let { applyTriStateFilters(it, params) }
    return applySort(filtered, params.sortMode, params.sortAscending)
}

internal fun applyCategoryFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> =
    if (params.selectedCategory != null) items.filter { it.id in params.categoryMangaIds } else items

internal fun applyNsfwFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> =
    if (!params.showNsfw) items.filter { !it.isNsfw } else items

internal fun applySearchFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
    val matchingIds = params.searchMatchingIds ?: return items
    return items.filter { it.id in matchingIds }
}

internal fun applyHasNotesFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> =
    if (params.filterHasNotes) items.filter { it.hasNote } else items

internal fun applySourceFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> =
    if (params.filterSourceId != null) items.filter { it.sourceId == params.filterSourceId } else items

internal fun applyReadingListFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> =
    if (params.filterReadingListId != null) items.filter { it.id in params.readingListMangaIds } else items

internal fun applyGenreFilter(items: List<LibraryMangaItem>, filterGenres: Set<String>): List<LibraryMangaItem> =
    if (filterGenres.isEmpty()) items else items.filter { manga -> manga.genres.any { it in filterGenres } }

internal fun applyFilterMode(items: List<LibraryMangaItem>, filterMode: LibraryFilterMode): List<LibraryMangaItem> =
    when (filterMode) {
        LibraryFilterMode.DOWNLOADED -> items.filter { it.isDownloaded }
        LibraryFilterMode.UNREAD -> items.filter { it.unreadCount > 0 }
        LibraryFilterMode.COMPLETED -> items.filter { it.userCompleted }
        LibraryFilterMode.DROPPED -> items.filter { it.userDropped }
        LibraryFilterMode.TRACKING -> items.filter { it.hasTracking }
        LibraryFilterMode.READING_LIST, LibraryFilterMode.ALL -> items
    }

/** Applies independent tristate filters (Komikku parity). Each filter is additive. */
internal fun applyTriStateFilters(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
    var result = items
    result = applyTriState(result, params.filterDownloaded) { it.isDownloaded }
    result = applyTriState(result, params.filterUnread) { it.unreadCount > 0 }
    result = applyTriState(result, params.filterStarted) { it.lastRead != null }
    result = applyTriState(result, params.filterBookmarked) { it.hasTracking }
    result = applyTriState(result, params.filterCompleted) { it.userCompleted }
    return result
}

private fun applyTriState(
    items: List<LibraryMangaItem>,
    state: LibraryTriState,
    predicate: (LibraryMangaItem) -> Boolean,
): List<LibraryMangaItem> = when (state) {
    LibraryTriState.DISABLED -> items
    LibraryTriState.ENABLED_IS -> items.filter(predicate)
    LibraryTriState.ENABLED_NOT -> items.filterNot(predicate)
}

internal fun applySort(items: List<LibraryMangaItem>, sortMode: LibrarySortMode, ascending: Boolean): List<LibraryMangaItem> {
    val sorted = when (sortMode) {
        LibrarySortMode.ALPHABETICAL -> items.sortedBy { it.title }
        LibrarySortMode.LAST_READ -> items.sortedByDescending { it.lastRead ?: 0L }
        LibrarySortMode.DATE_ADDED -> items.sortedByDescending { it.dateAdded }
        LibrarySortMode.UNREAD_COUNT -> items.sortedByDescending { it.unreadCount }
        LibrarySortMode.SOURCE -> items.sortedBy { it.sourceId }
        LibrarySortMode.LAST_UPDATED -> items.sortedByDescending { it.lastUpdate }
        LibrarySortMode.TOTAL_CHAPTERS -> items.sortedByDescending { it.totalChapterCount }
        LibrarySortMode.LATEST_CHAPTER -> items.sortedByDescending { it.lastUpdate }
        LibrarySortMode.RANDOM -> items.shuffled()
    }
    return if (ascending) sorted else sorted.reversed()
}

internal fun Manga.toLibraryItem(
    isDownloaded: Boolean = false,
    hasTracking: Boolean = false,
) = LibraryMangaItem(
    id = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    unreadCount = unreadCount,
    isFavorite = favorite,
    hasNote = !notes.isNullOrBlank(),
    sourceId = sourceId,
    isDownloaded = isDownloaded,
    hasTracking = hasTracking,
    isNsfw = contentRating == ContentRating.EROTICA || contentRating == ContentRating.PORNOGRAPHIC,
    lastRead = lastRead,
    dateAdded = dateAdded,
    status = status,
    totalChapterCount = totalChapters,
    lastUpdate = lastUpdate,
    userCompleted = userCompleted,
    userDropped = userDropped,
    genres = genre,
)
