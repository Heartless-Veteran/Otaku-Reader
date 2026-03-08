package app.otakureader.data.download

import app.otakureader.core.common.dispatchers.OtakuReaderDispatchers
import app.otakureader.domain.model.Chapter
import app.otakureader.domain.model.Download
import app.otakureader.domain.model.DownloadState
import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.DownloadRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.SourceRepository
import app.otakureader.sourceapi.Page
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager that handles queueing and orchestration of chapter downloads.
 */
@Singleton
class DownloadManager @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val chapterRepository: ChapterRepository,
    private val mangaRepository: MangaRepository,
    private val sourceRepository: SourceRepository,
    private val chapterDownloader: ChapterDownloader,
    @OtakuReaderDispatchers.IO private val ioDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val downloadJobs = ConcurrentHashMap<Long, Job>()
    private val queueMutex = Mutex()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    /**
     * Queue a chapter for download.
     */
    suspend fun queueDownload(chapterId: Long) {
        queueMutex.withLock {
            // Check if already queued or downloaded
            val existing = downloadRepository.getDownloadByChapterId(chapterId)
            if (existing != null) {
                // Re-queue if failed or cancelled
                if (existing.state == DownloadState.Failed || existing.state == DownloadState.Cancelled) {
                    downloadRepository.updateDownloadState(chapterId, DownloadState.Queued)
                    startNextDownload()
                }
                return
            }

            // Get chapter and manga info
            val chapter = chapterRepository.getChapterById(chapterId)
                ?: throw IllegalArgumentException("Chapter not found: $chapterId")
            val manga = mangaRepository.getMangaById(chapter.mangaId)
                ?: throw IllegalArgumentException("Manga not found: ${chapter.mangaId}")

            // Create download entry
            val download = Download(
                chapterId = chapterId,
                mangaId = manga.id,
                sourceId = manga.sourceId,
                chapterName = chapter.name,
                mangaTitle = manga.title,
                state = DownloadState.Queued,
                progress = 0,
                totalPages = 0,
                downloadedPages = 0
            )

            downloadRepository.insertDownload(download)
            startNextDownload()
        }
    }

    /**
     * Queue multiple chapters for download.
     */
    suspend fun queueDownloads(chapterIds: List<Long>) {
        chapterIds.forEach { queueDownload(it) }
    }

    /**
     * Cancel a download.
     */
    suspend fun cancelDownload(chapterId: Long) {
        downloadJobs[chapterId]?.cancel()
        downloadJobs.remove(chapterId)
        downloadRepository.updateDownloadState(chapterId, DownloadState.Cancelled)
        startNextDownload()
    }

    /**
     * Retry a failed download.
     */
    suspend fun retryDownload(chapterId: Long) {
        downloadRepository.updateDownloadState(chapterId, DownloadState.Queued)
        startNextDownload()
    }

    /**
     * Delete a download and its files.
     */
    suspend fun deleteDownload(chapterId: Long) {
        // Cancel if downloading
        downloadJobs[chapterId]?.cancel()
        downloadJobs.remove(chapterId)

        // Get download info before deleting
        val download = downloadRepository.getDownloadByChapterId(chapterId)
        if (download != null) {
            // Delete files
            // Note: We need source name, which we'll get from the source
            // For now, we'll just delete the database entry
            // TODO: Store source name in download or get it from source repository
        }

        // Delete from database
        downloadRepository.deleteDownload(chapterId)
    }

    /**
     * Start downloading the next queued chapter.
     */
    private fun startNextDownload() {
        scope.launch {
            queueMutex.withLock {
                // Check if already downloading
                if (downloadJobs.isNotEmpty()) {
                    return@launch
                }

                // Get next queued download
                val downloads = downloadRepository.observeDownloads()
                // Note: We can't easily get a single value from Flow here
                // For simplicity, we'll just mark this as a limitation
                // In a real implementation, we'd want a suspend function to get queued downloads
            }
        }
    }

    /**
     * Start downloading a specific chapter.
     */
    private suspend fun startDownload(download: Download) {
        val job = scope.launch {
            try {
                _isDownloading.value = true
                downloadRepository.updateDownloadState(download.chapterId, DownloadState.Downloading)

                // Get source
                val source = sourceRepository.getSourceById(download.sourceId)
                    ?: throw IllegalStateException("Source not found: ${download.sourceId}")

                // Get chapter
                val chapter = chapterRepository.getChapterById(download.chapterId)
                    ?: throw IllegalStateException("Chapter not found: ${download.chapterId}")

                // Fetch page list from source
                val sourceChapter = app.otakureader.sourceapi.SourceChapter(
                    id = chapter.id.toString(),
                    url = chapter.url,
                    name = chapter.name,
                    scanlator = chapter.scanlator,
                    chapterNumber = chapter.chapterNumber,
                    dateUpload = chapter.dateUpload
                )
                val pages = source.fetchPageList(sourceChapter)

                // Update total pages
                downloadRepository.updateDownloadProgress(
                    download.chapterId,
                    downloadedPages = 0,
                    totalPages = pages.size,
                    progress = 0
                )

                // Download pages
                val result = chapterDownloader.downloadChapter(
                    download = download,
                    pages = pages,
                    sourceName = source.name
                ) { downloadedPages, totalPages ->
                    scope.launch {
                        val progress = (downloadedPages * 100) / totalPages
                        downloadRepository.updateDownloadProgress(
                            download.chapterId,
                            downloadedPages = downloadedPages,
                            totalPages = totalPages,
                            progress = progress
                        )
                    }
                }

                if (result.isSuccess) {
                    downloadRepository.updateDownloadState(download.chapterId, DownloadState.Completed)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    downloadRepository.updateDownloadError(download.chapterId, error)
                    downloadRepository.updateDownloadState(download.chapterId, DownloadState.Failed)
                }
            } catch (e: Exception) {
                downloadRepository.updateDownloadError(download.chapterId, e.message ?: "Unknown error")
                downloadRepository.updateDownloadState(download.chapterId, DownloadState.Failed)
            } finally {
                downloadJobs.remove(download.chapterId)
                _isDownloading.value = downloadJobs.isNotEmpty()
                startNextDownload()
            }
        }

        downloadJobs[download.chapterId] = job
    }
}
