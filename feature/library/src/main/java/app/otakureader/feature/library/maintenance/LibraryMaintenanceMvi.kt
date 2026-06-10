package app.otakureader.feature.library.maintenance

data class LibraryMaintenanceState(
    val coverRefreshRunning: Boolean = false,
    val coverRefreshResult: String? = null,
    val metadataRefreshRunning: Boolean = false,
    val metadataRefreshResult: String? = null,
    val reindexRunning: Boolean = false,
    val reindexResult: String? = null,
    val orphanScanRunning: Boolean = false,
    val orphanScanResult: String? = null,
    val orphanCount: Int = 0,
)

sealed interface LibraryMaintenanceEvent {
    data object RefreshCovers : LibraryMaintenanceEvent
    data object RefreshMetadata : LibraryMaintenanceEvent
    data object ReindexDownloads : LibraryMaintenanceEvent
    data object ScanOrphans : LibraryMaintenanceEvent
    data object DeleteOrphans : LibraryMaintenanceEvent
}

sealed interface LibraryMaintenanceEffect {
    data class ShowSnackbar(val message: String) : LibraryMaintenanceEffect
}
