package app.otakureader.domain.backup

import java.io.File

interface BackupRepository {
    suspend fun createBackup(uriString: String)
    suspend fun restoreBackup(uriString: String)
    suspend fun createLocalBackup(): File
    suspend fun restoreLocalBackup(file: File)
    suspend fun listLocalBackups(): List<File>
    suspend fun pruneLocalBackups(maxCount: Int)
}
