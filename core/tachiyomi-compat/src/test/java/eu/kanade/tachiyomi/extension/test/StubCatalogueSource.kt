package eu.kanade.tachiyomi.extension.test

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import rx.Observable

/** Minimal CatalogueSource implementation for use in unit tests only. */
abstract class StubCatalogueSource : CatalogueSource {
    override val lang: String = "en"
    override val supportsLatest: Boolean = false

    override fun fetchPopularManga(page: Int): Observable<MangasPage> =
        Observable.empty<MangasPage>()

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        Observable.empty<MangasPage>()

    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> =
        Observable.empty<MangasPage>()

    override fun getFilterList(): FilterList = FilterList()

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        Observable.empty<List<Page>>()

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        Observable.empty<SManga>()

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        Observable.empty<List<SChapter>>()
}
