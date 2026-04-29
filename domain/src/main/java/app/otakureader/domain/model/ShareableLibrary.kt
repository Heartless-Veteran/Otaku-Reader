package app.otakureader.domain.model

import androidx.compose.runtime.Immutable

/**
 * Minimal representation of a manga entry for QR-code library sharing.
 * Encoded as JSON and compressed into a QR code for local transfer between devices.
 */
@Immutable
data class ShareableManga(
    val title: String,
    val sourceId: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val author: String? = null,
    val status: MangaStatus = MangaStatus.UNKNOWN,
    val categories: List<String> = emptyList()
)

/**
 * Wrapper for the full library payload shared via QR.
 */
@Immutable
data class ShareableLibrary(
    val version: Int = 1,
    val app: String = "otaku-reader",
    val manga: List<ShareableManga>
)
