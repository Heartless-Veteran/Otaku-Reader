package app.otakureader.data.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles directory structure for chapter downloads.
 * Structure: /OtakuReader/{Source}/{Manga}/{Chapter}/
 */
@Singleton
class DownloadStorageProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Base directory for all downloads.
     * Uses filesDir for persistent storage.
     */
    private val baseDir: File by lazy {
        File(context.filesDir, "OtakuReader").apply { mkdirs() }
    }

    /**
     * Get the directory for a specific source.
     */
    fun getSourceDir(sourceId: Long, sourceName: String): File {
        val sanitizedName = sanitizeName(sourceName)
        return File(baseDir, "${sourceId}_$sanitizedName").apply { mkdirs() }
    }

    /**
     * Get the directory for a specific manga.
     */
    fun getMangaDir(sourceId: Long, sourceName: String, mangaId: Long, mangaTitle: String): File {
        val sourceDir = getSourceDir(sourceId, sourceName)
        val sanitizedTitle = sanitizeName(mangaTitle)
        return File(sourceDir, "${mangaId}_$sanitizedTitle").apply { mkdirs() }
    }

    /**
     * Get the directory for a specific chapter.
     */
    fun getChapterDir(
        sourceId: Long,
        sourceName: String,
        mangaId: Long,
        mangaTitle: String,
        chapterId: Long,
        chapterName: String
    ): File {
        val mangaDir = getMangaDir(sourceId, sourceName, mangaId, mangaTitle)
        val sanitizedName = sanitizeName(chapterName)
        return File(mangaDir, "${chapterId}_$sanitizedName").apply { mkdirs() }
    }

    /**
     * Get the file path for a specific page.
     */
    fun getPageFile(
        sourceId: Long,
        sourceName: String,
        mangaId: Long,
        mangaTitle: String,
        chapterId: Long,
        chapterName: String,
        pageIndex: Int,
        extension: String = "jpg"
    ): File {
        val chapterDir = getChapterDir(sourceId, sourceName, mangaId, mangaTitle, chapterId, chapterName)
        return File(chapterDir, "${pageIndex.toString().padStart(4, '0')}.$extension")
    }

    /**
     * Delete a chapter directory and all its contents.
     */
    fun deleteChapterDir(
        sourceId: Long,
        sourceName: String,
        mangaId: Long,
        mangaTitle: String,
        chapterId: Long,
        chapterName: String
    ): Boolean {
        val chapterDir = getChapterDir(sourceId, sourceName, mangaId, mangaTitle, chapterId, chapterName)
        return chapterDir.deleteRecursively()
    }

    /**
     * Delete a manga directory and all its contents.
     */
    fun deleteMangaDir(sourceId: Long, sourceName: String, mangaId: Long, mangaTitle: String): Boolean {
        val mangaDir = getMangaDir(sourceId, sourceName, mangaId, mangaTitle)
        return mangaDir.deleteRecursively()
    }

    /**
     * Get all downloaded pages in a chapter directory.
     */
    fun getDownloadedPages(
        sourceId: Long,
        sourceName: String,
        mangaId: Long,
        mangaTitle: String,
        chapterId: Long,
        chapterName: String
    ): List<File> {
        val chapterDir = getChapterDir(sourceId, sourceName, mangaId, mangaTitle, chapterId, chapterName)
        return chapterDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
    }

    /**
     * Check if a chapter is fully downloaded.
     */
    fun isChapterDownloaded(
        sourceId: Long,
        sourceName: String,
        mangaId: Long,
        mangaTitle: String,
        chapterId: Long,
        chapterName: String,
        expectedPageCount: Int
    ): Boolean {
        val pages = getDownloadedPages(sourceId, sourceName, mangaId, mangaTitle, chapterId, chapterName)
        return pages.size == expectedPageCount
    }

    /**
     * Sanitize a name for use in file paths.
     * Removes illegal characters and limits length.
     */
    private fun sanitizeName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            .take(100)
    }
}
