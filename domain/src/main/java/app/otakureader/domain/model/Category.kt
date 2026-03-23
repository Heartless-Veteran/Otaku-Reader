package app.otakureader.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents a user-defined category for organizing manga in the library.
 *
 * **UI Performance Note:** Marked with [@Immutable] for Compose performance.
 */
@Immutable
data class Category(
    val id: Long,
    val name: String,
    val order: Int = 0,
    val mangaCount: Int = 0,
    val isHidden: Boolean = false,
    val isNsfw: Boolean = false
)
