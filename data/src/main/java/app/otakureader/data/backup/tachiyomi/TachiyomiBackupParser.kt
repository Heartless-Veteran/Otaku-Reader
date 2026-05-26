package app.otakureader.data.backup.tachiyomi

import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.zip.GZIPInputStream
import javax.inject.Inject

/** Source/format-agnostic representation of a Tachiyomi/Mihon backup the importer can consume. */
internal data class ParsedBackup(
    val manga: List<ParsedManga>,
    val categories: List<ParsedCategory>,
) {
    val chapterCount: Int get() = manga.sumOf { it.chapters.size }
    val trackingCount: Int get() = manga.sumOf { it.tracking.size }
}

internal data class ParsedManga(
    val source: Long,
    val url: String,
    val title: String,
    val artist: String?,
    val author: String?,
    val description: String?,
    val genre: List<String>,
    val status: Int,
    val thumbnailUrl: String?,
    val favorite: Boolean,
    val dateAdded: Long,
    val lastUpdate: Long,
    val chapterFlags: Int,
    val viewerFlags: Int,
    /** Category `order` values this manga belongs to (empty for legacy JSON backups). */
    val categoryOrders: List<Long>,
    val chapters: List<ParsedChapter>,
    val tracking: List<ParsedTracking>,
)

internal data class ParsedChapter(
    val url: String,
    val name: String,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Int,
    val chapterNumber: Float?,
    val sourceOrder: Int,
    val dateFetch: Long,
    val dateUpload: Long,
)

internal data class ParsedCategory(
    val name: String,
    val order: Int,
    val flags: Long,
)

internal data class ParsedTracking(
    val trackerId: Int,
    val remoteId: Long,
    val title: String?,
    val lastChapterRead: Float,
    val totalChapters: Int,
    val score: Float,
    val status: Int,
    val startDate: Long,
    val finishDate: Long,
    val url: String,
)

/**
 * Parses raw backup bytes into a [ParsedBackup], transparently handling:
 *  - gzip compression (`.tachibk` / `.proto.gz`),
 *  - protobuf payloads (modern Mihon/Tachiyomi), and
 *  - legacy JSON payloads (older exports / Otaku Reader's own Tachiyomi JSON).
 */
internal class TachiyomiBackupParser @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun parse(data: ByteArray): ParsedBackup {
        val raw = if (isGzip(data)) gunzip(data) else data
        return if (looksLikeJson(raw)) parseJson(raw.decodeToString()) else parseProto(raw)
    }

    private fun isGzip(data: ByteArray): Boolean =
        data.size >= 2 && data[0] == GZIP_MAGIC_0 && data[1] == GZIP_MAGIC_1

    private fun gunzip(data: ByteArray): ByteArray =
        GZIPInputStream(data.inputStream()).use { it.readBytes() }

    private fun looksLikeJson(data: ByteArray): Boolean {
        val first = data.firstOrNull { !it.toInt().toChar().isWhitespace() } ?: return false
        return first.toInt().toChar() == '{'
    }

    private fun parseProto(data: ByteArray): ParsedBackup {
        val backup = ProtoBuf.decodeFromByteArray(MihonBackup.serializer(), data)
        val categories = backup.backupCategories.map {
            ParsedCategory(name = it.name, order = it.order.toInt(), flags = it.flags)
        }
        val manga = backup.backupManga.map { m ->
            ParsedManga(
                source = m.source,
                url = m.url,
                title = m.title,
                artist = m.artist,
                author = m.author,
                description = m.description,
                genre = m.genre,
                status = m.status,
                thumbnailUrl = m.thumbnailUrl,
                favorite = m.favorite,
                dateAdded = m.dateAdded,
                lastUpdate = m.lastModifiedAt,
                chapterFlags = m.chapterFlags,
                viewerFlags = m.viewerFlags,
                categoryOrders = m.categories,
                chapters = m.chapters.map { c ->
                    ParsedChapter(
                        url = c.url,
                        name = c.name,
                        read = c.read,
                        bookmark = c.bookmark,
                        lastPageRead = c.lastPageRead.toInt(),
                        chapterNumber = c.chapterNumber,
                        sourceOrder = c.sourceOrder.toInt(),
                        dateFetch = c.dateFetch,
                        dateUpload = c.dateUpload,
                    )
                },
                tracking = m.tracking.map { t ->
                    ParsedTracking(
                        trackerId = t.syncId,
                        remoteId = t.remoteId,
                        title = t.title.ifBlank { null },
                        lastChapterRead = t.lastChapterRead,
                        totalChapters = t.totalChapters,
                        score = t.score,
                        status = t.status,
                        startDate = t.startedReadingDate,
                        finishDate = t.finishedReadingDate,
                        url = t.trackingUrl,
                    )
                },
            )
        }
        return ParsedBackup(manga = manga, categories = categories)
    }

    private fun parseJson(text: String): ParsedBackup {
        val backup = json.decodeFromString<TachiyomiBackup>(text)
        val chaptersByManga = backup.chapters.groupBy { it.mangaId }
        val trackingByManga = backup.tracking.groupBy { it.mangaId }
        val categories = backup.categories.map {
            ParsedCategory(name = it.name, order = it.order, flags = it.flags)
        }
        val manga = backup.manga.map { m ->
            ParsedManga(
                source = m.source,
                url = m.url,
                title = m.title,
                artist = m.artist,
                author = m.author,
                description = m.description,
                genre = m.genre,
                status = m.status,
                thumbnailUrl = m.thumbnailUrl,
                favorite = m.favorite,
                dateAdded = m.dateAdded,
                lastUpdate = m.lastUpdate,
                chapterFlags = m.chapterFlags,
                viewerFlags = m.viewerFlags,
                // Legacy JSON backups do not carry per-manga category membership.
                categoryOrders = emptyList(),
                chapters = (chaptersByManga[m.id] ?: emptyList()).map { c ->
                    ParsedChapter(
                        url = c.url,
                        name = c.name,
                        read = c.read,
                        bookmark = c.bookmark,
                        lastPageRead = c.lastPageRead,
                        chapterNumber = c.chapterNumber,
                        sourceOrder = c.sourceOrder,
                        dateFetch = c.dateFetch,
                        dateUpload = c.dateUpload,
                    )
                },
                tracking = (trackingByManga[m.id] ?: emptyList()).map { t ->
                    ParsedTracking(
                        trackerId = t.syncId,
                        remoteId = t.remoteId,
                        title = t.title,
                        lastChapterRead = (t.lastChapterRead ?: 0).toFloat(),
                        totalChapters = t.totalChapters ?: 0,
                        score = t.score ?: 0f,
                        status = t.status ?: 0,
                        startDate = t.startedReadingDate,
                        finishDate = t.finishedReadingDate,
                        url = t.trackingUrl ?: "",
                    )
                },
            )
        }
        return ParsedBackup(manga = manga, categories = categories)
    }

    private companion object {
        const val GZIP_MAGIC_0 = 0x1f.toByte()
        const val GZIP_MAGIC_1 = 0x8b.toByte()
    }
}
