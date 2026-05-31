package app.otakureader.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.otakureader.core.preferences.BackupPreferences
import app.otakureader.data.backup.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Background worker that creates an automatic backup and saves it to local storage.
 *
 * On success it:
 *  - Prunes old backup files according to [BackupPreferences.autoBackupMaxCount].
 *  - Posts a success notification via [BackupNotifier].
 *
 * On failure it posts a failure notification so the user is aware.
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val backupPreferences: BackupPreferences
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val notifier = BackupNotifier(applicationContext)
        return try {
            val file = backupRepository.createLocalBackup()

            val maxCount = backupPreferences.autoBackupMaxCount.first()
            backupRepository.pruneLocalBackups(maxCount)

            // Mirror the backup to the user-chosen location (e.g. a cloud-synced folder), if set.
            val locationUri = backupPreferences.autoBackupLocationUri.first()
            if (locationUri.isNotBlank()) {
                copyToLocation(file, locationUri)
            }

            backupPreferences.setLastAutoBackupTimestamp(System.currentTimeMillis())
            notifier.notifySuccess(file.name)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            notifier.notifyFailure(e.message)
            // Retry with WorkManager's backoff so a transient failure (e.g. low storage)
            // doesn't skip the whole interval — but cap attempts so a persistent failure
            // (corruption, missing permission, a bug) doesn't drain the battery looping.
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private fun copyToLocation(source: File, treeUriString: String) {
        try {
            val tree = DocumentFile.fromTreeUri(applicationContext, Uri.parse(treeUriString))
            if (tree == null || !tree.canWrite()) {
                Log.w(TAG, "Backup location not writable: $treeUriString")
                return
            }
            tree.findFile(source.name)?.delete()
            val target = tree.createFile("application/json", source.name) ?: return
            applicationContext.contentResolver.openOutputStream(target.uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            }
        } catch (e: Exception) {
            // Best-effort: the private-storage backup already succeeded, so a failed mirror
            // shouldn't fail the whole job. Surface it in logs for diagnosis.
            Log.e(TAG, "Failed to copy backup to $treeUriString", e)
        }
    }

    companion object {
        private const val TAG = "BackupWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
        const val WORK_NAME = "auto_backup"
    }
}
