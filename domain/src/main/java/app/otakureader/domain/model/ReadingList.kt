package app.otakureader.domain.model

import androidx.compose.runtime.Immutable

/**
 * A user-created reading list for organizing manga beyond categories.
 */
@Immutable
data class ReadingList(
    val id: Long,
    val name: String,
    val description: String? = null,
    val color: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    /** Number of manga in this list (denormalized for UI). */
    val itemCount: Int = 0
)
