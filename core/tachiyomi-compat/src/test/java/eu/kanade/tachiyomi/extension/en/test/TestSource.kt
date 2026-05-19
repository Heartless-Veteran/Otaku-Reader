package eu.kanade.tachiyomi.extension.en.test

import eu.kanade.tachiyomi.extension.test.StubCatalogueSource

class TestSource : StubCatalogueSource() {
    override val id: Long = 1001L
    override val name: String = "Test Source"
}
