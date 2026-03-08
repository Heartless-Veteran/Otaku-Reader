package app.otakureader.data.download

import app.otakureader.core.common.dispatchers.OtakuReaderDispatchers
import app.otakureader.core.database.dao.DownloadDao
import app.otakureader.core.database.entity.DownloadPageEntity
import app.otakureader.domain.model.Download
import app.otakureader.domain.model.DownloadState
import app.otakureader.domain.repository.DownloadRepository
import app.otakureader.sourceapi.Page
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that downloads chapter pages using OkHttp.
 */
@Singleton
class ChapterDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadDao,
    private val storageProvider: DownloadStorageProvider,
    @OtakuReaderDispatchers.IO private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Download all pages for a chapter.
     */
    suspend fun downloadChapter(
        download: Download,
        pages: List<Page>,
        sourceName: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Save pages to database
            val pageEntities = pages.map { page ->
                DownloadPageEntity(
                    chapterId = download.chapterId,
                    pageIndex = page.index,
                    imageUrl = page.imageUrl ?: page.url,
                    localPath = null,
                    downloaded = false
                )
            }
            downloadDao.insertPages(pageEntities)

            // Download each page
            var downloadedCount = 0
            pages.forEachIndexed { index, page ->
                val imageUrl = page.imageUrl ?: page.url
                if (imageUrl.isEmpty()) {
                    throw IllegalStateException("Page $index has no image URL")
                }

                val extension = getExtensionFromUrl(imageUrl)
                val pageFile = storageProvider.getPageFile(
                    sourceId = download.sourceId,
                    sourceName = sourceName,
                    mangaId = download.mangaId,
                    mangaTitle = download.mangaTitle,
                    chapterId = download.chapterId,
                    chapterName = download.chapterName,
                    pageIndex = page.index,
                    extension = extension
                )

                // Download the page
                downloadPage(imageUrl, pageFile)

                // Update database
                val pageEntity = pageEntities[index]
                downloadDao.updatePageLocalPath(pageEntity.id, pageFile.absolutePath)

                // Update progress
                downloadedCount++
                val progress = (downloadedCount * 100) / pages.size
                onProgress(downloadedCount, pages.size)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download a single page to a file.
     */
    private suspend fun downloadPage(url: String, destination: File) = withContext(ioDispatcher) {
        val request = Request.Builder()
            .url(url)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download page: ${response.code}")
            }

            response.body?.byteStream()?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            } ?: throw Exception("Response body is null")
        }
    }

    /**
     * Get file extension from URL.
     */
    private fun getExtensionFromUrl(url: String): String {
        return when {
            url.contains(".jpg", ignoreCase = true) -> "jpg"
            url.contains(".jpeg", ignoreCase = true) -> "jpeg"
            url.contains(".png", ignoreCase = true) -> "png"
            url.contains(".webp", ignoreCase = true) -> "webp"
            url.contains(".gif", ignoreCase = true) -> "gif"
            else -> "jpg" // Default to jpg
        }
    }
}
