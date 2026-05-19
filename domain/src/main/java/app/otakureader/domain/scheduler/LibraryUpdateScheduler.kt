package app.otakureader.domain.scheduler

interface LibraryUpdateScheduler {
    fun schedule(intervalHours: Int, wifiOnly: Boolean)
    fun cancel()
    fun enqueueNow()
}
