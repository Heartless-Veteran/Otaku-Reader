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
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.domain.repository.EhFavoritesRepository
import app.otakureader.domain.usecase.SyncEhFavoritesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that syncs the authenticated user's E-Hentai favorites
 * into the local library.
 *
 * The worker exits early (success) when:
 *   - No EH session is configured.
 *   - The NSFW content gate is disabled in settings.
 *   - The max retry count has been reached (to avoid infinite loops on persistent errors).
 *
 * Schedule via [EhFavoritesSyncWorker.schedule]; cancel via [EhFavoritesSyncWorker.cancel].
 */
@HiltWorker
class EhFavoritesSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEhFavorites: SyncEhFavoritesUseCase,
    private val ehFavoritesRepository: EhFavoritesRepository,
    private val generalPreferences: GeneralPreferences,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (runAttemptCount >= MAX_RETRIES) return Result.failure()
        if (!ehFavoritesRepository.isConfigured()) return Result.success()
        if (!generalPreferences.showNsfwContent.first()) return Result.success()
        return try {
            syncEhFavorites()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "eh_favorites_sync"
        private const val DEFAULT_INTERVAL_HOURS = 24
        private const val MAX_RETRIES = 3

        fun schedule(context: Context, intervalHours: Int = DEFAULT_INTERVAL_HOURS) {
            val safeInterval = intervalHours.coerceAtLeast(1)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<EhFavoritesSyncWorker>(
                repeatInterval = safeInterval.toLong(),
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
