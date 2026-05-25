package app.otakureader.feature.reader.viewmodel.delegate

import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.PageBookmark
import app.otakureader.domain.model.TapZoneConfig
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.PageBookmarkRepository
import app.otakureader.domain.repository.ReaderSettingsRepository
import app.otakureader.domain.model.ReaderMode
import app.otakureader.feature.reader.PageRotation
import app.otakureader.feature.reader.ReaderEffect
import app.otakureader.feature.reader.ReaderEvent
import app.otakureader.feature.reader.ReaderSetting
import app.otakureader.feature.reader.ReaderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns display and interaction controls for the reader:
 *  - Reader mode, direction, rotation (display)
 *  - Menu, gallery, fullscreen (overlay)
 *  - Brightness, zoom, auto-scroll
 *  - Color filter and background color
 *  - Page bookmarks
 *  - Reader settings toggles and tap-zone config
 *
 * Extracted from [app.otakureader.feature.reader.ReaderViewModel] so the
 * ViewModel remains a thin event-router delegating all domain-specific
 * behaviour to focused delegates.
 */
class ReaderDisplayDelegate @Inject constructor(
    private val settingsRepository: ReaderSettingsRepository,
    private val mangaRepository: MangaRepository,
    private val pageBookmarkRepository: PageBookmarkRepository,
) {

    fun handleDisplay(
        event: ReaderEvent.DisplayControl,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
        scope: CoroutineScope,
    ) {
        when (event) {
            is ReaderEvent.OnModeChange -> changeReaderMode(event.mode, updateState, scope)
            is ReaderEvent.OnDirectionChange -> {
                updateState { it.copy(readingDirection = event.direction) }
                scope.launch { settingsRepository.setReadingDirection(event.direction) }
            }
            ReaderEvent.RotateCW -> updateState { it.copy(pageRotation = it.pageRotation.next()) }
            ReaderEvent.ResetRotation -> updateState { it.copy(pageRotation = PageRotation.NONE) }
        }
    }

    fun handleOverlay(
        event: ReaderEvent.OverlayControl,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
        scope: CoroutineScope,
    ) {
        when (event) {
            ReaderEvent.ToggleMenu -> updateState { it.copy(isMenuVisible = !it.isMenuVisible) }
            ReaderEvent.ToggleGallery -> updateState { it.copy(isGalleryOpen = !it.isGalleryOpen) }
            is ReaderEvent.SetGalleryColumns ->
                updateState { it.copy(galleryColumns = event.columns.coerceIn(2, 4)) }
            ReaderEvent.ToggleFullscreen -> {
                var newFullscreen = false
                updateState { current ->
                    newFullscreen = !current.isFullscreen
                    current.copy(isFullscreen = newFullscreen)
                }
                scope.launch { settingsRepository.setFullscreen(newFullscreen) }
            }
        }
    }

    fun handleBrightness(
        event: ReaderEvent.BrightnessControl,
        state: ReaderState,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
        scope: CoroutineScope,
    ) {
        val newBrightness = when (event) {
            is ReaderEvent.OnBrightnessChange -> event.brightness
            ReaderEvent.BrightnessUp -> state.brightness + BRIGHTNESS_INCREMENT
            ReaderEvent.BrightnessDown -> state.brightness - BRIGHTNESS_INCREMENT
        }.coerceIn(0.1f, 1.5f)
        updateState { it.copy(brightness = newBrightness) }
        scope.launch { settingsRepository.setBrightness(newBrightness) }
    }

    fun handleZoom(
        event: ReaderEvent.ZoomControl,
        state: ReaderState,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
    ) {
        val newZoom = when (event) {
            is ReaderEvent.OnZoomChange -> event.zoom
            ReaderEvent.ZoomIn -> state.zoomLevel + ZOOM_INCREMENT
            ReaderEvent.ZoomOut -> state.zoomLevel - ZOOM_INCREMENT
            ReaderEvent.ResetZoom -> 1f
            ReaderEvent.ZoomToWidth -> 1.5f
            ReaderEvent.ZoomToHeight -> 1.2f
        }.coerceIn(MIN_ZOOM, MAX_ZOOM)
        updateState { it.copy(zoomLevel = newZoom) }
    }

    fun handleAutoScroll(
        event: ReaderEvent.AutoScrollControl,
        state: ReaderState,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
        scope: CoroutineScope,
    ) {
        when (event) {
            ReaderEvent.ToggleAutoScroll ->
                updateState { it.copy(isAutoScrollEnabled = !it.isAutoScrollEnabled) }
            is ReaderEvent.OnAutoScrollSpeedChange ->
                setAutoScrollSpeed(event.speed, updateState, scope)
            ReaderEvent.AutoScrollSpeedUp ->
                setAutoScrollSpeed(state.autoScrollSpeed + AUTO_SCROLL_INCREMENT, updateState, scope)
            ReaderEvent.AutoScrollSpeedDown ->
                setAutoScrollSpeed(state.autoScrollSpeed - AUTO_SCROLL_INCREMENT, updateState, scope)
        }
    }

    fun handleColorFilter(
        event: ReaderEvent.ColorFilterControl,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
        scope: CoroutineScope,
        getCurrentManga: () -> Manga?,
        onMangaUpdated: (Manga?) -> Unit,
    ) {
        when (event) {
            is ReaderEvent.SetColorFilterMode -> {
                updateState { it.copy(colorFilterMode = event.mode) }
                scope.launch { settingsRepository.setColorFilterMode(event.mode) }
            }
            is ReaderEvent.SetCustomTintColor -> {
                updateState { it.copy(customTintColor = event.color) }
                scope.launch { settingsRepository.setCustomTintColor(event.color) }
            }
            is ReaderEvent.SetReaderBackgroundColor -> {
                updateState { it.copy(readerBackgroundColor = event.color) }
                scope.launch {
                    getCurrentManga()?.let { manga ->
                        val updated = manga.copy(readerBackgroundColor = event.color)
                        mangaRepository.updateManga(updated)
                        onMangaUpdated(updated)
                    }
                }
            }
        }
    }

    fun handleSettings(
        event: ReaderEvent.SettingsControl,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
        scope: CoroutineScope,
    ) {
        when (event) {
            is ReaderEvent.ToggleSetting -> toggleSetting(event.setting, updateState, scope)
            is ReaderEvent.UpdateTapZones -> scope.launch {
                settingsRepository.setTapZoneConfig(event.config)
            }
        }
    }

    fun toggleBookmark(
        state: ReaderState,
        chapterId: Long,
        mangaId: Long,
        scope: CoroutineScope,
        sendEffect: suspend (ReaderEffect) -> Unit,
    ) {
        val currentPage = state.currentPage
        scope.launch {
            if (state.isCurrentPageBookmarked) {
                pageBookmarkRepository.removeBookmark(chapterId, currentPage)
                sendEffect(ReaderEffect.ShowSnackbar("Page bookmark removed"))
            } else {
                pageBookmarkRepository.addBookmark(
                    PageBookmark(
                        mangaId = mangaId,
                        chapterId = chapterId,
                        pageIndex = currentPage
                    )
                )
                sendEffect(ReaderEffect.ShowSnackbar("Page bookmarked"))
            }
        }
    }

    private fun changeReaderMode(
        mode: ReaderMode,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
        scope: CoroutineScope,
    ) {
        updateState { current ->
            val withMode = current.copy(mode = mode)
            if (mode == ReaderMode.DUAL_PAGE && withMode.currentPage % 2 != 0) {
                withMode.copy(currentPage = withMode.currentPage - 1)
            } else {
                withMode
            }
        }
        scope.launch { settingsRepository.setReaderMode(mode) }
    }

    private fun setAutoScrollSpeed(
        speed: Float,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
        scope: CoroutineScope,
    ) {
        val clamped = speed.coerceIn(10f, 500f)
        updateState { it.copy(autoScrollSpeed = clamped) }
        scope.launch { settingsRepository.setAutoScrollSpeed(clamped) }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun toggleSetting(
        setting: ReaderSetting,
        updateState: ((ReaderState) -> ReaderState) -> Unit,
        scope: CoroutineScope,
    ) {
        when (setting) {
            ReaderSetting.KEEP_SCREEN_ON -> {
                var v = false
                updateState { it.copy(keepScreenOn = (!it.keepScreenOn).also { v = it }) }
                scope.launch { settingsRepository.setKeepScreenOn(v) }
            }
            ReaderSetting.SHOW_PAGE_NUMBER -> {
                var v = false
                updateState { it.copy(showPageNumber = (!it.showPageNumber).also { v = it }) }
                scope.launch { settingsRepository.setShowPageNumber(v) }
            }
            ReaderSetting.DOUBLE_TAP_ZOOM -> {
                var v = false
                updateState { it.copy(doubleTapZoomEnabled = (!it.doubleTapZoomEnabled).also { v = it }) }
                scope.launch { settingsRepository.setDoubleTapZoomEnabled(v) }
            }
            ReaderSetting.VOLUME_KEY_NAVIGATION -> {
                var v = false
                updateState { it.copy(volumeKeysEnabled = (!it.volumeKeysEnabled).also { v = it }) }
                scope.launch { settingsRepository.setVolumeKeysEnabled(v) }
            }
            ReaderSetting.VOLUME_KEYS_INVERTED -> {
                var v = false
                updateState { it.copy(volumeKeysInverted = (!it.volumeKeysInverted).also { v = it }) }
                scope.launch { settingsRepository.setVolumeKeysInverted(v) }
            }
            ReaderSetting.INCOGNITO_MODE -> {
                var v = false
                updateState { it.copy(incognitoMode = (!it.incognitoMode).also { v = it }) }
                scope.launch { settingsRepository.setIncognitoMode(v) }
            }
            ReaderSetting.CROP_BORDERS -> {
                var v = false
                updateState { it.copy(cropBordersEnabled = (!it.cropBordersEnabled).also { v = it }) }
                scope.launch { settingsRepository.setCropBordersEnabled(v) }
            }
            ReaderSetting.SKIP_READ_CHAPTERS -> {
                var v = false
                updateState { it.copy(skipReadChapters = (!it.skipReadChapters).also { v = it }) }
                scope.launch { settingsRepository.setSkipReadChapters(v) }
            }
            ReaderSetting.SKIP_FILTERED_CHAPTERS -> {
                var v = false
                updateState { it.copy(skipFilteredChapters = (!it.skipFilteredChapters).also { v = it }) }
                scope.launch { settingsRepository.setSkipFilteredChapters(v) }
            }
            ReaderSetting.SKIP_DUPLICATE_CHAPTERS -> {
                var v = false
                updateState { it.copy(skipDuplicateChapters = (!it.skipDuplicateChapters).also { v = it }) }
                scope.launch { settingsRepository.setSkipDuplicateChapters(v) }
            }
        }
    }

    companion object {
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 5f
        const val ZOOM_INCREMENT = 0.25f
        const val BRIGHTNESS_INCREMENT = 0.1f
        const val AUTO_SCROLL_INCREMENT = 50f
    }
}
