package app.otakureader.domain.updater

import app.otakureader.domain.model.AppVersionInfo

interface AppUpdateChecker {
    suspend fun checkForUpdate(): AppVersionInfo?
}
