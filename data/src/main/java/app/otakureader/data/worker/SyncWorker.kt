package app.otakureader.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.otakureader.core.preferences.SyncSettingsStore
import app.otakureader.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background worker that drains the local sync queue and pulls remote progress.
 *
 * The worker runs on a 15-minute periodic schedule when a sync server URL is
 * configured (see [schedulePeriodicSync]).  A one-shot run can be triggered
 * immediately after a chapter is read via [enqueueSingleSync].
 *
 * If the worker fails it retries up to [MAX_RETRIES] times before giving up
 * and returning [Result.failure].  WorkManager handles back-off automatically.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val syncSettingsStore: SyncSettingsStore,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val serverUrl = syncSettingsStore.serverUrl.first()
        // Nothing to do if no server has been configured.
        if (serverUrl.isBlank()) return Result.success()

        return try {
            syncRepository.drainQueue()
            val deviceId = syncSettingsStore.ensureDeviceId()
            syncRepository.pullAndApply(
                deviceId = deviceId,
                since = System.currentTimeMillis() - PULL_WINDOW_MS,
            )
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_ONE_SHOT = "sync_one_shot"

        private const val MAX_RETRIES = 3
        private const val SYNC_INTERVAL_MINUTES = 15L

        /** Pull events from the last 7 days on each sync cycle. */
        private const val PULL_WINDOW_MS = 7L * 24 * 60 * 60 * 1_000L

        /**
         * Enqueues a periodic sync job that runs every 15 minutes while the
         * device has network connectivity.
         *
         * Uses [ExistingPeriodicWorkPolicy.KEEP] so that rescheduling (e.g. on
         * settings save) does not reset the existing timer unless the URL changed.
         */
        fun schedulePeriodicSync(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * Enqueues an immediate one-shot sync.  Replaces any pending one-shot so
         * rapid consecutive chapter reads collapse into a single network trip.
         */
        fun enqueueSingleSync(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            workManager.enqueueUniqueWork(
                WORK_NAME_ONE_SHOT,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
