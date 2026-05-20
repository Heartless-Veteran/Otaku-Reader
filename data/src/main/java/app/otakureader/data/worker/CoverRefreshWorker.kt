package app.otakureader.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.otakureader.domain.repository.MangaRepository
import coil3.ImageLoader
import coil3.disk.DiskCachePolicy
import coil3.memory.MemoryCachePolicy
import coil3.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

private const val COVER_REFRESH_CHANNEL_ID = "cover_refresh_channel"
private const val COVER_REFRESH_PROGRESS_ID = Int.MAX_VALUE - 2
private const val COVER_BATCH_SIZE = 10
private const val TAG = "CoverRefreshWorker"
private const val WORK_NAME = "cover_refresh_work"

@HiltWorker
class CoverRefreshWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mangaRepository: MangaRepository,
    private val imageLoader: ImageLoader,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        createChannel()
        setForeground(buildForeground(0, 0))

        return try {
            val libraryManga = mangaRepository.getLibraryManga().first()
            val total = libraryManga.size

            libraryManga
                .mapNotNull { it.thumbnailUrl }
                .distinct()
                .forEachIndexed { index, url ->
                    if (index % COVER_BATCH_SIZE == 0) {
                        setForeground(buildForeground(index, total))
                    }
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .memoryCachePolicy(MemoryCachePolicy.DISABLED)
                        .diskCachePolicy(DiskCachePolicy.WRITE_ONLY)
                        .build()
                    try {
                        imageLoader.execute(request)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to refresh cover: $url", e)
                    }
                }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cover refresh failed", e)
            Result.failure()
        }
    }

    private fun buildForeground(current: Int, total: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, COVER_REFRESH_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Refreshing library covers…")
            .apply {
                if (total > 0) {
                    setContentText("$current / $total")
                    setProgress(total, current, false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        return ForegroundInfo(COVER_REFRESH_PROGRESS_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                COVER_REFRESH_CHANNEL_ID,
                "Cover refresh",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress while refreshing library cover images"
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<CoverRefreshWorker>().build(),
            )
        }
    }
}
