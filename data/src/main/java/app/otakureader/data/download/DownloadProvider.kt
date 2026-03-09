package app.otakureader.data.download

import android.content.Context
import java.io.File

/**
 * Provides filesystem helpers for locally downloaded chapter pages.
 *
 * Directory layout (inside the app-specific external files directory):
 * ```
 * OtakuReader/
 *   {sourceName}/
 *     {mangaTitle}/
 *       {chapterName}/
 *         0.jpg
 *         1.jpg
 *         …
 *         chapter.cbz   ← optional CBZ archive
 * ```
 *
 * Using `Context.getExternalFilesDir` means no storage permission is required on
 * any supported Android version.
 *
 * The `Context`-based public API resolves the root directory from the given context.
 * Internal overloads that accept a root `File` directly are provided so that
 * pure-JVM unit tests can exercise the logic without needing an Android Context.
 */
object DownloadProvider {

    private const val ROOT_DIR = "OtakuReader"

    /** The file extensions recognised as downloaded page images. */
    private val PAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    // -------------------------------------------------------------------------
    // Context-based public API
    // -------------------------------------------------------------------------

    /**
     * Returns the directory that holds all pages for [chapterName].
     * The directory may not exist yet; callers are responsible for creating it.
     */
    fun getChapterDir(
        context: Context,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): File = getChapterDir(rootFor(context), sourceName, mangaTitle, chapterName)

    /**
     * Returns the [File] where the page at [pageIndex] should be (or is) stored.
     * Uses `.jpg` as the default extension regardless of the source URL.
     */
    fun getPageFile(
        context: Context,
        sourceName: String,
        mangaTitle: String,
        chapterName: String,
        pageIndex: Int
    ): File = getPageFile(rootFor(context), sourceName, mangaTitle, chapterName, pageIndex)

    /**
     * Returns the [File] path for the CBZ archive of [chapterName].
     * The file may not exist yet.
     */
    fun getCbzFile(
        context: Context,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): File = getCbzFile(rootFor(context), sourceName, mangaTitle, chapterName)

    /**
     * Returns `true` when the chapter directory exists and contains at least one page file
     * or a CBZ archive.
     */
    fun isChapterDownloaded(
        context: Context,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): Boolean = isChapterDownloaded(rootFor(context), sourceName, mangaTitle, chapterName)

    /**
     * Returns an ordered list of `file://` URIs for every page that has been
     * downloaded for the given chapter. Pages are sorted by their numeric filename.
     *
     * When the chapter was saved as a CBZ archive and no loose page files exist,
     * the archive is extracted into the chapter directory on demand and those file
     * URIs are returned.
     *
     * Returns an empty list if nothing has been downloaded yet.
     */
    fun getDownloadedPageUris(
        context: Context,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): List<String> = getDownloadedPageUris(rootFor(context), sourceName, mangaTitle, chapterName)

    /**
     * Deletes all downloaded files for the given chapter. Returns true if anything was removed.
     */
    fun deleteChapter(
        context: Context,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): Boolean = deleteChapter(rootFor(context), sourceName, mangaTitle, chapterName)

    // -------------------------------------------------------------------------
    // Internal root-File overloads (used for testing without a real Context)
    // -------------------------------------------------------------------------

    internal fun getChapterDir(
        root: File,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): File = File(
        root,
        "$ROOT_DIR/${sanitize(sourceName)}/${sanitize(mangaTitle)}/${sanitize(chapterName)}"
    )

    internal fun getPageFile(
        root: File,
        sourceName: String,
        mangaTitle: String,
        chapterName: String,
        pageIndex: Int
    ): File = File(getChapterDir(root, sourceName, mangaTitle, chapterName), "$pageIndex.jpg")

    internal fun getCbzFile(
        root: File,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): File = File(getChapterDir(root, sourceName, mangaTitle, chapterName), CbzCreator.CBZ_FILE_NAME)

    internal fun isChapterDownloaded(
        root: File,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): Boolean {
        val dir = getChapterDir(root, sourceName, mangaTitle, chapterName)
        if (!dir.isDirectory) return false
        return dir.listFiles()?.any {
            it.isFile && (it.extension.lowercase() in PAGE_EXTENSIONS || it.name == CbzCreator.CBZ_FILE_NAME)
        } == true
    }

    internal fun getDownloadedPageUris(
        root: File,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): List<String> {
        val dir = getChapterDir(root, sourceName, mangaTitle, chapterName)
        if (!dir.isDirectory) return emptyList()

        // Prefer loose page files when they exist (backward-compatible path).
        val looseFiles = dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in PAGE_EXTENSIONS }
            ?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
        if (!looseFiles.isNullOrEmpty()) {
            return looseFiles.map { "file://${it.absolutePath}" }
        }

        // Fall back to CBZ: extract pages on demand and return file:// URIs.
        val cbzFile = File(dir, CbzCreator.CBZ_FILE_NAME)
        if (cbzFile.exists()) {
            val extracted = CbzCreator.extractCbzPages(cbzFile, dir).getOrNull()
            if (!extracted.isNullOrEmpty()) {
                return extracted.map { "file://${it.absolutePath}" }
            }
        }

        return emptyList()
    }

    internal fun deleteChapter(
        root: File,
        sourceName: String,
        mangaTitle: String,
        chapterName: String
    ): Boolean {
        val dir = getChapterDir(root, sourceName, mangaTitle, chapterName)
        if (!dir.exists()) return false
        return dir.deleteRecursively()
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    /**
     * Replaces characters that are illegal in filesystem paths with underscores and
     * trims surrounding whitespace.
     */
    internal fun sanitize(name: String): String =
        name.replace(Regex("""[/\\:*?"<>|]"""), "_").trim()

    private fun rootFor(context: Context): File =
        context.getExternalFilesDir(null) ?: context.filesDir
}
