@file:Suppress("MatchingDeclarationName")

package app.otakureader.feature.settings

import app.otakureader.domain.model.TachiyomiBackupPreview

data class BackupSettingsState(
    val isBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val restoringBackupFileName: String? = null,
    val autoBackupEnabled: Boolean = false,
    val autoBackupIntervalHours: Int = 24,
    val autoBackupMaxCount: Int = 5,
    val autoBackupLocationUri: String = "",
    val lastAutoBackupTimestamp: Long = 0L,
    val localBackupFiles: List<String> = emptyList(),
    // Tachiyomi import: preview shown before the user confirms; progress shown during import.
    val tachiyomiImportPreview: TachiyomiBackupPreview? = null,
    val pendingTachiyomiImportUri: String? = null,
    val isTachiyomiImporting: Boolean = false,
    val tachiyomiImportProgress: Int = 0,
    val tachiyomiImportTotal: Int = 0,
)
