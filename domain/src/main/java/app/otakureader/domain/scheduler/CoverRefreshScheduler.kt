package app.otakureader.domain.scheduler

import android.content.Context

/** Schedules a one-shot background job that refreshes all library cover images. */
interface CoverRefreshScheduler {
    fun schedule(context: Context)
}
