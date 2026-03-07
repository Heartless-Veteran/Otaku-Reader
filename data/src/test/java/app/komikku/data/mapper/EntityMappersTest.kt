package app.komikku.data.mapper

import app.komikku.domain.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {

    @Test
    fun `Chapter toEntity maps all fields correctly`() {
        val chapter = Chapter(
            id = 1L,
            mangaId = 2L,
            url = "http://example.com/chapter/1",
            name = "Chapter 1",
            scanlator = "ScanlatorGroup",
            dateUpload = 1620000000000L,
            chapterNumber = 1.5f,
            sourceOrder = 5,
            read = true,
            bookmark = true,
            lastPageRead = 10,
            totalPageCount = 20,
            dateFetch = 1620000000001L
        )

        val entity = chapter.toEntity()

        assertEquals(chapter.id, entity.id)
        assertEquals(chapter.mangaId, entity.mangaId)
        assertEquals(chapter.url, entity.url)
        assertEquals(chapter.name, entity.name)
        assertEquals(chapter.scanlator, entity.scanlator)
        assertEquals(chapter.dateUpload, entity.dateUpload)
        assertEquals(chapter.chapterNumber, entity.chapterNumber)
        assertEquals(chapter.sourceOrder, entity.sourceOrder)
        assertEquals(chapter.read, entity.read)
        assertEquals(chapter.bookmark, entity.bookmark)
        assertEquals(chapter.lastPageRead, entity.lastPageRead)
        assertEquals(chapter.totalPageCount, entity.totalPageCount)
        assertEquals(chapter.dateFetch, entity.dateFetch)
    }

    @Test
    fun `Chapter toEntity maps correctly with default values`() {
        val chapter = Chapter(
            mangaId = 3L,
            url = "/chapter/2",
            name = "Chapter 2"
        )

        val entity = chapter.toEntity()

        assertEquals(0L, entity.id)
        assertEquals(3L, entity.mangaId)
        assertEquals("/chapter/2", entity.url)
        assertEquals("Chapter 2", entity.name)
        assertEquals(null, entity.scanlator)
        assertEquals(0L, entity.dateUpload)
        assertEquals(-1f, entity.chapterNumber)
        assertEquals(0, entity.sourceOrder)
        assertEquals(false, entity.read)
        assertEquals(false, entity.bookmark)
        assertEquals(0, entity.lastPageRead)
        assertEquals(0, entity.totalPageCount)
        assertEquals(0L, entity.dateFetch)
    }
}
