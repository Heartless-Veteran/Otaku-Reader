package app.otakureader.feature.settings

data class BackupSettingsState(
    val isBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val restoringBackupFileName: String? = null,
    val autoBackupEnabled: Boolean = false,
    val autoBackupIntervalHours: Int = 24,
    val autoBackupMaxCount: Int = 5,
    val localBackupFiles: List<String> = emptyList(),
)
