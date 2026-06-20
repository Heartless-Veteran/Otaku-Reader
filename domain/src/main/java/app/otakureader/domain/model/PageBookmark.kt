package app.otakureader.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain model for a page-level bookmark within a chapter.
 *
 * @property id database ID
 * @property mangaId parent manga ID
 * @property chapterId parent chapter ID
 * @property pageIndex page index within the chapter (0-based)
 * @property note optional user note
 * @property createdAt epoch millis when bookmarked
 */
@Immutable
data class PageBookmark(
    val id: Long = 0,
    val mangaId: Long,
    val chapterId: Long,
    val pageIndex: Int,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val collectionId: Long? = null
)
