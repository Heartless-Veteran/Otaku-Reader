package app.otakureader.core.preferences

import kotlinx.serialization.Serializable

/**
 * A named snapshot of the key global reader display settings.
 * Stored as part of a JSON list in DataStore via [ReaderPreferences].
 */
@Serializable
data class ReaderPreset(
    val id: String,
    val name: String,
    val readerMode: Int = 0,
    val backgroundColor: Int = 0,
    val readerScale: Int = 0,
    val autoZoomWideImages: Boolean = true,
    val animatePageTransitions: Boolean = true,
    val webtoonSidePadding: Int = 0,
    val invertTapZones: Boolean = false,
    val volumeKeysEnabled: Boolean = false,
    val volumeKeysInverted: Boolean = false,
    val showPageNumber: Boolean = true,
    val skipReadChapters: Boolean = false,
    val skipFilteredChapters: Boolean = true,
)
