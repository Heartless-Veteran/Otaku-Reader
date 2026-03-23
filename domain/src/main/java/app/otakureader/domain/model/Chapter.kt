package app.otakureader.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Domain model representing a manga chapter.
 *
 * **UI Performance Note:** Marked with [@Immutable] to prevent unnecessary
 * recompositions in Jetpack Compose. All properties are immutable (val).
 */
@Immutable
@Serializable
data class Chapter(
    val id: Long,
    val mangaId: Long,
    val url: String,
    val name: String,
    val scanlator: String? = null,
    val read: Boolean = false,
    val bookmark: Boolean = false,
    val lastPageRead: Int = 0,
    val chapterNumber: Float = -1f,
    val dateUpload: Long = 0,
    val dateFetch: Long = 0
)
