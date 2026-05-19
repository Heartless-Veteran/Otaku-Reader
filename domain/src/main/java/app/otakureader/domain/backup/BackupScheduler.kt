package app.otakureader.domain.backup

interface BackupScheduler {
    fun schedule(intervalHours: Int)
    fun cancel()
}
