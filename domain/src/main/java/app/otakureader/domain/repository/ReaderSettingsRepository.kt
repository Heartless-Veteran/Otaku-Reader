package app.otakureader.domain.repository

import app.otakureader.domain.model.ColorFilterMode
import app.otakureader.domain.model.ImageQuality
import app.otakureader.domain.model.ReaderMode
import app.otakureader.domain.model.ReadingDirection
import app.otakureader.domain.model.TapZoneConfig
import kotlinx.coroutines.flow.Flow

interface ReaderSettingsRepository {

    val writeFailureEvents: Flow<Unit>

    val readerMode: Flow<ReaderMode>
    val readingDirection: Flow<ReadingDirection>
    val brightness: Flow<Float>
    val keepScreenOn: Flow<Boolean>
    val showPageNumber: Flow<Boolean>
    val volumeKeysEnabled: Flow<Boolean>
    val volumeKeysInverted: Flow<Boolean>
    val volumeKeyBehaviorSinglePage: Flow<Int>
    val volumeKeyBehaviorDualPage: Flow<Int>
    val volumeKeyBehaviorWebtoon: Flow<Int>
    val volumeKeyBehaviorSmartPanels: Flow<Int>
    val fullscreen: Flow<Boolean>
    val incognitoMode: Flow<Boolean>
    val colorFilterMode: Flow<ColorFilterMode>
    val customTintColor: Flow<Long>
    val cropBordersEnabled: Flow<Boolean>
    val imageQuality: Flow<ImageQuality>
    val dataSaverEnabled: Flow<Boolean>
    val showReadingTimer: Flow<Boolean>
    val showBatteryTime: Flow<Boolean>
    val preloadPagesBefore: Flow<Int>
    val preloadPagesAfter: Flow<Int>
    val smartPrefetchEnabled: Flow<Boolean>
    val prefetchStrategyOrdinal: Flow<Int>
    val adaptiveLearningEnabled: Flow<Boolean>
    val prefetchAdjacentChapters: Flow<Boolean>
    val prefetchOnlyOnWiFi: Flow<Boolean>
    val showContentInCutout: Flow<Boolean>
    val backgroundColor: Flow<Int>
    val animatePageTransitions: Flow<Boolean>
    val showReadingModeOverlay: Flow<Boolean>
    val showTapZonesOverlay: Flow<Boolean>
    val readerScale: Flow<Int>
    val autoZoomWideImages: Flow<Boolean>
    val invertTapZones: Flow<Boolean>
    val tapZoneConfig: Flow<TapZoneConfig>
    val webtoonSidePadding: Flow<Int>
    val webtoonGapDp: Flow<Int>
    val webtoonMenuHideSensitivity: Flow<Int>
    val webtoonDoubleTapZoom: Flow<Boolean>
    val webtoonDisableZoomOut: Flow<Boolean>
    // ==================== Auto-Scroll Speed ====================
    val autoScrollSpeed: Flow<Float>
    
    val einkFlashOnPageChange: Flow<Boolean>
    val einkBlackAndWhite: Flow<Boolean>
    val skipReadChapters: Flow<Boolean>
    val skipFilteredChapters: Flow<Boolean>
    val skipDuplicateChapters: Flow<Boolean>
    val alwaysShowChapterTransition: Flow<Boolean>
    val showActionsOnLongTap: Flow<Boolean>
    val savePagesToSeparateFolders: Flow<Boolean>
    val showPageThumbnailStrip: Flow<Boolean>
    val secureScreen: Flow<Boolean>

    suspend fun setReaderMode(mode: ReaderMode)
    suspend fun setBrightness(brightness: Float)
    suspend fun setReadingDirection(direction: ReadingDirection)
    suspend fun setKeepScreenOn(enabled: Boolean)
    suspend fun setShowPageNumber(enabled: Boolean)
    suspend fun setVolumeKeysEnabled(enabled: Boolean)
    suspend fun setVolumeKeysInverted(inverted: Boolean)
    suspend fun setFullscreen(enabled: Boolean)
    suspend fun setAutoScrollSpeed(speed: Float)
    suspend fun setDoubleTapZoomEnabled(enabled: Boolean)
    suspend fun setTapZoneConfig(config: TapZoneConfig)
    suspend fun setIncognitoMode(enabled: Boolean)
    suspend fun setColorFilterMode(mode: ColorFilterMode)
    suspend fun setCustomTintColor(color: Long)
    suspend fun setCropBordersEnabled(enabled: Boolean)
    suspend fun setShowPageThumbnailStrip(enabled: Boolean)
    suspend fun setSecureScreen(enabled: Boolean)
    suspend fun setPreloadPagesBefore(count: Int)
    suspend fun setPreloadPagesAfter(count: Int)
    suspend fun setImageQuality(quality: ImageQuality)
    suspend fun setDataSaverEnabled(enabled: Boolean)
    suspend fun setSkipReadChapters(enabled: Boolean)
    suspend fun setSkipFilteredChapters(enabled: Boolean)
    suspend fun setSkipDuplicateChapters(enabled: Boolean)

    companion object {
        const val DEFAULT_PRELOAD_PAGES = 3
    }
}
