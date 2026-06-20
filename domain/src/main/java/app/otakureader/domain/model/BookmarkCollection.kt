package app.otakureader.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class BookmarkCollection(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
