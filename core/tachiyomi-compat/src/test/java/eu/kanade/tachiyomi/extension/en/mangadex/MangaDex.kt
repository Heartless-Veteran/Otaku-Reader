package eu.kanade.tachiyomi.extension.en.mangadex

import eu.kanade.tachiyomi.extension.test.StubCatalogueSource

class MangaDex : StubCatalogueSource() {
    override val id: Long = 2499283573021220255L
    override val name: String = "MangaDex"
    override val baseUrl: String = "https://mangadex.org"
}
