package app.otakureader.domain.scheduler

/**
 * Schedules (or cancels) periodic background checks for extension updates.
 *
 * Lives in the domain layer so feature modules can react to preference changes without
 * depending on the data/WorkManager implementation.
 */
interface ExtensionUpdateScheduler {
    fun schedule(intervalHours: Int = 24, wifiOnly: Boolean = true)
    fun cancel()
}
