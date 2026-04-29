package app.otakureader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterTest {

    @Test
    fun `create chapter with default read status`() {
        val chapter = Chapter(
            id = 1L,
            mangaId = 2L,
            url = "https://example.com/ch/1",
            name = "Chapter 1",
            chapterNumber = 1f,
        )

        assertEquals(1L, chapter.id)
        assertEquals(2L, chapter.mangaId)
        assertEquals("Chapter 1", chapter.name)
        assertEquals(1f, chapter.chapterNumber)
        assertFalse(chapter.read) // Default should be false
        assertFalse(chapter.bookmark) // Default should be false
    }

    @Test
    fun `mark chapter as read`() {
        val chapter = Chapter(id = 1L, mangaId = 2L, url = "/c/1", name = "Ch 1", read = false)
        val readChapter = chapter.copy(read = true, lastPageRead = 10)

        assertTrue(readChapter.read)
        assertEquals(10, readChapter.lastPageRead)
    }

    @Test
    fun `chapter sort order by chapter number`() {
        val ch1 = Chapter(id = 1L, mangaId = 1L, url = "/c/1", name = "Ch 1", chapterNumber = 1f)
        val ch2 = Chapter(id = 2L, mangaId = 1L, url = "/c/2", name = "Ch 2", chapterNumber = 2f)
        val ch3 = Chapter(id = 3L, mangaId = 1L, url = "/c/3", name = "Ch 0", chapterNumber = 0f)

        val sorted = listOf(ch1, ch2, ch3).sortedBy { it.chapterNumber }
        assertEquals(ch3, sorted[0])
        assertEquals(ch1, sorted[1])
        assertEquals(ch2, sorted[2])
    }
}
