package eu.kanade.tachiyomi.extension.en.test

import eu.kanade.tachiyomi.extension.test.StubCatalogueSource

class Source : StubCatalogueSource() {
    override val id: Long = 1002L
    override val name: String = "Test Extension Source"
}
