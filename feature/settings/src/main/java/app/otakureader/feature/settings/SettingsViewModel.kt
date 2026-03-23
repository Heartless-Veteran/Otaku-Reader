package app.otakureader.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.AppPreferences
import app.otakureader.core.preferences.AiPreferences
import app.otakureader.core.preferences.BackupPreferences
import app.otakureader.core.preferences.DownloadPreferences
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.core.preferences.LibraryPreferences
import app.otakureader.core.preferences.LocalSourcePreferences
import app.otakureader.core.preferences.ReaderPreferences
import app.otakureader.core.preferences.ReadingGoalPreferences
import app.otakureader.core.preferences.SyncPreferences
import app.otakureader.core.discord.DiscordRpcService
import app.otakureader.data.backup.BackupScheduler
import app.otakureader.data.tracking.TrackManager
import app.otakureader.data.worker.ReadingReminderScheduler
import app.otakureader.domain.repository.AiRepository
import app.otakureader.domain.sync.SyncManager
import app.otakureader.domain.sync.SyncStatus as DomainSyncStatus
import app.otakureader.feature.reader.model.ImageQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val generalPreferences: GeneralPreferences,
    private val libraryPreferences: LibraryPreferences,
    private val readerPreferences: ReaderPreferences,
    private val downloadPreferences: DownloadPreferences,
    private val localSourcePreferences: LocalSourcePreferences,
    private val backupPreferences: BackupPreferences,
    private val backupRepository: app.otakureader.data.backup.repository.BackupRepository,
    private val backupScheduler: BackupScheduler,
    private val readerSettingsRepository: app.otakureader.feature.reader.repository.ReaderSettingsRepository,
    private val trackManager: TrackManager,
    private val appPreferences: AppPreferences,
    private val aiPreferences: AiPreferences,
    private val aiRepository: AiRepository,
    private val readingGoalPreferences: ReadingGoalPreferences,
    private val readingReminderScheduler: ReadingReminderScheduler,
    private val discordRpcService: DiscordRpcService,
    private val syncPreferences: SyncPreferences,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                aiPreferences.migrateLegacyApiKeyIfNeeded()
            } catch (e: Exception) {
                _effect.send(SettingsEffect.ShowSnackbar("Failed to load AI settings. You may need to re-enter your API key."))
            }
        }
        observePreferences()
        observeAiPreferences()
        observeReadingGoalPreferences()
        observeSyncPreferences()
        refreshTrackers()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            // Start with general preferences
            combine(
                generalPreferences.themeMode,
                generalPreferences.useDynamicColor,
                generalPreferences.locale,
                generalPreferences.notificationsEnabled,
                generalPreferences.updateCheckInterval,
                generalPreferences.usePureBlackDarkMode,
                generalPreferences.useHighContrast,
                generalPreferences.colorScheme,
                generalPreferences.customAccentColor
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                Triple(
                    values[0] as Int,      // themeMode
                    values[1] as Boolean,  // useDynamicColor
                    Triple(
                        values[2] as String,   // locale
                        values[3] as Boolean,  // notificationsEnabled
                        values[4] as Int       // updateCheckInterval
                    )
                ) to Triple(
                    values[5] as Boolean,  // usePureBlackDarkMode
                    values[6] as Boolean,  // useHighContrast
                    values[7] as Int       // colorScheme
                ) to (values[8] as Long)  // customAccentColor
            }.combine(generalPreferences.showNsfwContent) { base, showNsfw ->
                base to showNsfw
            }.combine(generalPreferences.discordRpcEnabled) { base, discordRpc ->
                base to discordRpc
            }
            // Library preferences
            .combine(libraryPreferences.gridSize) { base, gridSize ->
                base to gridSize
            }.combine(libraryPreferences.showBadges) { base, showBadges ->
                base to showBadges
            }.combine(libraryPreferences.updateOnlyOnWifi) { base, updateOnWifi ->
                base to updateOnWifi
            }.combine(libraryPreferences.updateOnlyPinnedCategories) { base, pinnedOnly ->
                base to pinnedOnly
            }.combine(libraryPreferences.autoRefreshOnStart) { base, autoRefresh ->
                base to autoRefresh
            }.combine(libraryPreferences.showUpdateProgress) { base, showProgress ->
                base to showProgress
            }
            // Reader preferences - Display
            .combine(readerPreferences.readerMode) { base, readerMode ->
                base to readerMode
            }.combine(readerPreferences.keepScreenOn) { base, keepScreenOn ->
                base to keepScreenOn
            }.combine(readerPreferences.fullscreen) { base, fullscreen ->
                base to fullscreen
            }.combine(readerPreferences.showContentInCutout) { base, showCutout ->
                base to showCutout
            }.combine(readerPreferences.showPageNumber) { base, showPageNum ->
                base to showPageNum
            }.combine(readerPreferences.backgroundColor) { base, bgColor ->
                base to bgColor
            }.combine(readerPreferences.animatePageTransitions) { base, animate ->
                base to animate
            }.combine(readerPreferences.showReadingModeOverlay) { base, showMode ->
                base to showMode
            }.combine(readerPreferences.showTapZonesOverlay) { base, showZones ->
                base to showZones
            }
            // Reader preferences - Scale
            .combine(readerPreferences.readerScale) { base, scale ->
                base to scale
            }.combine(readerPreferences.autoZoomWideImages) { base, autoZoom ->
                base to autoZoom
            }
            // Reader preferences - Tap Zones
            .combine(readerPreferences.tapZoneConfig) { base, tapConfig ->
                base to tapConfig
            }.combine(readerPreferences.invertTapZones) { base, invertZones ->
                base to invertZones
            }
            // Reader preferences - Volume Keys
            .combine(readerPreferences.volumeKeysEnabled) { base, volKeys ->
                base to volKeys
            }.combine(readerPreferences.volumeKeysInverted) { base, volInvert ->
                base to volInvert
            }
            // Reader preferences - Interaction
            .combine(readerPreferences.doubleTapAnimationSpeed) { base, animSpeed ->
                base to animSpeed
            }.combine(readerPreferences.showActionsOnLongTap) { base, longTap ->
                base to longTap
            }.combine(readerPreferences.savePagesToSeparateFolders) { base, separateFolders ->
                base to separateFolders
            }
            // Reader preferences - Webtoon
            .combine(readerPreferences.webtoonSidePadding) { base, padding ->
                base to padding
            }.combine(readerPreferences.webtoonMenuHideSensitivity) { base, sensitivity ->
                base to sensitivity
            }.combine(readerPreferences.webtoonDoubleTapZoom) { base, dtZoom ->
                base to dtZoom
            }.combine(readerPreferences.webtoonDisableZoomOut) { base, disableZoom ->
                base to disableZoom
            }
            // Reader preferences - E-ink
            .combine(readerPreferences.einkFlashOnPageChange) { base, flash ->
                base to flash
            }.combine(readerPreferences.einkBlackAndWhite) { base, bw ->
                base to bw
            }
            // Reader preferences - Behavior
            .combine(readerPreferences.skipReadChapters) { base, skipRead ->
                base to skipRead
            }.combine(readerPreferences.skipFilteredChapters) { base, skipFiltered ->
                base to skipFiltered
            }.combine(readerPreferences.skipDuplicateChapters) { base, skipDupes ->
                base to skipDupes
            }.combine(readerPreferences.alwaysShowChapterTransition) { base, showTransition ->
                base to showTransition
            }
            // Reader preferences - Other
            .combine(readerSettingsRepository.incognitoMode) { base, incognito ->
                base to incognito
            }.combine(readerSettingsRepository.preloadPagesBefore) { base, preloadBefore ->
                base to preloadBefore
            }.combine(readerSettingsRepository.preloadPagesAfter) { base, preloadAfter ->
                base to preloadAfter
            }.combine(readerSettingsRepository.cropBordersEnabled) { base, cropBorders ->
                base to cropBorders
            }.combine(readerSettingsRepository.imageQuality) { base, imageQuality ->
                base to imageQuality
            }.combine(readerSettingsRepository.dataSaverEnabled) { base, dataSaver ->
                base to dataSaver
            }
            // Download preferences
            .combine(downloadPreferences.deleteAfterReading) { base, deleteAfter ->
                base to deleteAfter
            }.combine(downloadPreferences.saveAsCbz) { base, saveCbz ->
                base to saveCbz
            }.combine(downloadPreferences.autoDownloadEnabled) { base, autoDownload ->
                base to autoDownload
            }.combine(downloadPreferences.downloadOnlyOnWifi) { base, dlWifi ->
                base to dlWifi
            }.combine(downloadPreferences.autoDownloadLimit) { base, dlLimit ->
                base to dlLimit
            }.combine(downloadPreferences.concurrentDownloads) { base, concurrent ->
                base to concurrent
            }.combine(downloadPreferences.downloadAheadWhileReading) { base, dlAhead ->
                base to dlAhead
            }.combine(downloadPreferences.downloadAheadOnlyOnWifi) { base, dlAheadWifi ->
                base to dlAheadWifi
            }.combine(downloadPreferences.downloadLocation) { base, dlLocation ->
                base to dlLocation
            }
            // Local source and other preferences
            .combine(localSourcePreferences.localSourceDirectory) { base, localDir ->
                base to localDir
            }.combine(appPreferences.migrationSimilarityThreshold) { base, threshold ->
                base to threshold
            }.combine(appPreferences.migrationAlwaysConfirm) { base, alwaysConfirm ->
                base to alwaysConfirm
            }.combine(appPreferences.migrationMinChapterCount) { base, minChapters ->
                base to minChapters
            }.combine(backupPreferences.autoBackupEnabled) { base, autoBackup ->
                base to autoBackup
            }.combine(backupPreferences.autoBackupIntervalHours) { base, backupInterval ->
                base to backupInterval
            }.combine(backupPreferences.autoBackupMaxCount) { base, backupMax ->
                base to backupMax
            }
            .collect { result ->
                // Unpack all the values
                @Suppress("UNCHECKED_CAST")
                val ((((((
                    (((((
                        (((((
                            (((((
                                (((((
                                    (((((
                                        ((((((
                                            themeData
                                        ), showNsfw), discordRpc)
                                    ), gridSize), showBadges)
                                ), updateOnWifi), updatePinned)
                            ), autoRefresh), showProgress)
                        ), readerMode), keepScreenOn)
                    ), fullscreen), showCutout)
                ), showPageNum), bgColor)
            ), animate), showMode)
        ), showZones)
    ), scale), autoZoom)
), tapConfig), invertZones)
), volKeys), volInvert)
), animSpeed), longTap)
), separateFolders)
), sidePadding), menuSensitivity)
), dtZoom), disableZoomOut)
), einkFlash), einkBw)
), skipRead), skipFiltered)
), skipDupes), showTransition)
), incognito), preloadBefore)
), preloadAfter), cropBorders)
), imageQuality), dataSaver)
), deleteAfter), saveCbz)
), autoDownload), dlWifi)
), dlLimit), concurrent)
), dlAhead), dlAheadWifi)
), dlLocation), localDir)
), threshold), alwaysConfirm)
), minChapters), autoBackup)
), backupInterval), backupMax) = result

                @Suppress("UNCHECKED_CAST")
                val ((((themeMode, useDynamicColor, generalData), pureBlackHighContrast), colorScheme), customAccent) = themeData
                val (locale, notificationsEnabled, updateInterval) = generalData
                val (usePureBlackDarkMode, useHighContrast, colorSchemeValue) = pureBlackHighContrast

                _state.update { current ->
                    current.copy(
                        themeMode = themeMode,
                        useDynamicColor = useDynamicColor,
                        usePureBlackDarkMode = usePureBlackDarkMode,
                        useHighContrast = useHighContrast,
                        colorScheme = colorSchemeValue,
                        customAccentColor = customAccent,
                        locale = locale,
                        notificationsEnabled = notificationsEnabled,
                        updateCheckInterval = updateInterval,
                        showNsfwContent = showNsfw,
                        discordRpcEnabled = discordRpc,
                        libraryGridSize = gridSize,
                        showBadges = showBadges,
                        updateOnlyOnWifi = updateOnWifi,
                        updateOnlyPinnedCategories = updatePinned,
                        autoRefreshOnStart = autoRefresh,
                        showUpdateProgress = showProgress,
                        readerMode = readerMode,
                        keepScreenOn = keepScreenOn,
                        fullscreen = fullscreen,
                        showContentInCutout = showCutout,
                        showPageNumber = showPageNum,
                        backgroundColor = bgColor,
                        animatePageTransitions = animate,
                        showReadingModeOverlay = showMode,
                        showTapZonesOverlay = showZones,
                        readerScale = scale,
                        autoZoomWideImages = autoZoom,
                        tapZoneConfig = tapConfig,
                        invertTapZones = invertZones,
                        volumeKeysEnabled = volKeys,
                        volumeKeysInverted = volInvert,
                        doubleTapAnimationSpeed = animSpeed,
                        showActionsOnLongTap = longTap,
                        savePagesToSeparateFolders = separateFolders,
                        webtoonSidePadding = sidePadding,
                        webtoonMenuHideSensitivity = menuSensitivity,
                        webtoonDoubleTapZoom = dtZoom,
                        webtoonDisableZoomOut = disableZoomOut,
                        einkFlashOnPageChange = einkFlash,
                        einkBlackAndWhite = einkBw,
                        skipReadChapters = skipRead,
                        skipFilteredChapters = skipFiltered,
                        skipDuplicateChapters = skipDupes,
                        alwaysShowChapterTransition = showTransition,
                        incognitoMode = incognito,
                        preloadPagesBefore = preloadBefore,
                        preloadPagesAfter = preloadAfter,
                        cropBordersEnabled = cropBorders,
                        imageQuality = imageQuality.name,
                        dataSaverEnabled = dataSaver,
                        deleteAfterReading = deleteAfter,
                        saveAsCbz = saveCbz,
                        autoDownloadEnabled = autoDownload,
                        downloadOnlyOnWifi = dlWifi,
                        autoDownloadLimit = dlLimit,
                        concurrentDownloads = concurrent,
                        downloadAheadWhileReading = dlAhead,
                        downloadAheadOnlyOnWifi = dlAheadWifi,
                        downloadLocation = dlLocation,
                        localSourceDirectory = localDir,
                        migrationSimilarityThreshold = threshold,
                        migrationAlwaysConfirm = alwaysConfirm,
                        migrationMinChapterCount = minChapters,
                        autoBackupEnabled = autoBackup,
                        autoBackupIntervalHours = backupInterval,
                        autoBackupMaxCount = backupMax,
                        // Preserve in-flight states
                        isBackupInProgress = current.isBackupInProgress,
                        isRestoreInProgress = current.isRestoreInProgress,
                        restoringBackupFileName = current.restoringBackupFileName,
                        localBackupFiles = current.localBackupFiles,
                        trackers = current.trackers,
                        trackingLoginInProgress = current.trackingLoginInProgress,
                        // Preserve AI fields
                        aiEnabled = current.aiEnabled,
                        aiTier = current.aiTier,
                        aiApiKeySet = current.aiApiKeySet,
                        showRemoveApiKeyDialog = current.showRemoveApiKeyDialog,
                        aiReadingInsights = current.aiReadingInsights,
                        aiSmartSearch = current.aiSmartSearch,
                        aiRecommendations = current.aiRecommendations,
                        aiPanelReader = current.aiPanelReader,
                        aiSfxTranslation = current.aiSfxTranslation,
                        aiSummaryTranslation = current.aiSummaryTranslation,
                        aiSourceIntelligence = current.aiSourceIntelligence,
                        aiSmartNotifications = current.aiSmartNotifications,
                        aiAutoCategorization = current.aiAutoCategorization,
                        aiTokensUsedThisMonth = current.aiTokensUsedThisMonth,
                        aiTokenTrackingPeriod = current.aiTokenTrackingPeriod,
                        // Preserve reading goal fields
                        dailyChapterGoal = current.dailyChapterGoal,
                        weeklyChapterGoal = current.weeklyChapterGoal,
                        readingRemindersEnabled = current.readingRemindersEnabled,
                        readingReminderHour = current.readingReminderHour,
                        // Preserve sync fields
                        syncEnabled = current.syncEnabled,
                        syncProviderId = current.syncProviderId,
                        syncProviderName = current.syncProviderName,
                        lastSyncTime = current.lastSyncTime,
                        syncStatus = current.syncStatus,
                        autoSyncEnabled = current.autoSyncEnabled,
                        syncIntervalHours = current.syncIntervalHours,
                        syncOnlyOnWifi = current.syncOnlyOnWifi,
                        conflictResolutionStrategy = current.conflictResolutionStrategy
                    )
                }
            }
        }
    }

    private fun refreshTrackers() {
        _state.update { it.copy(trackers = trackManager.all.map { t -> TrackerInfo(t.id, t.name, t.isLoggedIn) }) }
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                // Appearance
                is SettingsEvent.SetThemeMode -> generalPreferences.setThemeMode(event.mode)
                is SettingsEvent.SetDynamicColor -> generalPreferences.setUseDynamicColor(event.enabled)
                is SettingsEvent.SetPureBlackDarkMode -> generalPreferences.setUsePureBlackDarkMode(event.enabled)
                is SettingsEvent.SetHighContrast -> generalPreferences.setUseHighContrast(event.enabled)
                is SettingsEvent.SetColorScheme -> generalPreferences.setColorScheme(event.scheme)
                is SettingsEvent.SetCustomAccentColor -> generalPreferences.setCustomAccentColor(event.color)
                is SettingsEvent.SetLocale -> generalPreferences.setLocale(event.locale)

                // Reader - Display
                is SettingsEvent.SetReaderMode -> readerPreferences.setReaderMode(event.mode)
                is SettingsEvent.SetKeepScreenOn -> readerPreferences.setKeepScreenOn(event.enabled)
                is SettingsEvent.SetFullscreen -> readerPreferences.setFullscreen(event.enabled)
                is SettingsEvent.SetShowContentInCutout -> readerPreferences.setShowContentInCutout(event.enabled)
                is SettingsEvent.SetShowPageNumber -> readerPreferences.setShowPageNumber(event.enabled)
                is SettingsEvent.SetBackgroundColor -> readerPreferences.setBackgroundColor(event.color)
                is SettingsEvent.SetAnimatePageTransitions -> readerPreferences.setAnimatePageTransitions(event.enabled)
                is SettingsEvent.SetShowReadingModeOverlay -> readerPreferences.setShowReadingModeOverlay(event.enabled)
                is SettingsEvent.SetShowTapZonesOverlay -> readerPreferences.setShowTapZonesOverlay(event.enabled)

                // Reader - Scale
                is SettingsEvent.SetReaderScale -> readerPreferences.setReaderScale(event.scale)
                is SettingsEvent.SetAutoZoomWideImages -> readerPreferences.setAutoZoomWideImages(event.enabled)

                // Reader - Tap Zones
                is SettingsEvent.SetTapZoneConfig -> readerPreferences.setTapZoneConfig(event.config)
                is SettingsEvent.SetInvertTapZones -> readerPreferences.setInvertTapZones(event.enabled)

                // Reader - Volume Keys
                is SettingsEvent.SetVolumeKeysEnabled -> readerPreferences.setVolumeKeysEnabled(event.enabled)
                is SettingsEvent.SetVolumeKeysInverted -> readerPreferences.setVolumeKeysInverted(event.enabled)

                // Reader - Interaction
                is SettingsEvent.SetDoubleTapAnimationSpeed -> readerPreferences.setDoubleTapAnimationSpeed(event.speed)
                is SettingsEvent.SetShowActionsOnLongTap -> readerPreferences.setShowActionsOnLongTap(event.enabled)
                is SettingsEvent.SetSavePagesToSeparateFolders -> readerPreferences.setSavePagesToSeparateFolders(event.enabled)

                // Reader - Webtoon
                is SettingsEvent.SetWebtoonSidePadding -> readerPreferences.setWebtoonSidePadding(event.padding)
                is SettingsEvent.SetWebtoonMenuHideSensitivity -> readerPreferences.setWebtoonMenuHideSensitivity(event.sensitivity)
                is SettingsEvent.SetWebtoonDoubleTapZoom -> readerPreferences.setWebtoonDoubleTapZoom(event.enabled)
                is SettingsEvent.SetWebtoonDisableZoomOut -> readerPreferences.setWebtoonDisableZoomOut(event.enabled)

                // Reader - E-ink
                is SettingsEvent.SetEinkFlashOnPageChange -> readerPreferences.setEinkFlashOnPageChange(event.enabled)
                is SettingsEvent.SetEinkBlackAndWhite -> readerPreferences.setEinkBlackAndWhite(event.enabled)

                // Reader - Behavior
                is SettingsEvent.SetSkipReadChapters -> readerPreferences.setSkipReadChapters(event.enabled)
                is SettingsEvent.SetSkipFilteredChapters -> readerPreferences.setSkipFilteredChapters(event.enabled)
                is SettingsEvent.SetSkipDuplicateChapters -> readerPreferences.setSkipDuplicateChapters(event.enabled)
                is SettingsEvent.SetAlwaysShowChapterTransition -> readerPreferences.setAlwaysShowChapterTransition(event.enabled)

                // Reader - Other
                is SettingsEvent.SetIncognitoMode -> readerSettingsRepository.setIncognitoMode(event.enabled)
                is SettingsEvent.SetPreloadPagesBefore -> readerSettingsRepository.setPreloadPagesBefore(event.count)
                is SettingsEvent.SetPreloadPagesAfter -> readerSettingsRepository.setPreloadPagesAfter(event.count)
                is SettingsEvent.SetCropBordersEnabled -> readerSettingsRepository.setCropBordersEnabled(event.enabled)
                is SettingsEvent.SetImageQuality -> {
                    val normalizedInput = event.quality.trim().uppercase()
                    val quality = ImageQuality.entries.firstOrNull { 
                        it.name.equals(normalizedInput, ignoreCase = true) 
                    } ?: ImageQuality.ORIGINAL
                    readerSettingsRepository.setImageQuality(quality)
                }
                is SettingsEvent.SetDataSaverEnabled -> readerSettingsRepository.setDataSaverEnabled(event.enabled)

                // Library
                is SettingsEvent.SetLibraryGridSize -> libraryPreferences.setGridSize(event.size)
                is SettingsEvent.SetShowBadges -> libraryPreferences.setShowBadges(event.enabled)
                is SettingsEvent.SetUpdateOnlyOnWifi -> libraryPreferences.setUpdateOnlyOnWifi(event.enabled)
                is SettingsEvent.SetUpdateOnlyPinnedCategories -> libraryPreferences.setUpdateOnlyPinnedCategories(event.enabled)
                is SettingsEvent.SetAutoRefreshOnStart -> libraryPreferences.setAutoRefreshOnStart(event.enabled)
                is SettingsEvent.SetShowUpdateProgress -> libraryPreferences.setShowUpdateProgress(event.enabled)

                // Downloads
                is SettingsEvent.SetDeleteAfterReading -> downloadPreferences.setDeleteAfterReading(event.enabled)
                is SettingsEvent.SetSaveAsCbz -> downloadPreferences.setSaveAsCbz(event.enabled)
                is SettingsEvent.SetAutoDownloadEnabled -> downloadPreferences.setAutoDownloadEnabled(event.enabled)
                is SettingsEvent.SetDownloadOnlyOnWifi -> downloadPreferences.setDownloadOnlyOnWifi(event.enabled)
                is SettingsEvent.SetAutoDownloadLimit -> downloadPreferences.setAutoDownloadLimit(event.limit)
                is SettingsEvent.SetConcurrentDownloads -> downloadPreferences.setConcurrentDownloads(event.count)
                is SettingsEvent.SetDownloadAheadWhileReading -> downloadPreferences.setDownloadAheadWhileReading(event.count)
                is SettingsEvent.SetDownloadAheadOnlyOnWifi -> downloadPreferences.setDownloadAheadOnlyOnWifi(event.enabled)
                is SettingsEvent.SetDownloadLocation -> downloadPreferences.setDownloadLocation(event.location)

                // Local Source
                is SettingsEvent.SetLocalSourceDirectory -> localSourcePreferences.setLocalSourceDirectory(event.path)

                // Notifications
                is SettingsEvent.SetNotificationsEnabled -> generalPreferences.setNotificationsEnabled(event.enabled)
                is SettingsEvent.SetUpdateInterval -> generalPreferences.setUpdateCheckInterval(event.hours)

                // Backup
                SettingsEvent.OnCreateBackup -> _effect.send(SettingsEffect.ShowBackupPicker)
                SettingsEvent.OnRestoreBackup -> _effect.send(SettingsEffect.ShowRestorePicker)
                is SettingsEvent.SetAutoBackupEnabled -> handleSetAutoBackupEnabled(event.enabled)
                is SettingsEvent.SetAutoBackupInterval -> handleSetAutoBackupInterval(event.hours)
                is SettingsEvent.SetAutoBackupMaxCount -> backupPreferences.setAutoBackupMaxCount(event.count)
                SettingsEvent.RefreshLocalBackups -> refreshLocalBackups()
                is SettingsEvent.RestoreLocalBackup -> restoreLocalBackup(event.fileName)

                // Tracking
                is SettingsEvent.LoginTracker -> loginTracker(event.trackerId, event.username, event.password)
                is SettingsEvent.LogoutTracker -> logoutTracker(event.trackerId)

                // Migration
                is SettingsEvent.SetMigrationSimilarityThreshold -> appPreferences.setMigrationSimilarityThreshold(event.threshold)
                is SettingsEvent.SetMigrationAlwaysConfirm -> appPreferences.setMigrationAlwaysConfirm(event.enabled)
                is SettingsEvent.SetMigrationMinChapterCount -> appPreferences.setMigrationMinChapterCount(event.count)
                SettingsEvent.OnNavigateToMigration -> _effect.send(SettingsEffect.NavigateToMigrationEntry)

                // Browse
                is SettingsEvent.SetShowNsfwContent -> generalPreferences.setShowNsfwContent(event.enabled)

                // Discord
                is SettingsEvent.SetDiscordRpcEnabled -> handleSetDiscordRpcEnabled(event.enabled)

                // AI
                is SettingsEvent.SetAiEnabled -> aiPreferences.setAiEnabled(event.enabled)
                is SettingsEvent.SetAiTier -> aiPreferences.setAiTier(event.tier)
                is SettingsEvent.SetAiApiKey -> handleSetAiApiKey(event.key)
                SettingsEvent.RemoveAiApiKey -> _state.update { it.copy(showRemoveApiKeyDialog = true) }
                SettingsEvent.ConfirmRemoveAiApiKey -> handleConfirmRemoveAiApiKey()
                SettingsEvent.DismissRemoveApiKeyDialog -> _state.update { it.copy(showRemoveApiKeyDialog = false) }
                is SettingsEvent.SetAiReadingInsights -> aiPreferences.setAiReadingInsights(event.enabled)
                is SettingsEvent.SetAiSmartSearch -> aiPreferences.setAiSmartSearch(event.enabled)
                is SettingsEvent.SetAiRecommendations -> aiPreferences.setAiRecommendations(event.enabled)
                is SettingsEvent.SetAiPanelReader -> aiPreferences.setAiPanelReader(event.enabled)
                is SettingsEvent.SetAiSfxTranslation -> aiPreferences.setAiSfxTranslation(event.enabled)
                is SettingsEvent.SetAiSummaryTranslation -> aiPreferences.setAiSummaryTranslation(event.enabled)
                is SettingsEvent.SetAiSourceIntelligence -> aiPreferences.setAiSourceIntelligence(event.enabled)
                is SettingsEvent.SetAiSmartNotifications -> aiPreferences.setAiSmartNotifications(event.enabled)
                is SettingsEvent.SetAiAutoCategorization -> aiPreferences.setAiAutoCategorization(event.enabled)
                SettingsEvent.ClearAiCache -> handleClearAiCache()

                // Reading Goals
                is SettingsEvent.SetDailyChapterGoal -> readingGoalPreferences.setDailyChapterGoal(event.goal)
                is SettingsEvent.SetWeeklyChapterGoal -> readingGoalPreferences.setWeeklyChapterGoal(event.goal)
                is SettingsEvent.SetReadingRemindersEnabled -> handleSetReadingRemindersEnabled(event.enabled)
                is SettingsEvent.SetReadingReminderHour -> handleSetReadingReminderHour(event.hour)

                // Cloud Sync
                is SettingsEvent.SetSyncEnabled -> handleSetSyncEnabled(event.enabled, event.providerId)
                SettingsEvent.TriggerManualSync -> handleTriggerManualSync()
                is SettingsEvent.SetAutoSyncEnabled -> syncPreferences.setAutoSyncEnabled(event.enabled)
                is SettingsEvent.SetSyncIntervalHours -> syncPreferences.setSyncIntervalHours(event.hours)
                is SettingsEvent.SetSyncOnlyOnWifi -> syncPreferences.setSyncOnlyOnWifi(event.onlyWifi)
                is SettingsEvent.SetConflictResolutionStrategy -> syncPreferences.setConflictResolutionStrategy(event.strategy)
            }
        }
    }

    private fun handleSetAiApiKey(key: String) {
        viewModelScope.launch {
            if (key.isNotBlank() && !isGeminiApiKeyFormatValid(key)) {
                _effect.send(SettingsEffect.ShowSnackbar("Invalid API key format"))
                return@launch
            }
            aiPreferences.setGeminiApiKey(key)
            val persistedKey = aiPreferences.getGeminiApiKey()
            val isSet = persistedKey.isNotBlank()
            _state.update { it.copy(aiApiKeySet = isSet) }
            if (key.isNotBlank() && !isSet) {
                _effect.send(SettingsEffect.ShowSnackbar("Failed to save AI API key"))
            } else if (isSet) {
                aiRepository.clearApiKey()
                aiRepository.initialize(persistedKey)
            }
        }
    }

    private fun handleConfirmRemoveAiApiKey() {
        viewModelScope.launch {
            _state.update { it.copy(showRemoveApiKeyDialog = false) }
            aiPreferences.clearGeminiApiKey()
            aiRepository.clearApiKey()
            _state.update { it.copy(aiApiKeySet = false) }
            _effect.send(SettingsEffect.ShowSnackbar("AI API key removed"))
        }
    }

    private fun handleClearAiCache() {
        viewModelScope.launch {
            aiPreferences.setAiCacheLastCleared(System.currentTimeMillis())
            _effect.send(SettingsEffect.ShowSnackbar("AI suggestions will refresh for future requests"))
        }
    }

    private fun isGeminiApiKeyFormatValid(key: String): Boolean {
        return key.matches(Regex("^AIza[0-9A-Za-z_-]{35}$"))
    }

    private fun handleSetDiscordRpcEnabled(enabled: Boolean) {
        viewModelScope.launch {
            generalPreferences.setDiscordRpcEnabled(enabled)
            if (enabled) {
                discordRpcService.connect()
            } else {
                discordRpcService.disconnect()
            }
        }
    }

    private fun handleSetAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupPreferences.setAutoBackupEnabled(enabled)
            if (enabled) {
                val intervalHours = backupPreferences.autoBackupIntervalHours.first()
                backupScheduler.scheduleAutoBackup(intervalHours)
            } else {
                backupScheduler.cancelAutoBackup()
            }
        }
    }

    private fun handleSetAutoBackupInterval(hours: Int) {
        viewModelScope.launch {
            backupPreferences.setAutoBackupIntervalHours(hours)
            if (backupPreferences.autoBackupEnabled.first()) {
                backupScheduler.scheduleAutoBackup(hours)
            }
        }
    }

    private fun refreshLocalBackups() {
        viewModelScope.launch {
            val files = backupRepository.listLocalBackupFiles()
            _state.update { it.copy(localBackupFiles = files) }
        }
    }

    private fun restoreLocalBackup(fileName: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                isRestoreInProgress = true,
                restoringBackupFileName = fileName
            )}
            try {
                backupRepository.restoreLocalBackup(fileName)
                _effect.send(SettingsEffect.ShowSnackbar("Backup restored successfully"))
            } catch (e: Exception) {
                _effect.send(SettingsEffect.ShowSnackbar("Failed to restore backup: ${e.message}"))
            } finally {
                _state.update { it.copy(
                    isRestoreInProgress = false,
                    restoringBackupFileName = null
                )}
            }
        }
    }

    private fun loginTracker(trackerId: Int, username: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(trackingLoginInProgress = true) }
            try {
                val tracker = trackManager.get(trackerId)
                if (tracker != null) {
                    val success = tracker.login(username, password)
                    if (success) {
                        refreshTrackers()
                        _effect.send(SettingsEffect.ShowSnackbar("Logged in to ${tracker.name}"))
                    } else {
                        _effect.send(SettingsEffect.ShowSnackbar("Failed to login to ${tracker.name}"))
                    }
                }
            } catch (e: Exception) {
                _effect.send(SettingsEffect.ShowSnackbar("Error: ${e.message}"))
            } finally {
                _state.update { it.copy(trackingLoginInProgress = false) }
            }
        }
    }

    private fun logoutTracker(trackerId: Int) {
        viewModelScope.launch {
            val tracker = trackManager.get(trackerId)
            tracker?.logout()
            refreshTrackers()
            _effect.send(SettingsEffect.ShowSnackbar("Logged out from ${tracker?.name}"))
        }
    }

    private fun handleSetReadingRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            readingGoalPreferences.setReadingRemindersEnabled(enabled)
            if (enabled) {
                val hour = readingGoalPreferences.readingReminderHour.first()
                readingReminderScheduler.scheduleDailyReminder(hour)
            } else {
                readingReminderScheduler.cancelReminders()
            }
        }
    }

    private fun handleSetReadingReminderHour(hour: Int) {
        viewModelScope.launch {
            readingGoalPreferences.setReadingReminderHour(hour)
            if (readingGoalPreferences.readingRemindersEnabled.first()) {
                readingReminderScheduler.scheduleDailyReminder(hour)
            }
        }
    }

    private fun observeAiPreferences() {
        viewModelScope.launch {
            combine(
                aiPreferences.aiEnabled,
                aiPreferences.aiTier,
                aiPreferences.aiReadingInsights,
                aiPreferences.aiSmartSearch,
                aiPreferences.aiRecommendations,
                aiPreferences.aiPanelReader,
                aiPreferences.aiSfxTranslation,
                aiPreferences.aiSummaryTranslation,
                aiPreferences.aiSourceIntelligence,
                aiPreferences.aiSmartNotifications,
                aiPreferences.aiAutoCategorization
            ) { values ->
                val enabled = values[0] as Boolean
                val tier = values[1] as AiTier
                _state.update { current ->
                    current.copy(
                        aiEnabled = enabled,
                        aiTier = tier,
                        aiApiKeySet = aiPreferences.getGeminiApiKey().isNotBlank(),
                        aiReadingInsights = values[2] as Boolean,
                        aiSmartSearch = values[3] as Boolean,
                        aiRecommendations = values[4] as Boolean,
                        aiPanelReader = values[5] as Boolean,
                        aiSfxTranslation = values[6] as Boolean,
                        aiSummaryTranslation = values[7] as Boolean,
                        aiSourceIntelligence = values[8] as Boolean,
                        aiSmartNotifications = values[9] as Boolean,
                        aiAutoCategorization = values[10] as Boolean
                    )
                }
            }.collect()
        }
    }

    private fun observeReadingGoalPreferences() {
        viewModelScope.launch {
            combine(
                readingGoalPreferences.dailyChapterGoal,
                readingGoalPreferences.weeklyChapterGoal,
                readingGoalPreferences.readingRemindersEnabled,
                readingGoalPreferences.readingReminderHour
            ) { daily, weekly, reminders, hour ->
                _state.update { current ->
                    current.copy(
                        dailyChapterGoal = daily,
                        weeklyChapterGoal = weekly,
                        readingRemindersEnabled = reminders,
                        readingReminderHour = hour
                    )
                }
            }.collect()
        }
    }

    private fun observeSyncPreferences() {
        viewModelScope.launch {
            combine(
                syncPreferences.syncEnabled,
                syncPreferences.syncProviderId,
                syncPreferences.autoSyncEnabled,
                syncPreferences.syncIntervalHours,
                syncPreferences.syncOnlyOnWifi,
                syncPreferences.conflictResolutionStrategy
            ) { enabled, providerId, autoSync, interval, wifiOnly, strategy ->
                val providerName = providerId?.let { 
                    syncManager.getProviderName(it)
                }
                val lastSync = if (enabled) syncManager.getLastSyncTime() else null
                val status = when (syncManager.syncStatus.value) {
                    is DomainSyncStatus.Syncing -> SyncStatus.SYNCING
                    is DomainSyncStatus.Success -> SyncStatus.SUCCESS
                    is DomainSyncStatus.Error -> SyncStatus.ERROR
                    else -> if (enabled) SyncStatus.IDLE else SyncStatus.DISABLED
                }
                _state.update { current ->
                    current.copy(
                        syncEnabled = enabled,
                        syncProviderId = providerId,
                        syncProviderName = providerName,
                        autoSyncEnabled = autoSync,
                        syncIntervalHours = interval,
                        syncOnlyOnWifi = wifiOnly,
                        conflictResolutionStrategy = strategy,
                        lastSyncTime = lastSync,
                        syncStatus = status
                    )
                }
            }.collect()
        }
    }

    private fun handleSetSyncEnabled(enabled: Boolean, providerId: String?) {
        viewModelScope.launch {
            if (enabled && providerId != null) {
                syncManager.enableSync(providerId)
            } else {
                syncManager.disableSync()
            }
        }
    }

    private fun handleTriggerManualSync() {
        viewModelScope.launch {
            syncManager.triggerManualSync()
        }
    }
}
