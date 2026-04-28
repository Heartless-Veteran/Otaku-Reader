package app.otakureader.domain.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChapterTest {

    @Test
    fun `create chapter with default read status`() {
        val chapter = Chapter(
            id = 1L,
            mangaId = 2L,
            name = "Chapter 1",
            chapterNumber = 1.0,
            url = "https://example.com/ch/1",
        )

        assertEquals(1L, chapter.id)
        assertEquals(2L, chapter.mangaId)
        assertEquals("Chapter 1", chapter.name)
        assertEquals(1.0, chapter.chapterNumber)
        assertFalse(chapter.read) // Default should be false
        assertFalse(chapter.bookmark) // Default should be false
    }

    @Test
    fun `mark chapter as read`() {
        val chapter = Chapter(id = 1L, mangaId = 2L, name = "Ch 1", read = false)
        val readChapter = chapter.copy(read = true, lastPageRead = 10)

        assertTrue(readChapter.read)
        assertEquals(10, readChapter.lastPageRead)
    }

    @Test
    fun `chapter sort order by source order`() {
        val ch1 = Chapter(id = 1L, mangaId = 1L, sourceOrder = 1)
        val ch2 = Chapter(id = 2L, mangaId = 1L, sourceOrder = 2)
        val ch3 = Chapter(id = 3L, mangaId = 1L, sourceOrder = 0)

        val sorted = listOf(ch1, ch2, ch3).sortedBy { it.sourceOrder }
        assertEquals(ch3, sorted[0])
        assertEquals(ch1, sorted[1])
        assertEquals(ch2, sorted[2])
    }
}
