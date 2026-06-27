package app.otakureader.feature.reader.viewmodel.delegate

import app.otakureader.domain.model.ColorFilterMode
import app.otakureader.domain.model.ImageQuality
import app.otakureader.domain.model.ReaderMode
import app.otakureader.domain.model.ReaderOrientation
import app.otakureader.domain.model.ReadingDirection
import app.otakureader.domain.model.TapInvertMode
import app.otakureader.feature.reader.PageRotation
import app.otakureader.feature.reader.ReaderSetting
import app.otakureader.feature.reader.ReaderState
import app.otakureader.domain.model.TapZoneConfig
import app.otakureader.domain.repository.ReaderSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Encapsulates display, overlay, brightness, auto-scroll, color-filter and settings
 * mutation logic that was previously inline in [ReaderViewModel].
 *
 * The delegate receives the same [MutableStateFlow] the ViewModel owns so that all
 * state changes remain in a single source of truth while the behavioural logic lives
 * in a testable, focused class.
 */
class ReaderDisplayDelegate @Inject constructor(
    private val settingsRepository: ReaderSettingsRepository,
) {

    /** The ViewModel wires its [_state] here after creation. */
    lateinit var stateFlow: MutableStateFlow<ReaderState>

    /** The ViewModel wires its [viewModelScope] here after creation. */
    lateinit var scope: CoroutineScope

    private inline fun update(crossinline block: (ReaderState) -> ReaderState) {
        stateFlow.update(block)
    }

    private fun launchSave(block: suspend ReaderSettingsRepository.() -> Unit) {
        scope.launch { settingsRepository.block() }
    }

    // ── Display control ──────────────────────────────────────────────────────

    fun changeReaderMode(mode: ReaderMode) {
        update { current ->
            val needsPageAdjust = mode == ReaderMode.DUAL_PAGE && current.currentPage % 2 != 0
            current.copy(
                mode = mode,
                currentPage = if (needsPageAdjust) current.currentPage - 1 else current.currentPage
            )
        }
        launchSave { setReaderMode(mode) }
    }

    fun updateReadingDirection(direction: ReadingDirection) {
        update { it.copy(readingDirection = direction) }
        launchSave { setReadingDirection(direction) }
    }

    fun changeOrientation(orientation: ReaderOrientation) {
        update { it.copy(readerOrientation = orientation) }
        launchSave { setReaderOrientation(orientation) }
    }

    fun cyclePageRotation() {
        update { it.copy(pageRotation = it.pageRotation.next()) }
    }

    fun resetRotation() {
        update { it.copy(pageRotation = PageRotation.NONE) }
    }

    // ── Zoom ─────────────────────────────────────────────────────────────────

    fun updateZoom(zoom: Float) {
        val clamped = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        update { it.copy(zoomLevel = clamped) }
    }

    // ── Overlay ──────────────────────────────────────────────────────────────

    fun toggleMenu() {
        update { it.copy(isMenuVisible = !it.isMenuVisible) }
    }

    fun toggleGallery() {
        update { it.copy(isGalleryOpen = !it.isGalleryOpen) }
    }

    fun setGalleryColumns(columns: Int) {
        val clamped = columns.coerceIn(2, 4)
        update { it.copy(galleryColumns = clamped) }
    }

    fun toggleFullscreen() {
        val newValue = !stateFlow.value.isFullscreen
        update { it.copy(isFullscreen = newValue) }
        launchSave { setFullscreen(newValue) }
    }

    fun toggleSettingsOverlay() {
        update { it.copy(isSettingsOverlayVisible = !it.isSettingsOverlayVisible) }
    }

    fun toggleChapterListOverlay() {
        update { it.copy(isChapterListOverlayVisible = !it.isChapterListOverlayVisible) }
    }

    fun toggleCommentsOverlay() {
        update { it.copy(isCommentsOverlayVisible = !it.isCommentsOverlayVisible) }
    }

    // ── Brightness ───────────────────────────────────────────────────────────

    fun updateBrightness(brightness: Float) {
        val clamped = brightness.coerceIn(0.1f, 1.5f)
        update { it.copy(brightness = clamped) }
        launchSave { setBrightness(clamped) }
    }

    // ── Auto-scroll ─────────────────────────────────────────────────────────

    fun toggleAutoScroll() {
        update { it.copy(isAutoScrollEnabled = !it.isAutoScrollEnabled) }
    }

    fun updateAutoScrollSpeed(speed: Float) {
        val clamped = speed.coerceIn(10f, 500f)
        update { it.copy(autoScrollSpeed = clamped) }
        launchSave { setAutoScrollSpeed(clamped) }
    }

    // ── Color filter ───────────────────────────────────────────────────────

    fun updateColorFilterMode(mode: ColorFilterMode) {
        update { it.copy(colorFilterMode = mode) }
        launchSave { setColorFilterMode(mode) }
    }

    fun updateCustomTintColor(color: Long) {
        update { it.copy(customTintColor = color) }
        launchSave { setCustomTintColor(color) }
    }

    fun updateReaderBackgroundColor(color: Long?) {
        update { it.copy(readerBackgroundColor = color) }
    }

    // ── Settings toggles ───────────────────────────────────────────────────

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun toggleSetting(setting: ReaderSetting) {
        when (setting) {
            ReaderSetting.KEEP_SCREEN_ON -> {
                val new = !stateFlow.value.keepScreenOn
                update { it.copy(keepScreenOn = new) }
                launchSave { setKeepScreenOn(new) }
            }
            ReaderSetting.SHOW_PAGE_NUMBER -> {
                val new = !stateFlow.value.showPageNumber
                update { it.copy(showPageNumber = new) }
                launchSave { setShowPageNumber(new) }
            }
            ReaderSetting.DOUBLE_TAP_ZOOM -> {
                val new = !stateFlow.value.doubleTapZoomEnabled
                update { it.copy(doubleTapZoomEnabled = new) }
                launchSave { setDoubleTapZoomEnabled(new) }
            }
            ReaderSetting.VOLUME_KEY_NAVIGATION -> {
                val new = !stateFlow.value.volumeKeysEnabled
                update { it.copy(volumeKeysEnabled = new) }
                launchSave { setVolumeKeysEnabled(new) }
            }
            ReaderSetting.VOLUME_KEYS_INVERTED -> {
                val new = !stateFlow.value.volumeKeysInverted
                update { it.copy(volumeKeysInverted = new) }
                launchSave { setVolumeKeysInverted(new) }
            }
            ReaderSetting.INCOGNITO_MODE -> {
                val new = !stateFlow.value.incognitoMode
                update { it.copy(incognitoMode = new) }
                launchSave { setIncognitoMode(new) }
            }
            ReaderSetting.CROP_BORDERS -> {
                val new = !stateFlow.value.cropBordersEnabled
                update { it.copy(cropBordersEnabled = new) }
                launchSave { setCropBordersEnabled(new) }
            }
            ReaderSetting.SKIP_READ_CHAPTERS -> {
                val new = !stateFlow.value.skipReadChapters
                update { it.copy(skipReadChapters = new) }
                launchSave { setSkipReadChapters(new) }
            }
            ReaderSetting.SKIP_FILTERED_CHAPTERS -> {
                val new = !stateFlow.value.skipFilteredChapters
                update { it.copy(skipFilteredChapters = new) }
                launchSave { setSkipFilteredChapters(new) }
            }
            ReaderSetting.SKIP_DUPLICATE_CHAPTERS -> {
                val new = !stateFlow.value.skipDuplicateChapters
                update { it.copy(skipDuplicateChapters = new) }
                launchSave { setSkipDuplicateChapters(new) }
            }
            ReaderSetting.SHOW_READING_TIMER -> {
                val new = !stateFlow.value.showReadingTimer
                update { it.copy(showReadingTimer = new) }
                launchSave { setShowReadingTimer(new) }
            }
            ReaderSetting.SHOW_BATTERY_TIME -> {
                val new = !stateFlow.value.showBatteryTime
                update { it.copy(showBatteryTime = new) }
                launchSave { setShowBatteryTime(new) }
            }
            ReaderSetting.ANIMATE_PAGE_TRANSITIONS -> {
                val new = !stateFlow.value.animatePageTransitions
                update { it.copy(animatePageTransitions = new) }
                launchSave { setAnimatePageTransitions(new) }
            }
            ReaderSetting.ALWAYS_SHOW_CHAPTER_TRANSITION -> {
                val new = !stateFlow.value.alwaysShowChapterTransition
                update { it.copy(alwaysShowChapterTransition = new) }
                launchSave { setAlwaysShowChapterTransition(new) }
            }
            ReaderSetting.SHOW_ACTIONS_ON_LONG_TAP -> {
                val new = !stateFlow.value.showActionsOnLongTap
                update { it.copy(showActionsOnLongTap = new) }
                launchSave { setShowActionsOnLongTap(new) }
            }
            ReaderSetting.EINK_FLASH_ON_PAGE_CHANGE -> {
                val new = !stateFlow.value.einkFlashOnPageChange
                update { it.copy(einkFlashOnPageChange = new) }
                launchSave { setEinkFlashOnPageChange(new) }
            }
            ReaderSetting.EINK_BLACK_AND_WHITE -> {
                val new = !stateFlow.value.einkBlackAndWhite
                update { it.copy(einkBlackAndWhite = new) }
                launchSave { setEinkBlackAndWhite(new) }
            }
            ReaderSetting.WEBTOON_DOUBLE_TAP_ZOOM -> {
                val new = !stateFlow.value.webtoonDoubleTapZoom
                update { it.copy(webtoonDoubleTapZoom = new) }
                launchSave { setWebtoonDoubleTapZoom(new) }
            }
            ReaderSetting.WEBTOON_DISABLE_ZOOM_OUT -> {
                val new = !stateFlow.value.webtoonDisableZoomOut
                update { it.copy(webtoonDisableZoomOut = new) }
                launchSave { setWebtoonDisableZoomOut(new) }
            }
            ReaderSetting.AUTO_ZOOM_WIDE_IMAGES -> {
                val new = !stateFlow.value.autoZoomWideImages
                update { it.copy(autoZoomWideImages = new) }
                launchSave { setAutoZoomWideImages(new) }
            }
            ReaderSetting.SHOW_CONTENT_IN_CUTOUT -> {
                val new = !stateFlow.value.showContentInCutout
                update { it.copy(showContentInCutout = new) }
                launchSave { setShowContentInCutout(new) }
            }
            ReaderSetting.FULLSCREEN -> {
                val new = !stateFlow.value.isFullscreen
                update { it.copy(isFullscreen = new) }
                launchSave { setFullscreen(new) }
            }
            ReaderSetting.SMALLER_TAP_ZONE -> {
                val new = !stateFlow.value.smallerTapZone
                update { it.copy(smallerTapZone = new) }
                launchSave { setSmallerTapZone(new) }
            }
        }
    }

    fun updateBackgroundColor(color: Int) {
        update { it.copy(backgroundColor = color) }
        launchSave { setBackgroundColor(color) }
    }

    fun updateReaderScale(scale: Int) {
        update { it.copy(readerScale = scale) }
        launchSave { setReaderScale(scale) }
    }

    fun updateWebtoonSidePadding(padding: Int) {
        update { it.copy(webtoonSidePadding = padding) }
        launchSave { setWebtoonSidePadding(padding) }
    }

    fun updateTapZones(config: TapZoneConfig) {
        scope.launch { settingsRepository.setTapZoneConfig(config) }
    }

    fun updateNavigationModePager(mode: Int) {
        val coerced = mode.coerceIn(0, 5)
        update { it.copy(navigationModePager = coerced) }
        launchSave { setNavigationModePager(coerced) }
    }

    fun updateNavigationModeWebtoon(mode: Int) {
        val coerced = mode.coerceIn(0, 5)
        update { it.copy(navigationModeWebtoon = coerced) }
        launchSave { setNavigationModeWebtoon(coerced) }
    }

    fun updateTapInvertModePager(mode: TapInvertMode) {
        update { it.copy(tapInvertModePager = mode) }
        launchSave { setTapInvertModePager(mode) }
    }

    fun updateTapInvertModeWebtoon(mode: TapInvertMode) {
        update { it.copy(tapInvertModeWebtoon = mode) }
        launchSave { setTapInvertModeWebtoon(mode) }
    }

    // ── Tap zone handling ────────────────────────────────────────────────────

    fun handlePageTap(
        zone: app.otakureader.feature.reader.TapZone,
        getState: () -> ReaderState,
        onNavigate: (delta: Int) -> Unit,
        onToggleMenu: () -> Unit
    ) {
        when (zone) {
            app.otakureader.feature.reader.TapZone.LEFT ->
                when (getState().readingDirection) {
                    ReadingDirection.RTL -> onNavigate(1)
                    else -> onNavigate(-1)
                }
            app.otakureader.feature.reader.TapZone.RIGHT ->
                when (getState().readingDirection) {
                    ReadingDirection.RTL -> onNavigate(-1)
                    else -> onNavigate(1)
                }
            app.otakureader.feature.reader.TapZone.CENTER -> onToggleMenu()
        }
    }

    companion object {
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 5f
    }
}
