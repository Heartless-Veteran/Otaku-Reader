package eu.kanade.tachiyomi.extension.en.test1

import eu.kanade.tachiyomi.extension.test.StubCatalogueSource

class Source1 : StubCatalogueSource() {
    override val id: Long = 1011L
    override val name: String = "Test Extension 1 Source"
}
