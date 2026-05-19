package app.otakureader.domain.backup

import app.otakureader.domain.model.ImportResult

interface TachiyomiBackupImporter {
    suspend fun importBackup(backupJson: String): ImportResult
}
