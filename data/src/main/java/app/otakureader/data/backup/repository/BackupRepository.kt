package app.otakureader.data.backup.repository

import android.content.Context
import android.net.Uri
import app.otakureader.data.backup.BackupCreator
import app.otakureader.data.backup.BackupRestorer
import app.otakureader.domain.backup.BackupRepository as BackupRepositoryInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupCreator: BackupCreator,
    private val backupRestorer: BackupRestorer
) : BackupRepositoryInterface {

    override suspend fun createBackup(uriString: String) = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        val backupJson = backupCreator.createBackup()
        context.contentResolver.openOutputStream(uri)?.use { it.write(backupJson.toByteArray()) }
            ?: error("Failed to open output stream for URI: $uriString")
    }

    override suspend fun restoreBackup(uriString: String) = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        val backupJson = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Failed to open input stream for URI: $uriString")
        backupRestorer.restoreBackup(backupJson)
    }

    override suspend fun createLocalBackup(): File = withContext(Dispatchers.IO) {
        val backupJson = backupCreator.createBackup()
        val dir = getLocalBackupDir()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "otakureader_backup_$timestamp.json")
        file.writeText(backupJson)
        file
    }

    override suspend fun restoreLocalBackup(file: File) = withContext(Dispatchers.IO) {
        backupRestorer.restoreBackup(file.readText())
    }

    override suspend fun listLocalBackups(): List<File> = withContext(Dispatchers.IO) {
        getLocalBackupDir()
            .listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    override suspend fun pruneLocalBackups(maxCount: Int) = withContext(Dispatchers.IO) {
        val safeMax = maxCount.coerceAtLeast(1)
        val backups = listLocalBackups()
        if (backups.size > safeMax) {
            backups.drop(safeMax).forEach { it.delete() }
        }
    }

    private fun getLocalBackupDir(): File {
        val dir = File(context.filesDir, LOCAL_BACKUP_DIR)
        if (!dir.exists() && !dir.mkdirs()) {
            throw java.io.IOException("Failed to create backup directory: ${dir.absolutePath}")
        }
        return dir
    }

    companion object {
        const val LOCAL_BACKUP_DIR = "auto_backups"
    }
}
