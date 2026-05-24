package app.otakureader.domain.scheduler

interface TrackerSyncScheduler {
    fun schedule(intervalHours: Int = 6)
    fun cancel()
}
