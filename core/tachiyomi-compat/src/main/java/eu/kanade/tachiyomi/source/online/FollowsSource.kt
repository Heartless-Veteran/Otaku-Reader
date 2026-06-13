package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga

/**
 * SIMPLIFIED port of the EXH `FollowsSource`.
 *
 * Upstream `fetchAllFollows()` returns `List<Pair<SManga, RaisedSearchMetadata>>`; since the
 * EXH metadata subsystem isn't ported, the metadata half is dropped here and a plain list of
 * [SManga] is returned instead.
 */
interface FollowsSource : CatalogueSource {
    suspend fun fetchFollows(page: Int): MangasPage

    /**
     * Returns a list of all Follows retrieved for the logged-in user.
     */
    suspend fun fetchAllFollows(): List<SManga>
}
