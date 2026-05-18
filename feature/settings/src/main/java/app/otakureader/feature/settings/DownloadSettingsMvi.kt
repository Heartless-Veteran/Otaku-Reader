@file:Suppress("MatchingDeclarationName")

package app.otakureader.feature.settings

data class DownloadSettingsState(
    val deleteAfterReading: Boolean = false,
    val saveAsCbz: Boolean = false,
    val autoDownloadEnabled: Boolean = false,
    val downloadOnlyOnWifi: Boolean = true,
    val autoDownloadLimit: Int = 3,
    val concurrentDownloads: Int = 2,
    val downloadAheadWhileReading: Int = 0,
    val downloadAheadOnlyOnWifi: Boolean = true,
    val downloadLocation: String? = null,
)
