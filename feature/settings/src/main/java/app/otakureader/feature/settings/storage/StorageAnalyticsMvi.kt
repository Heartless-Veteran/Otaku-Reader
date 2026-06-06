package app.otakureader.feature.settings.storage

data class SourceStorageEntry(
    val sourceName: String,
    val totalBytes: Long,
    val manga: List<MangaStorageEntry>,
)

data class MangaStorageEntry(
    val sourceName: String,
    val title: String,
    val totalBytes: Long,
)

data class StorageAnalyticsState(
    val isLoading: Boolean = true,
    val sources: List<SourceStorageEntry> = emptyList(),
    val totalBytes: Long = 0L,
    val expandedSources: Set<String> = emptySet(),
)

sealed interface StorageAnalyticsEvent {
    data class ToggleSource(val sourceName: String) : StorageAnalyticsEvent
    data class DeleteMangaDownloads(val sourceName: String, val mangaTitle: String) : StorageAnalyticsEvent
    data object Refresh : StorageAnalyticsEvent
}
