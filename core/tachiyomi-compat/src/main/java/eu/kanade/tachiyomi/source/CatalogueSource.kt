package eu.kanade.tachiyomi.source

import android.util.Log
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * A basic interface for creating a catalogue source. It could be an online source, a local source, etc.
 */
interface CatalogueSource : Source {

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    override val lang: String

    /**
     * Whether the source has support for latest updates.
     */
    val supportsLatest: Boolean

    /**
     * Get a page with a list of manga.
     *
     * @since extensions-lib 1.5
     */
    suspend fun getPopularManga(page: Int): MangasPage

    /**
     * Get a page with a list of manga.
     *
     * @since extensions-lib 1.5
     */
    suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage

    /**
     * Get a page with a list of latest manga updates.
     *
     * @since extensions-lib 1.5
     */
    suspend fun getLatestUpdates(page: Int): MangasPage

    /**
     * Returns the list of filters for the source.
     */
    fun getFilterList(): FilterList

    // KMK -->

    /**
     * Whether parsing related mangas in manga page or extension provide custom related mangas request.
     * @since komikku/extensions-lib 1.6
     */
    val supportsRelatedMangas: Boolean get() = false

    /**
     * Extensions doesn't want to use App's [getRelatedMangaListBySearch].
     * @since komikku/extensions-lib 1.6
     */
    val disableRelatedMangasBySearch: Boolean get() = false

    /**
     * Disable showing any related mangas.
     * @since komikku/extensions-lib 1.6
     */
    val disableRelatedMangas: Boolean get() = false

    /**
     * Get all the available related mangas for a manga.
     * Normally it's not needed to override this method.
     *
     * @since komikku/extensions-lib 1.6
     */
    override suspend fun getRelatedMangaList(
        manga: SManga,
        exceptionHandler: (Throwable) -> Unit,
        pushResults: suspend (relatedManga: Pair<String, List<SManga>>, completed: Boolean) -> Unit,
    ) {
        val handler = CoroutineExceptionHandler { _, e -> exceptionHandler(e) }
        if (!disableRelatedMangas) {
            supervisorScope {
                if (supportsRelatedMangas) launch(handler) { getRelatedMangaListByExtension(manga, pushResults) }
                if (!disableRelatedMangasBySearch) launch(handler) { getRelatedMangaListBySearch(manga, pushResults) }
            }
        }
    }

    /**
     * Get related mangas provided by extension
     *
     * @since komikku/extensions-lib 1.6
     */
    suspend fun getRelatedMangaListByExtension(
        manga: SManga,
        pushResults: suspend (relatedManga: Pair<String, List<SManga>>, completed: Boolean) -> Unit,
    ) {
        runCatching { fetchRelatedMangaList(manga) }
            .onSuccess { if (it.isNotEmpty()) pushResults(Pair("", it), false) }
            .onFailure { e ->
                Log.e("CatalogueSource", "## getRelatedMangaListByExtension: $e", e)
            }
    }

    /**
     * Fetch related mangas for a manga from source/site.
     *
     * @since komikku/extensions-lib 1.6
     */
    suspend fun fetchRelatedMangaList(manga: SManga): List<SManga> = throw UnsupportedOperationException("Unsupported!")

    /**
     * Slit & strip manga's title into separate searchable keywords.
     * Used for searching related mangas.
     *
     * @since komikku/extensions-lib 1.6
     */
    fun String.stripKeywordForRelatedMangas(): List<String> {
        val regexWhitespace = Regex("\\s+")
        val regexSpecialCharacters =
            Regex("([!~#$%^&*+_|/\\\\,?:;'“”‘’\"<>(){}\\[\\]。・～：—！？、―«»《》〘〙【】「」｜]|\\s-|-\\s|\\s\\.|\\.\\s)")
        val regexNumberOnly = Regex("^\\d+$")

        return replace(regexSpecialCharacters, " ")
            .split(regexWhitespace)
            .map {
                // remove number only
                it.replace(regexNumberOnly, "")
                    .lowercase()
            }
            // exclude single character
            .filter { it.length > 1 }
    }

    /**
     * Get related mangas by searching for each keywords from manga's title.
     *
     * @since komikku/extensions-lib 1.6
     */
    suspend fun getRelatedMangaListBySearch(
        manga: SManga,
        pushResults: suspend (relatedManga: Pair<String, List<SManga>>, completed: Boolean) -> Unit,
    ) {
        val words = HashSet<String>()
        words.add(manga.title)
        if (manga.title.lowercase() != manga.originalTitle.lowercase()) words.add(manga.originalTitle)
        manga.title.stripKeywordForRelatedMangas()
            .filterNot { word -> words.any { it.lowercase() == word } }
            .onEach { words.add(it) }
        manga.originalTitle.stripKeywordForRelatedMangas()
            .filterNot { word -> words.any { it.lowercase() == word } }
            .onEach { words.add(it) }
        if (words.isEmpty()) return

        coroutineScope {
            val filterList = getFilterList()
            words.map { keyword ->
                launch {
                    runCatching {
                        // Simplified: no QuerySanitizer; use the trimmed keyword directly.
                        getSearchManga(1, keyword.trim(), filterList).mangas
                    }
                        .onSuccess { if (it.isNotEmpty()) pushResults(Pair(keyword, it), false) }
                        .onFailure { e ->
                            Log.e("CatalogueSource", "## getRelatedMangaListBySearch: $e", e)
                        }
                }
            }
        }
    }
    // KMK <--
}
