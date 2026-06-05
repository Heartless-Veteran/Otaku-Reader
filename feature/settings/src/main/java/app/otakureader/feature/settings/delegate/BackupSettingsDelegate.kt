package app.otakureader.feature.settings.delegate

import android.content.Context
import app.otakureader.core.preferences.BackupPreferences
import app.otakureader.domain.backup.BackupRepository
import app.otakureader.domain.backup.BackupScheduler
import app.otakureader.domain.backup.TachiyomiBackupImporter
import app.otakureader.feature.settings.SettingsEffect
import app.otakureader.feature.settings.SettingsEvent
import app.otakureader.feature.settings.SettingsState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSettingsDelegate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupPreferences: BackupPreferences,
    private val backupRepository: BackupRepository,
    private val backupScheduler: BackupScheduler,
    private val tachiyomiImporter: TachiyomiBackupImporter,
) {

    private var updateState: ((SettingsState) -> SettingsState) -> Unit = {}

    /** URI of the backup the user is previewing, awaiting import confirmation. */
    private var pendingImportUri: String? = null

    private fun readBytes(uri: android.net.Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Failed to read backup file")

    fun startObserving(
        scope: CoroutineScope,
        updateState: ((SettingsState) -> SettingsState) -> Unit,
    ) {
        this.updateState = updateState
        scope.launch {
            combine(
                backupPreferences.autoBackupEnabled,
                backupPreferences.autoBackupIntervalHours,
                backupPreferences.autoBackupMaxCount,
                backupPreferences.autoBackupLocationUri,
                backupPreferences.lastAutoBackupTimestamp,
            ) { autoBackup, backupInterval, backupMax, locationUri, lastBackup ->
                updateState { it.copy(backup = it.backup.copy(
                    autoBackupEnabled = autoBackup,
                    autoBackupIntervalHours = backupInterval,
                    autoBackupMaxCount = backupMax,
                    autoBackupLocationUri = locationUri,
                    lastAutoBackupTimestamp = lastBackup,
                )) }
            }.collect { }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod", "InstanceOfCheckForException")
    suspend fun handleEvent(
        event: SettingsEvent,
        sendEffect: suspend (SettingsEffect) -> Unit,
    ): Boolean = when (event) {
        SettingsEvent.OnCreateBackup -> { sendEffect(SettingsEffect.ShowBackupPicker); true }
        SettingsEvent.OnRestoreBackup -> { sendEffect(SettingsEffect.ShowRestorePicker); true }
        SettingsEvent.OnImportTachiyomiBackup -> { sendEffect(SettingsEffect.ShowTachiyomiImportPicker); true }
        is SettingsEvent.ImportTachiyomiBackupFromUri -> {
            try {
                val data = readBytes(event.uri)
                val preview = tachiyomiImporter.preview(data)
                pendingImportUri = event.uri.toString()
                updateState {
                    it.copy(backup = it.backup.copy(
                        tachiyomiImportPreview = preview,
                        pendingTachiyomiImportUri = event.uri.toString(),
                    ))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowSnackbar("Couldn't read backup: ${e.message}"))
            }
            true
        }
        SettingsEvent.CancelTachiyomiImport -> {
            pendingImportUri = null
            updateState {
                it.copy(backup = it.backup.copy(
                    tachiyomiImportPreview = null,
                    pendingTachiyomiImportUri = null,
                ))
            }
            true
        }
        is SettingsEvent.ConfirmTachiyomiImport -> {
            val uriString = pendingImportUri
            pendingImportUri = null
            if (uriString == null) {
                true
            } else {
                updateState {
                    it.copy(backup = it.backup.copy(
                        isRestoreInProgress = true,
                        isTachiyomiImporting = true,
                        tachiyomiImportPreview = null,
                        pendingTachiyomiImportUri = null,
                        tachiyomiImportProgress = 0,
                        tachiyomiImportTotal = 0,
                    ))
                }
                try {
                    val data = readBytes(android.net.Uri.parse(uriString))
                    val result = tachiyomiImporter.importBackup(
                        data = data,
                        overwriteExisting = event.overwriteExisting,
                        onProgress = { current, total ->
                            updateState {
                                it.copy(backup = it.backup.copy(
                                    tachiyomiImportProgress = current,
                                    tachiyomiImportTotal = total,
                                ))
                            }
                        },
                    )
                    sendEffect(SettingsEffect.ShowSnackbar(
                        "Imported ${result.mangaImported} manga, ${result.chaptersImported} chapters, " +
                            "${result.categoriesImported} categories, ${result.trackingImported} tracked " +
                            "(${result.skipped} skipped)"
                    ))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sendEffect(SettingsEffect.ShowSnackbar("Failed to import Tachiyomi backup: ${e.message}"))
                } finally {
                    updateState { it.copy(backup = it.backup.copy(isRestoreInProgress = false, isTachiyomiImporting = false)) }
                }
                true
            }
        }
        is SettingsEvent.CreateBackupWithUri -> {
            updateState { it.copy(backup = it.backup.copy(isBackupInProgress = true)) }
            try {
                backupRepository.createBackup(event.uri.toString())
                sendEffect(SettingsEffect.ShowSnackbar("Backup created successfully"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                sendEffect(SettingsEffect.ShowSnackbar("Failed to create backup: ${e.message}"))
            } finally {
                updateState { it.copy(backup = it.backup.copy(isBackupInProgress = false)) }
            }
            true
        }
        is SettingsEvent.RestoreBackupFromUri -> {
            updateState {
                it.copy(backup = it.backup.copy(isRestoreInProgress = true, tachiyomiImportTotal = 0, tachiyomiImportProgress = 0))
            }
            try {
                backupRepository.restoreBackup(event.uri.toString())
                sendEffect(SettingsEffect.ShowSnackbar("Backup restored successfully"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                sendEffect(SettingsEffect.ShowSnackbar("Failed to restore backup: ${e.message}"))
            } finally {
                updateState { it.copy(backup = it.backup.copy(isRestoreInProgress = false)) }
            }
            true
        }
        is SettingsEvent.SetAutoBackupEnabled -> { handleSetAutoBackupEnabled(event.enabled); true }
        is SettingsEvent.SetAutoBackupInterval -> { handleSetAutoBackupInterval(event.hours); true }
        is SettingsEvent.SetAutoBackupMaxCount -> { backupPreferences.setAutoBackupMaxCount(event.count); true }
        SettingsEvent.RequestAutoBackupLocationPicker -> { sendEffect(SettingsEffect.ShowAutoBackupLocationPicker); true }
        is SettingsEvent.SetAutoBackupLocation -> { backupPreferences.setAutoBackupLocationUri(event.uri); true }
        SettingsEvent.RefreshLocalBackups -> {
            val files = backupRepository.listLocalBackups().map { it.name }
            updateState { it.copy(backup = it.backup.copy(localBackupFiles = files)) }
            true
        }
        is SettingsEvent.RestoreLocalBackup -> {
            updateState {
                it.copy(backup = it.backup.copy(isRestoreInProgress = true, restoringBackupFileName = event.fileName, tachiyomiImportTotal = 0, tachiyomiImportProgress = 0))
            }
            try {
                val allFiles = backupRepository.listLocalBackups()
                val file = allFiles.firstOrNull { it.name == event.fileName }
                    ?: throw IllegalArgumentException("Backup file not found: ${event.fileName}")
                backupRepository.restoreLocalBackup(file)
                sendEffect(SettingsEffect.ShowSnackbar("Backup restored successfully"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                sendEffect(SettingsEffect.ShowSnackbar("Failed to restore backup: ${e.message}"))
            } finally {
                updateState { it.copy(backup = it.backup.copy(isRestoreInProgress = false, restoringBackupFileName = null)) }
            }
            true
        }
        else -> false
    }

    private suspend fun handleSetAutoBackupEnabled(enabled: Boolean) {
        backupPreferences.setAutoBackupEnabled(enabled)
        if (enabled) {
            val intervalHours = backupPreferences.autoBackupIntervalHours.first()
            backupScheduler.schedule(intervalHours)
        } else {
            backupScheduler.cancel()
        }
    }

    private suspend fun handleSetAutoBackupInterval(hours: Int) {
        backupPreferences.setAutoBackupIntervalHours(hours)
        if (backupPreferences.autoBackupEnabled.first()) {
            backupScheduler.schedule(hours)
        }
    }
}
