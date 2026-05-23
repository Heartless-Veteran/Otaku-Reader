package app.otakureader.domain.scheduler

/** Schedules a one-shot background job that refreshes all library cover images. */
interface CoverRefreshScheduler {
    fun schedule()
}
