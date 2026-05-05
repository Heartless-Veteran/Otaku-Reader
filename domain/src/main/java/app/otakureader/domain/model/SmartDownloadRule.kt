package app.otakureader.domain.model

import androidx.compose.runtime.Immutable

/**
 * Configuration for smart (auto) download behavior.
 *
 * When enabled, the app automatically queues downloads for upcoming chapters
 * based on reading progress — no manual intervention required.
 */
@Immutable
data class SmartDownloadRule(
    val enabled: Boolean = false,
    /** How many upcoming chapters to pre-download. */
    val chaptersAhead: Int = 3,
    /** Reading progress (0.0–1.0) at which to trigger the next batch. */
    val progressThreshold: Float = 0.8f,
    /** Only auto-download on unmetered Wi-Fi. */
    val wifiOnly: Boolean = true,
    /** Restrict auto-download to favorited manga only. */
    val favoritesOnly: Boolean = true,
    /** Minimum free storage (MB) required before downloading. */
    val minFreeStorageMb: Int = 500,
) {
    companion object {
        /** Default conservative rule: download next 3 chapters at 80% on Wi-Fi, favorites only. */
        val DEFAULT = SmartDownloadRule()

        /** Aggressive rule: download 5 chapters at 50%, any network, all manga. */
        val AGGRESSIVE = SmartDownloadRule(
            enabled = true,
            chaptersAhead = 5,
            progressThreshold = 0.5f,
            wifiOnly = false,
            favoritesOnly = false
        )

        /** Conservative rule: download 1 chapter at 90%, Wi-Fi, favorites only. */
        val CONSERVATIVE = SmartDownloadRule(
            enabled = true,
            chaptersAhead = 1,
            progressThreshold = 0.9f,
            wifiOnly = true,
            favoritesOnly = true
        )
    }
}
