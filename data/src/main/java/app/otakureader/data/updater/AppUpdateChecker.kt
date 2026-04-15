package app.otakureader.data.updater

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.otakureader.core.preferences.GeneralPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing the latest app version information.
 */
@Serializable
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val releaseDate: Long
)

/**
 * Worker that periodically checks for app updates from GitHub releases.
 */
class AppUpdateWorker(
    context: Context,
    params: WorkerParameters,
    private val generalPreferences: GeneralPreferences
) : Worker(context, params) {

    override fun doWork(): Result {
        // Check if update checking is enabled
        val isEnabled = runBlocking { generalPreferences.appUpdateCheckEnabled.first() }
        if (!isEnabled) {
            return Result.success()
        }

        return try {
            // TODO: Implement actual GitHub API check
            // For now, this is a placeholder that simulates checking
            // In production, this would:
            // 1. Query GitHub API for latest release
            // 2. Compare version code with current version
            // 3. Save version info if newer version available
            // 4. Show notification if update available

            runBlocking {
                generalPreferences.setLastAppUpdateCheck(System.currentTimeMillis())
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "app_update_check"

        /**
         * Schedule periodic app update checks.
         */
        fun schedule(context: Context, intervalHours: Int = 24) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(
                intervalHours.toLong(),
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
        }

        /**
         * Cancel periodic app update checks.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Run an immediate app update check.
         */
        fun checkNow(context: Context) {
            // TODO: Implement one-time work request for immediate check
        }
    }
}

/**
 * Singleton class to manage app update checking.
 */
@Singleton
class AppUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generalPreferences: GeneralPreferences
) {
    /**
     * Check if an update is available.
     * Returns the VersionInfo if an update is available, null otherwise.
     */
    suspend fun checkForUpdate(): VersionInfo? = withContext(Dispatchers.IO) {
        // TODO: Implement actual GitHub API call
        // 1. Get current version from preferences
        // 2. Query GitHub API for latest release
        // 3. Compare versions
        // 4. Return VersionInfo if newer, null if up to date
        null
    }

    /**
     * Schedule periodic update checks.
     */
    fun scheduleChecks(intervalHours: Int = 24) {
        AppUpdateWorker.schedule(context, intervalHours)
    }

    /**
     * Cancel periodic update checks.
     */
    fun cancelChecks() {
        AppUpdateWorker.cancel(context)
    }
}
