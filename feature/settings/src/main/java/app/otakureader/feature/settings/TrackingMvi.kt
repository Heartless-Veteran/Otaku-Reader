package app.otakureader.feature.settings

data class TrackingSettingsState(
    val trackers: List<TrackerInfo> = emptyList(),
    val trackingLoginInProgress: Boolean = false,
)
