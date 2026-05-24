package app.otakureader.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.otakureader.core.preferences.BackupPreferences
import app.otakureader.data.backup.BackupCreator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that creates automatic local backups.
 *
 * Respects [BackupPreferences] for interval and retention. Runs on unmetered
 * network with charging preferred. Deletes old backups to stay within max count.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupCreator: BackupCreator,
    private val backupPreferences: BackupPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = backupPreferences.autoBackupEnabled.first()
        if (!enabled) {
            Log.d(TAG, "Auto-backup disabled — skipping")
            return Result.success()
        }

        return try {
            // Create backup — file I/O performed on IO dispatcher
            val backupDir = File(applicationContext.filesDir, AUTO_BACKUP_DIR)
            val backupFile: File
            val backupJson: String

            withContext(Dispatchers.IO) {
                backupDir.mkdirs()

                val timestamp = System.currentTimeMillis()
                backupFile = File(backupDir, "auto_backup_$timestamp.json")

                backupJson = backupCreator.createBackup()
                backupFile.writeText(backupJson)
            }

            Log.i(TAG, "Auto-backup created: ${backupFile.name} (${backupFile.length()} bytes)")

            // Enforce retention limit
            cleanupOldBackups(backupDir)

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Auto-backup failed", e)
            Result.retry()
        }
    }

    private suspend fun cleanupOldBackups(backupDir: File) {
        val maxCount = backupPreferences.autoBackupMaxCount.first()

        withContext(Dispatchers.IO) {
            val backups = backupDir.listFiles { f -> f.name.startsWith("auto_backup_") }
                ?.sortedByDescending { it.lastModified() }
                ?: return@withContext

            if (backups.size > maxCount) {
                backups.drop(maxCount).forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old auto-backup: ${file.name}")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AutoBackupWorker"
        private const val WORK_NAME = "auto_backup_periodic"
        private const val AUTO_BACKUP_DIR = "auto_backups"

        /**
         * Schedule or reschedule the periodic auto-backup worker based on current preferences.
         * Call this whenever backup settings change.
         */
        fun schedule(context: Context, intervalHours: Int) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

            Log.i(TAG, "Scheduled auto-backup every $intervalHours hours")
        }

        /** Cancel the scheduled auto-backup worker. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled auto-backup")
        }
    }
}
