package app.otakureader.feature.library

/** All filter/sort parameters derived from [LibraryState] for use in the filtered-items combine. */
internal data class FilterSortParams(
    val query: String,
    val searchMatchingIds: Set<Long>?,
    val filterHasNotes: Boolean,
    val sortMode: LibrarySortMode,
    val filterMode: LibraryFilterMode,
    val filterSourceId: Long?,
    val showNsfw: Boolean,
    val selectedCategory: Long?,
    val categoryMangaIds: Set<Long> = emptySet(),
    val filterReadingListId: Long? = null,
    val readingListMangaIds: Set<Long> = emptySet(),
    val filterGenres: Set<String> = emptySet(),
    val sortAscending: Boolean = true,
    // Independent tristate filters (Komikku parity)
    val filterDownloaded: LibraryTriState = LibraryTriState.DISABLED,
    val filterUnread: LibraryTriState = LibraryTriState.DISABLED,
    val filterStarted: LibraryTriState = LibraryTriState.DISABLED,
    val filterTracking: LibraryTriState = LibraryTriState.DISABLED,
    val filterCompleted: LibraryTriState = LibraryTriState.DISABLED,
)
