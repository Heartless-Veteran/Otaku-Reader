package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga

/**
 * A basic interface for creating a source. It could be an online source, a local source, stub source, etc.
 */
interface Source {

    /**
     * ID for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    val lang: String
        get() = ""

    /**
     * Get the updated details for a manga.
     *
     * @since extensions-lib 1.4
     */
    suspend fun getMangaDetails(manga: SManga): SManga

    /**
     * Get all the available chapters for a manga.
     *
     * @since extensions-lib 1.4
     */
    suspend fun getChapterList(manga: SManga): List<SChapter>

    /**
     * Get the list of pages a chapter has.
     *
     * @since komikku/extensions-lib 1.7
     */
    suspend fun getPageList(chapter: SChapter): List<Page>

    // KMK -->

    /**
     * Get all the available related mangas for a manga.
     *
     * @since komikku/extensions-lib 1.6
     */
    suspend fun getRelatedMangaList(
        manga: SManga,
        exceptionHandler: (Throwable) -> Unit,
        pushResults: suspend (relatedManga: Pair<String, List<SManga>>, completed: Boolean) -> Unit,
    )
    // KMK <--
}
