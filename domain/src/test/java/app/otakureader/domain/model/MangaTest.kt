package app.otakureader.domain.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MangaTest {

    @Test
    fun `create manga with default values`() {
        val manga = Manga(
            id = 1L,
            title = "Test Manga",
            description = "A test description",
            coverLastModified = 123456789L,
            source = 1L,
            url = "https://example.com/manga/1",
        )

        assertEquals(1L, manga.id)
        assertEquals("Test Manga", manga.title)
        assertEquals("A test description", manga.description)
        assertEquals(false, manga.favorite)
        assertEquals(0, manga.fetchInterval)
    }

    @Test
    fun `manga equality based on id`() {
        val manga1 = Manga(id = 1L, title = "A", source = 1L)
        val manga2 = Manga(id = 1L, title = "B", source = 2L)
        val manga3 = Manga(id = 2L, title = "A", source = 1L)

        assertEquals(manga1, manga2) // Same ID = equal
        assertNotEquals(manga1, manga3) // Different ID = not equal
    }

    @Test
    fun `copy manga with updated values`() {
        val manga = Manga(id = 1L, title = "Original", favorite = false)
        val updated = manga.copy(favorite = true, title = "Updated")

        assertEquals(true, updated.favorite)
        assertEquals("Updated", updated.title)
        assertEquals(1L, updated.id) // ID unchanged
    }
}
