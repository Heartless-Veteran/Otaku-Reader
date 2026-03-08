package app.otakureader.data.download

import app.otakureader.core.database.dao.DownloadDao
import app.otakureader.domain.model.Chapter
import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.SourceRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides local page files for the reader when offline.
 */
@Singleton
class DownloadProvider @Inject constructor(
    private val downloadDao: DownloadDao,
    private val chapterRepository: ChapterRepository,
    private val mangaRepository: MangaRepository,
    private val sourceRepository: SourceRepository,
    private val storageProvider: DownloadStorageProvider
) {
    /**
     * Get local page file for a specific chapter and page index.
     * Returns null if the page is not downloaded.
     */
    suspend fun getLocalPage(chapterId: Long, pageIndex: Int): File? {
        // Check if chapter is downloaded
        if (!downloadDao.isChapterDownloaded(chapterId)) {
            return null
        }

        // Get page from database
        val pages = downloadDao.getPagesByChapterId(chapterId)
        val page = pages.find { it.pageIndex == pageIndex } ?: return null

        // Return local path if exists
        val localPath = page.localPath ?: return null
        val file = File(localPath)
        return if (file.exists()) file else null
    }

    /**
     * Get all local pages for a chapter.
     * Returns an empty list if the chapter is not downloaded.
     */
    suspend fun getLocalPages(chapterId: Long): List<File> {
        // Check if chapter is downloaded
        if (!downloadDao.isChapterDownloaded(chapterId)) {
            return emptyList()
        }

        // Get pages from database
        val pages = downloadDao.getPagesByChapterId(chapterId)
        return pages.mapNotNull { page ->
            page.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) file else null
            }
        }
    }

    /**
     * Check if a chapter has local pages available.
     */
    suspend fun hasLocalPages(chapterId: Long): Boolean {
        return downloadDao.isChapterDownloaded(chapterId)
    }

    /**
     * Get the local file path for a page, or null if not available.
     */
    suspend fun getLocalPagePath(chapterId: Long, pageIndex: Int): String? {
        val file = getLocalPage(chapterId, pageIndex)
        return file?.absolutePath
    }
}
