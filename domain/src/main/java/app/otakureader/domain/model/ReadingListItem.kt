package app.otakureader.domain.model

import androidx.compose.runtime.Immutable

/**
 * A manga within a reading list, with optional user note.
 */
@Immutable
data class ReadingListItem(
    val listId: Long,
    val mangaId: Long,
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val note: String? = null
)
