package app.otakureader.domain.backup

interface TachiyomiBackupImporter {
    suspend fun importBackup(backupJson: String): ImportResult
}
