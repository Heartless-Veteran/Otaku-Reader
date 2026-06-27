package app.otakureader.feature.reader.viewmodel.delegate

import app.otakureader.domain.model.ColorFilterMode
import app.otakureader.domain.model.ImageQuality
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.PrefetchStrategy
import app.otakureader.domain.model.ReaderMode
import app.otakureader.domain.model.ReadingDirection
import app.otakureader.domain.model.TapInvertMode
import app.otakureader.domain.model.TapZoneConfig
import app.otakureader.domain.model.VolumeKeyBehavior
import app.otakureader.domain.repository.ReaderSettingsRepository
import app.otakureader.feature.reader.ReaderState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Loads all reader settings from [ReaderSettingsRepository] in parallel and applies
 * them to the reader state, factoring in per-manga overrides.
 *
 * Extracted from [app.otakureader.feature.reader.ReaderViewModel]
 * to keep the ViewModel focused on event routing and state aggregation.
 *
 * Settings are loaded in seven parallel groups so that [load] itself stays under the
 * JVM 64 KB per-method bytecode limit. Each group runs its own [coroutineScope] with
 * internal parallel reads, so the total wall-clock time is still bounded by the single
 * slowest DataStore read across all groups.
 */
class ReaderSettingsLoaderDelegate @Inject constructor(
    private val settingsRepository: ReaderSettingsRepository,
    private val prefetchDelegate: ReaderPrefetchDelegate,
) {

    /**
     * Loads all reader settings and returns a copy of [current] with the loaded
     * values applied. Per-manga overrides on [manga] take precedence over global
     * settings for the fields that support them.
     *
     * Side effect: populates the cached fields on [prefetchDelegate]. The
     * delegate's cache is the canonical place those values live now that the
     * prefetch concern has been split out of the ViewModel.
     */
    @Suppress("LongMethod")
    suspend fun load(current: ReaderState, manga: Manga?): ReaderState = coroutineScope {
        val coreD    = async { loadCoreSettings() }
        val displayD = async { loadDisplaySettings() }
        val webtoonD = async { loadWebtoonSettings() }
        val behaviorD = async { loadBehaviorSettings() }
        val prefetchD = async { loadPrefetchSettings() }
        val volKeysD  = async { loadVolumeKeySettings() }
        val navD      = async { loadNavigationSettings() }

        val core    = coreD.await()
        val display = displayD.await()
        val webtoon = webtoonD.await()
        val behavior = behaviorD.await()
        val prefetch = prefetchD.await()
        val volKeys  = volKeysD.await()
        val nav      = navD.await()

        // Populate prefetch-delegate cache.
        prefetchDelegate.cachedPreloadBefore = prefetch.preloadBefore
        prefetchDelegate.cachedPreloadAfter  = prefetch.preloadAfter
        prefetchDelegate.cachedSmartPrefetchEnabled = prefetch.smartPrefetchEnabled
        prefetchDelegate.cachedPrefetchStrategy =
            if (prefetch.prefetchStrategyOrdinal >= 0)
                PrefetchStrategy.fromOrdinal(prefetch.prefetchStrategyOrdinal)
            else PrefetchStrategy.Balanced
        prefetchDelegate.cachedAdaptiveLearningEnabled = prefetch.adaptiveLearningEnabled
        prefetchDelegate.cachedPrefetchAdjacentChapters = prefetch.prefetchAdjacentChapters
        prefetchDelegate.cachedPrefetchOnlyOnWiFi = prefetch.prefetchOnlyOnWiFi

        // Apply per-manga overrides if they exist (#260).
        val effectiveMode = manga?.readerMode?.let { ReaderMode.entries.getOrNull(it) } ?: core.mode
        val effectiveDirection = manga?.readerDirection?.let {
            if (it == 0) ReadingDirection.LTR else ReadingDirection.RTL
        } ?: core.direction
        val effectiveColorFilter = manga?.readerColorFilter?.let {
            ColorFilterMode.entries.getOrNull(it)
        } ?: core.colorFilterMode
        val effectiveTintColor = manga?.readerCustomTintColor ?: core.customTintColor

        // Compute effective volume-key state from global settings + mode-specific behavior.
        val modeBehavior = when (effectiveMode) {
            ReaderMode.SINGLE_PAGE  -> volKeys.single
            ReaderMode.DUAL_PAGE    -> volKeys.dual
            ReaderMode.WEBTOON      -> volKeys.webtoon
            ReaderMode.SMART_PANELS -> volKeys.smart
        }
        val effectiveVolKeysEnabled: Boolean
        val effectiveVolKeysInverted: Boolean
        when (modeBehavior) {
            VolumeKeyBehavior.DISABLED -> {
                effectiveVolKeysEnabled  = false
                effectiveVolKeysInverted = core.volumeKeysInverted
            }
            VolumeKeyBehavior.NORMAL -> {
                effectiveVolKeysEnabled  = true
                effectiveVolKeysInverted = false
            }
            VolumeKeyBehavior.INVERTED -> {
                effectiveVolKeysEnabled  = true
                effectiveVolKeysInverted = true
            }
            else -> {
                effectiveVolKeysEnabled  = core.volumeKeysEnabled
                effectiveVolKeysInverted = core.volumeKeysInverted
            }
        }

        current.copy(
            mode                      = effectiveMode,
            brightness                = core.brightness,
            keepScreenOn              = core.keepScreenOn,
            showPageNumber            = core.showPageNumber,
            readingDirection          = effectiveDirection,
            volumeKeysEnabled         = effectiveVolKeysEnabled,
            volumeKeysInverted        = effectiveVolKeysInverted,
            isFullscreen              = core.isFullscreen,
            incognitoMode             = core.incognitoMode,
            colorFilterMode           = effectiveColorFilter,
            customTintColor           = effectiveTintColor,
            showReadingTimer          = core.showReadingTimer,
            showBatteryTime           = core.showBatteryTime,
            cropBordersEnabled        = core.cropBordersEnabled,
            imageQuality              = core.imageQuality,
            dataSaverEnabled          = core.dataSaverEnabled,
            showContentInCutout       = core.showContentInCutout,
            backgroundColor           = core.backgroundColor,
            animatePageTransitions    = display.animatePageTransitions,
            showReadingModeOverlay    = display.showReadingModeOverlay,
            showTapZonesOverlay       = display.showTapZonesOverlay,
            readerScale               = display.readerScale,
            autoZoomWideImages        = display.autoZoomWideImages,
            invertTapZones            = display.invertTapZones,
            tapZoneConfig             = display.tapZoneConfig,
            webtoonSidePadding        = webtoon.sidePadding,
            webtoonGapDp              = webtoon.gapDp,
            webtoonMenuHideSensitivity = webtoon.menuHideSensitivity,
            webtoonDoubleTapZoom      = webtoon.doubleTapZoom,
            webtoonDisableZoomOut     = webtoon.disableZoomOut,
            einkFlashOnPageChange     = webtoon.einkFlashOnPageChange,
            einkBlackAndWhite         = webtoon.einkBlackAndWhite,
            skipReadChapters          = behavior.skipReadChapters,
            skipFilteredChapters      = behavior.skipFilteredChapters,
            skipDuplicateChapters     = behavior.skipDuplicateChapters,
            alwaysShowChapterTransition = behavior.alwaysShowChapterTransition,
            showActionsOnLongTap      = behavior.showActionsOnLongTap,
            showPageThumbnailStrip    = behavior.showPageThumbnailStrip,
            savePagesToSeparateFolders = behavior.savePagesToSeparateFolders,
            autoScrollSpeed           = behavior.autoScrollSpeed,
            secureScreen              = behavior.secureScreen,
            navigationModePager       = nav.modePager,
            navigationModeWebtoon     = nav.modeWebtoon,
            tapInvertModePager        = nav.invertPager,
            tapInvertModeWebtoon      = nav.invertWebtoon,
            smallerTapZone            = nav.smallerTapZone,
        )
    }

    // ── Grouped setting loaders ───────────────────────────────────────────────

    private data class CoreSettings(
        val mode: ReaderMode,
        val brightness: Float,
        val keepScreenOn: Boolean,
        val showPageNumber: Boolean,
        val direction: ReadingDirection,
        val volumeKeysEnabled: Boolean,
        val volumeKeysInverted: Boolean,
        val isFullscreen: Boolean,
        val incognitoMode: Boolean,
        val colorFilterMode: ColorFilterMode,
        val customTintColor: Long,
        val showReadingTimer: Boolean,
        val showBatteryTime: Boolean,
        val cropBordersEnabled: Boolean,
        val imageQuality: ImageQuality,
        val dataSaverEnabled: Boolean,
        val showContentInCutout: Boolean,
        val backgroundColor: Int,
    )

    @Suppress("InstanceOfCheckForException", "LongMethod", "CognitiveComplexMethod")
    private suspend fun loadCoreSettings(): CoreSettings = coroutineScope {
        val modeD              = async { settingsRepository.readerMode.first() }
        val brightnessD        = async { settingsRepository.brightness.first() }
        val keepScreenOnD      = async { settingsRepository.keepScreenOn.first() }
        val showPageNumberD    = async { settingsRepository.showPageNumber.first() }
        val directionD         = async { settingsRepository.readingDirection.first() }
        val volEnabledD        = async { settingsRepository.volumeKeysEnabled.first() }
        val volInvertedD       = async { settingsRepository.volumeKeysInverted.first() }
        val fullscreenD        = async { settingsRepository.fullscreen.first() }
        val incognitoD         = async { settingsRepository.incognitoMode.first() }
        val colorFilterD       = async { settingsRepository.colorFilterMode.first() }
        val tintColorD         = async { settingsRepository.customTintColor.first() }
        val showContentInCutoutD = async { settingsRepository.showContentInCutout.first() }
        val backgroundColorD   = async { settingsRepository.backgroundColor.first() }
        val showReadingTimerD  = async {
            try { settingsRepository.showReadingTimer.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                false
            }
        }
        val showBatteryTimeD   = async {
            try { settingsRepository.showBatteryTime.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                false
            }
        }
        val cropBordersD       = async {
            try { settingsRepository.cropBordersEnabled.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                false
            }
        }
        val imageQualityD      = async {
            try { settingsRepository.imageQuality.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ImageQuality.ORIGINAL
            }
        }
        val dataSaverD         = async {
            try { settingsRepository.dataSaverEnabled.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                false
            }
        }
        CoreSettings(
            mode              = modeD.await(),
            brightness        = brightnessD.await(),
            keepScreenOn      = keepScreenOnD.await(),
            showPageNumber    = showPageNumberD.await(),
            direction         = directionD.await(),
            volumeKeysEnabled = volEnabledD.await(),
            volumeKeysInverted = volInvertedD.await(),
            isFullscreen      = fullscreenD.await(),
            incognitoMode     = incognitoD.await(),
            colorFilterMode   = colorFilterD.await(),
            customTintColor   = tintColorD.await(),
            showReadingTimer  = showReadingTimerD.await(),
            showBatteryTime   = showBatteryTimeD.await(),
            cropBordersEnabled = cropBordersD.await(),
            imageQuality      = imageQualityD.await(),
            dataSaverEnabled  = dataSaverD.await(),
            showContentInCutout = showContentInCutoutD.await(),
            backgroundColor   = backgroundColorD.await(),
        )
    }

    private data class DisplaySettings(
        val animatePageTransitions: Boolean,
        val showReadingModeOverlay: Boolean,
        val showTapZonesOverlay: Boolean,
        val readerScale: Int,
        val autoZoomWideImages: Boolean,
        val invertTapZones: Boolean,
        val tapZoneConfig: TapZoneConfig,
    )

    @Suppress("InstanceOfCheckForException")
    private suspend fun loadDisplaySettings(): DisplaySettings = coroutineScope {
        val animateD           = async { settingsRepository.animatePageTransitions.first() }
        val showModeOverlayD   = async { settingsRepository.showReadingModeOverlay.first() }
        val showTapZonesD      = async { settingsRepository.showTapZonesOverlay.first() }
        val readerScaleD       = async { settingsRepository.readerScale.first() }
        val autoZoomD          = async { settingsRepository.autoZoomWideImages.first() }
        val invertTapD         = async { settingsRepository.invertTapZones.first() }
        val tapZoneConfigD     = async {
            try { settingsRepository.tapZoneConfig.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                TapZoneConfig()
            }
        }
        DisplaySettings(
            animatePageTransitions = animateD.await(),
            showReadingModeOverlay = showModeOverlayD.await(),
            showTapZonesOverlay    = showTapZonesD.await(),
            readerScale            = readerScaleD.await(),
            autoZoomWideImages     = autoZoomD.await(),
            invertTapZones         = invertTapD.await(),
            tapZoneConfig          = tapZoneConfigD.await(),
        )
    }

    private data class WebtoonSettings(
        val sidePadding: Int,
        val gapDp: Int,
        val menuHideSensitivity: Int,
        val doubleTapZoom: Boolean,
        val disableZoomOut: Boolean,
        val einkFlashOnPageChange: Boolean,
        val einkBlackAndWhite: Boolean,
    )

    private suspend fun loadWebtoonSettings(): WebtoonSettings = coroutineScope {
        val sidePaddingD      = async { settingsRepository.webtoonSidePadding.first() }
        val gapDpD            = async { settingsRepository.webtoonGapDp.first() }
        val menuSensitivityD  = async { settingsRepository.webtoonMenuHideSensitivity.first() }
        val doubleTapZoomD    = async { settingsRepository.webtoonDoubleTapZoom.first() }
        val disableZoomOutD   = async { settingsRepository.webtoonDisableZoomOut.first() }
        val einkFlashD        = async { settingsRepository.einkFlashOnPageChange.first() }
        val einkBlackWhiteD   = async { settingsRepository.einkBlackAndWhite.first() }
        WebtoonSettings(
            sidePadding         = sidePaddingD.await(),
            gapDp               = gapDpD.await(),
            menuHideSensitivity = menuSensitivityD.await(),
            doubleTapZoom       = doubleTapZoomD.await(),
            disableZoomOut      = disableZoomOutD.await(),
            einkFlashOnPageChange = einkFlashD.await(),
            einkBlackAndWhite   = einkBlackWhiteD.await(),
        )
    }

    private data class BehaviorSettings(
        val skipReadChapters: Boolean,
        val skipFilteredChapters: Boolean,
        val skipDuplicateChapters: Boolean,
        val alwaysShowChapterTransition: Boolean,
        val showActionsOnLongTap: Boolean,
        val showPageThumbnailStrip: Boolean,
        val savePagesToSeparateFolders: Boolean,
        val autoScrollSpeed: Float,
        val secureScreen: Boolean,
    )

    @Suppress("InstanceOfCheckForException")
    private suspend fun loadBehaviorSettings(): BehaviorSettings = coroutineScope {
        val skipReadD         = async { settingsRepository.skipReadChapters.first() }
        val skipFilteredD     = async { settingsRepository.skipFilteredChapters.first() }
        val skipDuplicateD    = async { settingsRepository.skipDuplicateChapters.first() }
        val alwaysTransitionD = async { settingsRepository.alwaysShowChapterTransition.first() }
        val longTapActionsD   = async { settingsRepository.showActionsOnLongTap.first() }
        val thumbnailStripD   = async { settingsRepository.showPageThumbnailStrip.first() }
        val separateFoldersD  = async { settingsRepository.savePagesToSeparateFolders.first() }
        val autoScrollSpeedD  = async { settingsRepository.autoScrollSpeed.first() }
        val secureScreenD     = async {
            try { settingsRepository.secureScreen.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                false
            }
        }
        BehaviorSettings(
            skipReadChapters           = skipReadD.await(),
            skipFilteredChapters       = skipFilteredD.await(),
            skipDuplicateChapters      = skipDuplicateD.await(),
            alwaysShowChapterTransition = alwaysTransitionD.await(),
            showActionsOnLongTap       = longTapActionsD.await(),
            showPageThumbnailStrip     = thumbnailStripD.await(),
            savePagesToSeparateFolders = separateFoldersD.await(),
            autoScrollSpeed            = autoScrollSpeedD.await(),
            secureScreen               = secureScreenD.await(),
        )
    }

    private data class PrefetchSettings(
        val preloadBefore: Int,
        val preloadAfter: Int,
        val smartPrefetchEnabled: Boolean,
        val prefetchStrategyOrdinal: Int,
        val adaptiveLearningEnabled: Boolean,
        val prefetchAdjacentChapters: Boolean,
        val prefetchOnlyOnWiFi: Boolean,
    )

    @Suppress("InstanceOfCheckForException", "CognitiveComplexMethod")
    private suspend fun loadPrefetchSettings(): PrefetchSettings = coroutineScope {
        val preloadBeforeD     = async {
            try { settingsRepository.preloadPagesBefore.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ReaderSettingsRepository.DEFAULT_PRELOAD_PAGES
            }
        }
        val preloadAfterD      = async {
            try { settingsRepository.preloadPagesAfter.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ReaderSettingsRepository.DEFAULT_PRELOAD_PAGES
            }
        }
        val smartPrefetchD     = async {
            try { settingsRepository.smartPrefetchEnabled.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                true
            }
        }
        val strategyOrdinalD   = async {
            try { settingsRepository.prefetchStrategyOrdinal.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                -1
            }
        }
        val adaptiveLearningD  = async {
            try { settingsRepository.adaptiveLearningEnabled.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                true
            }
        }
        val prefetchAdjacentD  = async {
            try { settingsRepository.prefetchAdjacentChapters.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                false
            }
        }
        val prefetchWifiOnlyD  = async {
            try { settingsRepository.prefetchOnlyOnWiFi.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                false
            }
        }
        PrefetchSettings(
            preloadBefore            = preloadBeforeD.await(),
            preloadAfter             = preloadAfterD.await(),
            smartPrefetchEnabled     = smartPrefetchD.await(),
            prefetchStrategyOrdinal  = strategyOrdinalD.await(),
            adaptiveLearningEnabled  = adaptiveLearningD.await(),
            prefetchAdjacentChapters = prefetchAdjacentD.await(),
            prefetchOnlyOnWiFi       = prefetchWifiOnlyD.await(),
        )
    }

    private data class VolumeKeySettings(
        val single: Int,
        val dual: Int,
        val webtoon: Int,
        val smart: Int,
    )

    @Suppress("InstanceOfCheckForException", "CognitiveComplexMethod")
    private suspend fun loadVolumeKeySettings(): VolumeKeySettings = coroutineScope {
        val singleD  = async {
            try { settingsRepository.volumeKeyBehaviorSinglePage.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                VolumeKeyBehavior.INHERIT
            }
        }
        val dualD    = async {
            try { settingsRepository.volumeKeyBehaviorDualPage.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                VolumeKeyBehavior.INHERIT
            }
        }
        val webtoonD = async {
            try { settingsRepository.volumeKeyBehaviorWebtoon.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                VolumeKeyBehavior.INHERIT
            }
        }
        val smartD   = async {
            try { settingsRepository.volumeKeyBehaviorSmartPanels.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                VolumeKeyBehavior.INHERIT
            }
        }
        VolumeKeySettings(
            single  = singleD.await(),
            dual    = dualD.await(),
            webtoon = webtoonD.await(),
            smart   = smartD.await(),
        )
    }

    private data class NavigationSettings(
        val modePager: Int = 0,
        val modeWebtoon: Int = 0,
        val invertPager: TapInvertMode = TapInvertMode.NONE,
        val invertWebtoon: TapInvertMode = TapInvertMode.NONE,
        val smallerTapZone: Boolean = false,
    )

    @Suppress("CognitiveComplexMethod", "InstanceOfCheckForException")
    private suspend fun loadNavigationSettings(): NavigationSettings = coroutineScope {
        val modePagerD = async {
            try { settingsRepository.navigationModePager.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                0
            }
        }
        val modeWebtoonD = async {
            try { settingsRepository.navigationModeWebtoon.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                0
            }
        }
        val invertPagerD = async {
            try { settingsRepository.tapInvertModePager.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                TapInvertMode.NONE
            }
        }
        val invertWebtoonD = async {
            try { settingsRepository.tapInvertModeWebtoon.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                TapInvertMode.NONE
            }
        }
        val smallerD = async {
            try { settingsRepository.smallerTapZone.first() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                false
            }
        }
        NavigationSettings(
            modePager     = modePagerD.await(),
            modeWebtoon   = modeWebtoonD.await(),
            invertPager   = invertPagerD.await(),
            invertWebtoon = invertWebtoonD.await(),
            smallerTapZone = smallerD.await(),
        )
    }
}
