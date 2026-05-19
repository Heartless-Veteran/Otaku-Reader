package app.otakureader.data.worker

import android.content.Context
import app.otakureader.domain.scheduler.LibraryUpdateScheduler as LibraryUpdateSchedulerInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages scheduling of periodic library update work.
 */
@Singleton
class LibraryUpdateScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : LibraryUpdateSchedulerInterface {

    override fun schedule(intervalHours: Int, wifiOnly: Boolean) {
        LibraryUpdateWorker.schedule(
            context = context,
            intervalHours = intervalHours,
            wifiOnly = wifiOnly
        )
    }

    override fun cancel() {
        LibraryUpdateWorker.cancelPeriodic(context)
    }

    override fun enqueueNow() {
        LibraryUpdateWorker.enqueue(context)
    }
}
