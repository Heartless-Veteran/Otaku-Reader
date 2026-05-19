package app.otakureader.domain.scheduler

interface ReminderScheduler {
    fun schedule(hour: Int)
    fun cancel()
}
