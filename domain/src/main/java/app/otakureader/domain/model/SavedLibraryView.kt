package app.otakureader.domain.model

import kotlinx.serialization.Serializable

/**
 * A named snapshot of filter and sort state that users can save and recall from the library toolbar.
 *
 * [sortField] is the ordinal of [app.otakureader.feature.library.LibrarySortMode].
 * [filterMode] is the ordinal of [app.otakureader.feature.library.LibraryFilterMode].
 * [filterGenres] is the set of genre names that were active when the view was saved.
 */
@Serializable
data class SavedLibraryView(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val sortField: Int,          // ordinal of LibrarySortMode
    val sortAscending: Boolean,
    val filterMode: Int,         // ordinal of LibraryFilterMode
    val filterGenres: List<String> = emptyList(),
    val filterHasNotes: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
