package app.otakureader.domain.model

import androidx.compose.runtime.Immutable

/**
 * A manga entry within a reading list, preserving junction metadata
 * (user note, sort order, added-at timestamp) alongside the manga itself.
 */
@Immutable
data class ReadingListMangaItem(
    val manga: Manga,
    val note: String? = null,
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
