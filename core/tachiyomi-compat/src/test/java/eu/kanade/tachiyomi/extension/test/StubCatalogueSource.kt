package eu.kanade.tachiyomi.extension.test

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga

/**
 * Minimal CatalogueSource implementation for use in unit tests only.
 *
 * Mirrors the current Tachiyomi/Komikku suspend-based CatalogueSource contract; all calls
 * return empty results. Subclasses only need to override [id] and [name].
 */
abstract class StubCatalogueSource : CatalogueSource {
    override val lang: String = "en"
    override val supportsLatest: Boolean = false

    // Test-only convenience: real HttpSource exposes baseUrl, but this stub implements the
    // plain CatalogueSource contract, so declare it here for subclasses (e.g. MangaDex) to set.
    open val baseUrl: String = ""

    override suspend fun getPopularManga(page: Int): MangasPage = MangasPage(emptyList(), false)

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        MangasPage(emptyList(), false)

    override suspend fun getLatestUpdates(page: Int): MangasPage = MangasPage(emptyList(), false)

    override fun getFilterList(): FilterList = FilterList()

    override suspend fun getMangaDetails(manga: SManga): SManga = manga

    override suspend fun getChapterList(manga: SManga): List<SChapter> = emptyList()

    override suspend fun getPageList(chapter: SChapter): List<Page> = emptyList()
}
