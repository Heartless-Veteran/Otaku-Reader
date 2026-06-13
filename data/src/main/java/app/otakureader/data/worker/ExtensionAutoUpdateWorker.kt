package app.otakureader.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.otakureader.core.extension.domain.repository.ExtensionRepository
import app.otakureader.data.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Periodically checks installed extensions for available updates and posts a notification when
 * any are found. The actual update download/install remains user-initiated (the "Update all"
 * action in the Extensions screen).
 */
@HiltWorker
class ExtensionAutoUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val extensionRepository: ExtensionRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val updateCount = extensionRepository.checkForUpdates()
            if (updateCount > 0) {
                notifyUpdatesAvailable(updateCount)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Cap retries so a persistently-failing update check doesn't wake the device forever;
            // failure() defers to the next periodic run.
            if (runAttemptCount >= MAX_RETRIES) Result.failure() else Result.retry()
        }
    }

    private fun notifyUpdatesAvailable(count: Int) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.extension_update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.extension_update_notification_title))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.extension_update_notification_text, count, count,
                ),
            )
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "extension_updates"
        private const val NOTIFICATION_ID = 4001
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "extension_auto_update_periodic"

        /** Schedule (or reschedule) periodic extension update checks. */
        fun schedule(context: Context, intervalHours: Int = 24, wifiOnly: Boolean = true) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<ExtensionAutoUpdateWorker>(
                intervalHours.coerceAtLeast(1).toLong(), TimeUnit.HOURS,
            ).setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
