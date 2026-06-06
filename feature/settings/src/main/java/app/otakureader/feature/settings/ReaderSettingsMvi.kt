@file:Suppress("MatchingDeclarationName")

package app.otakureader.feature.settings

import app.otakureader.domain.model.ImageQuality

data class ReaderSettingsState(
    // Display
    val readerMode: Int = 0,
    val keepScreenOn: Boolean = true,
    val fullscreen: Boolean = true,
    val showContentInCutout: Boolean = true,
    val showPageNumber: Boolean = true,
    val showPageThumbnailStrip: Boolean = true,
    val backgroundColor: Int = 0,
    val animatePageTransitions: Boolean = true,
    val showReadingModeOverlay: Boolean = true,
    val showTapZonesOverlay: Boolean = false,
    // Scale
    val readerScale: Int = 0,
    val autoZoomWideImages: Boolean = true,
    // Tap zones
    val tapZoneConfig: Int = 0,
    val invertTapZones: Boolean = false,
    // Volume keys
    val volumeKeysEnabled: Boolean = false,
    val volumeKeysInverted: Boolean = false,
    val volumeKeyBehaviorSinglePage: Int = 0,
    val volumeKeyBehaviorDualPage: Int = 0,
    val volumeKeyBehaviorWebtoon: Int = 0,
    val volumeKeyBehaviorSmartPanels: Int = 0,
    // Interaction
    val showActionsOnLongTap: Boolean = true,
    val savePagesToSeparateFolders: Boolean = false,
    // Webtoon
    val webtoonSidePadding: Int = 0,
    val webtoonMenuHideSensitivity: Int = 0,
    val webtoonDoubleTapZoom: Boolean = true,
    val webtoonDisableZoomOut: Boolean = false,
    // E-ink
    val einkFlashOnPageChange: Boolean = false,
    val einkBlackAndWhite: Boolean = false,
    // Behavior
    val skipReadChapters: Boolean = false,
    val skipFilteredChapters: Boolean = true,
    val skipDuplicateChapters: Boolean = false,
    val alwaysShowChapterTransition: Boolean = true,
    // Preload / quality
    val preloadPagesBefore: Int = 2,
    val preloadPagesAfter: Int = 3,
    val cropBordersEnabled: Boolean = false,
    val imageQuality: String = ImageQuality.ORIGINAL.name,
    val dataSaverEnabled: Boolean = false,
    val incognitoMode: Boolean = false,
    val secureScreen: Boolean = false,
)
