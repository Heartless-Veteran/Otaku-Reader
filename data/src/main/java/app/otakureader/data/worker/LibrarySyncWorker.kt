package app.otakureader.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.otakureader.domain.repository.ReaderSettingsRepository
import app.otakureader.domain.repository.TrackerSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically pushes all pending library tracker states
 * to remote trackers (#1025).
 *
 * Requires an unmetered (Wi-Fi) connection so sync does not consume mobile data.
 * Runs every [DEFAULT_INTERVAL_HOURS] hours by default; schedule via [schedule].
 */
@HiltWorker
class LibrarySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val trackerSyncRepository: TrackerSyncRepository,
    private val readerSettingsRepository: ReaderSettingsRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (runAttemptCount >= MAX_RETRIES) return Result.failure()
        return try {
            if (readerSettingsRepository.incognitoMode.first()) return Result.success()
            val summary = trackerSyncRepository.syncAllPending()
            if (summary.failed > 0) Result.retry() else Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "library_tracker_sync"
        private const val DEFAULT_INTERVAL_HOURS = 12
        private const val MAX_RETRIES = 3

        fun schedule(context: Context, intervalHours: Int = DEFAULT_INTERVAL_HOURS) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<LibrarySyncWorker>(
                repeatInterval = intervalHours.coerceAtLeast(1).toLong(),
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
