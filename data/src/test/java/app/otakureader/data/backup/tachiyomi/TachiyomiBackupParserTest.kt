package app.otakureader.data.backup.tachiyomi

import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class TachiyomiBackupParserTest {

    private val parser = TachiyomiBackupParser()

    private fun gzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    @Test
    fun `parses gzipped protobuf backup with categories, chapters and tracking`() {
        val backup = MihonBackup(
            backupManga = listOf(
                MihonBackupManga(
                    source = 1L,
                    url = "/manga/1",
                    title = "Test Manga",
                    genre = listOf("Action", "Comedy"),
                    favorite = true,
                    categories = listOf(0L),
                    chapters = listOf(
                        MihonBackupChapter(url = "/c/1", name = "Ch. 1", read = true, lastPageRead = 5),
                        MihonBackupChapter(url = "/c/2", name = "Ch. 2"),
                    ),
                    tracking = listOf(
                        MihonBackupTracking(syncId = 2, mediaId = 99L, title = "Tracked"),
                    ),
                ),
            ),
            backupCategories = listOf(MihonBackupCategory(name = "Reading", order = 0L)),
        )
        val raw = ProtoBuf.encodeToByteArray(MihonBackup.serializer(), backup)

        val parsed = parser.parse(gzip(raw))

        assertEquals(1, parsed.manga.size)
        assertEquals(1, parsed.categories.size)
        assertEquals(2, parsed.chapterCount)
        assertEquals(1, parsed.trackingCount)
        val manga = parsed.manga.first()
        assertEquals("Test Manga", manga.title)
        assertEquals(listOf(0L), manga.categoryOrders)
        assertEquals(5, manga.chapters.first().lastPageRead)
        // remoteId prefers the newer mediaId field (100) over libraryId (2).
        assertEquals(99L, manga.tracking.first().remoteId)
    }

    @Test
    fun `parses uncompressed protobuf backup`() {
        val backup = MihonBackup(
            backupManga = listOf(MihonBackupManga(source = 7L, url = "/x", title = "Plain")),
        )
        val raw = ProtoBuf.encodeToByteArray(MihonBackup.serializer(), backup)

        val parsed = parser.parse(raw)

        assertEquals(1, parsed.manga.size)
        assertEquals("Plain", parsed.manga.first().title)
    }

    @Test
    fun `parses legacy JSON backup`() {
        val jsonText = """
            {
              "version": 2,
              "manga": [{ "source": 3, "url": "/j", "title": "Json Manga", "genre": ["Drama"] }],
              "categories": [{ "name": "Default", "order": 0 }],
              "chapters": [{ "mangaId": 0, "url": "/jc", "name": "Ch" }]
            }
        """.trimIndent()

        val parsed = parser.parse(jsonText.toByteArray())

        assertEquals(1, parsed.manga.size)
        assertEquals("Json Manga", parsed.manga.first().title)
        assertEquals(1, parsed.categories.size)
    }
}
