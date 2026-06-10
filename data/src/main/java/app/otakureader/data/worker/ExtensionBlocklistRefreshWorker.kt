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
import app.otakureader.core.extension.blocklist.ExtensionBlocklistFetcher
import app.otakureader.core.extension.blocklist.ExtensionBlocklistStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Daily refresh of the remote extension blocklist (#1018).
 *
 * Failure is retried by WorkManager; the previously cached list keeps enforcing in
 * the meantime, so a missed fetch never disables the blocklist. Scheduled
 * unconditionally from MainActivity — this is a security mechanism, not a user
 * preference.
 */
@HiltWorker
class ExtensionBlocklistRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fetcher: ExtensionBlocklistFetcher,
    private val store: ExtensionBlocklistStore,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val document = fetcher.fetch().getOrElse { return Result.retry() }
            store.replace(document)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "extension_blocklist_refresh"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<ExtensionBlocklistRefreshWorker>(
                1, TimeUnit.DAYS,
            )
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
