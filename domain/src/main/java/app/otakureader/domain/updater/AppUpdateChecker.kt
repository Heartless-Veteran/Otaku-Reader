package app.otakureader.domain.updater

interface AppUpdateChecker {
    suspend fun checkForUpdate(): AppVersionInfo?
}
