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
    // Smart download rules
    val smartDownloadEnabled: Boolean = false,
    val smartDownloadChaptersAhead: Int = 3,
    val smartDownloadThreshold: Float = 0.8f,
    val smartDownloadWifiOnly: Boolean = true,
    val smartDownloadFavoritesOnly: Boolean = true,
    val smartDownloadMinStorageMb: Int = 500,
    // Data Saver
    val downloadDataSaverEnabled: Boolean = false,
)
