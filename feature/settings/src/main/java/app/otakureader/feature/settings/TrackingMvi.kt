@file:Suppress("MatchingDeclarationName")

package app.otakureader.feature.settings

data class TrackingSettingsState(
    val trackers: List<TrackerInfo> = emptyList(),
    val trackingLoginInProgress: Boolean = false,
    val batchSyncInProgress: Boolean = false,
    val batchSyncSummary: app.otakureader.domain.repository.TrackerSyncRepository.SyncSummary? = null,
)
