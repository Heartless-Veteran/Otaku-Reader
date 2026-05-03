package com.otakureader.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.delay

class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MANGA_ID = "manga_id"
        const val KEY_CHAPTER_NUM = "chapter_num"
        const val CHANNEL_ID = "downloads"

        fun enqueue(context: Context, mangaId: Int, chapterNum: Int) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    workDataOf(
                        KEY_MANGA_ID to mangaId,
                        KEY_CHAPTER_NUM to chapterNum,
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        val mangaId = inputData.getInt(KEY_MANGA_ID, -1)
        val chapterNum = inputData.getInt(KEY_CHAPTER_NUM, -1)

        if (mangaId < 0 || chapterNum < 0) return Result.failure()

        createNotificationChannel()
        val notifManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = mangaId * 1000 + chapterNum

        // Simulate download with progress updates
        for (progress in 0..100 step 10) {
            setProgress(workDataOf("progress" to progress))

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading Chapter $chapterNum")
                .setContentText("Manga #$mangaId")
                .setProgress(100, progress, false)
                .setOngoing(progress < 100)
                .setSilent(true)
                .build()

            notifManager.notify(notifId, notification)
            delay(500L)
        }

        // CBZ output stub — in production, write pages to zip archive
        val cbzPath = "${applicationContext.getExternalFilesDir(null)}/manga_${mangaId}/ch_${chapterNum}.cbz"

        notifManager.cancel(notifId)
        return Result.success(workDataOf("cbz_path" to cbzPath))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Manga chapter downloads" }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
