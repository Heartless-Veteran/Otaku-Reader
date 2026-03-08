package app.otakureader.domain.repository

import app.otakureader.domain.model.Download
import app.otakureader.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing chapter downloads.
 */
interface DownloadRepository {
    /**
     * Observe all downloads.
     */
    fun observeDownloads(): Flow<List<Download>>

    /**
     * Observe downloads for a specific manga.
     */
    fun observeDownloadsByMangaId(mangaId: Long): Flow<List<Download>>

    /**
     * Get a download by chapter ID.
     */
    suspend fun getDownloadByChapterId(chapterId: Long): Download?

    /**
     * Insert or update a download.
     */
    suspend fun insertDownload(download: Download): Long

    /**
     * Update download state.
     */
    suspend fun updateDownloadState(chapterId: Long, state: DownloadState)

    /**
     * Update download progress.
     */
    suspend fun updateDownloadProgress(
        chapterId: Long,
        downloadedPages: Int,
        totalPages: Int,
        progress: Int
    )

    /**
     * Update download error.
     */
    suspend fun updateDownloadError(chapterId: Long, error: String)

    /**
     * Delete a download.
     */
    suspend fun deleteDownload(chapterId: Long)

    /**
     * Delete all downloads for a manga.
     */
    suspend fun deleteDownloadsByMangaId(mangaId: Long)

    /**
     * Check if a chapter is downloaded.
     */
    suspend fun isChapterDownloaded(chapterId: Long): Boolean
}
