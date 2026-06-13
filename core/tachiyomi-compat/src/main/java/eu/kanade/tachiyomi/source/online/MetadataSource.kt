package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.CatalogueSource
import kotlin.reflect.KClass

/**
 * SIMPLIFIED port of the EXH `MetadataSource`.
 *
 * The upstream version is wired into the SY/EXH flat-metadata database (RaisedSearchMetadata,
 * FlatMetadata, Injekt-provided DAOs). Otaku Reader does not port that subsystem, so this keeps
 * only the generic parse surface needed for the type to exist on the host classpath. The DB-backed
 * helpers (parseToManga / fetchOrLoadMetadata) are intentionally omitted.
 */
interface MetadataSource<M : Any, I> : CatalogueSource {

    /**
     * The class of the metadata used by this source.
     */
    val metaClass: KClass<M>

    /**
     * Parse the supplied input into the supplied metadata object.
     */
    suspend fun parseIntoMetadata(metadata: M, input: I)

    /**
     * Use reflection to create a new instance of metadata.
     */
    fun newMetaInstance(): M
}
