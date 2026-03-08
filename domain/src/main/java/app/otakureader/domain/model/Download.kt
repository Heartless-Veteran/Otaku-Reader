package app.otakureader.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing a chapter download.
 */
@Serializable
data class Download(
    val id: Long = 0,
    val chapterId: Long,
    val mangaId: Long,
    val sourceId: Long,
    val chapterName: String,
    val mangaTitle: String,
    val state: DownloadState = DownloadState.Queued,
    val progress: Int = 0, // 0-100
    val totalPages: Int = 0,
    val downloadedPages: Int = 0,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents the current state of a download.
 */
@Serializable
sealed class DownloadState {
    @Serializable
    data object Queued : DownloadState()

    @Serializable
    data object Downloading : DownloadState()

    @Serializable
    data object Completed : DownloadState()

    @Serializable
    data object Failed : DownloadState()

    @Serializable
    data object Paused : DownloadState()

    @Serializable
    data object Cancelled : DownloadState()
}
