package app.otakureader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MangaTest {

    @Test
    fun `create manga with default values`() {
        val manga = Manga(
            id = 1L,
            sourceId = 1L,
            url = "https://example.com/manga/1",
            title = "Test Manga",
            description = "A test description",
        )

        assertEquals(1L, manga.id)
        assertEquals("Test Manga", manga.title)
        assertEquals("A test description", manga.description)
        assertEquals(false, manga.favorite)
    }

    @Test
    fun `manga equality based on id`() {
        val manga1 = Manga(id = 1L, sourceId = 1L, url = "/m/1", title = "A")
        val manga2 = manga1.copy() // exact copy
        val manga3 = Manga(id = 2L, sourceId = 1L, url = "/m/2", title = "A")

        assertEquals(manga1, manga2) // Same all fields = equal
        assertNotEquals(manga1, manga3) // Different ID = not equal
    }

    @Test
    fun `copy manga with updated values`() {
        val manga = Manga(id = 1L, sourceId = 1L, url = "/m/1", title = "Original", favorite = false)
        val updated = manga.copy(favorite = true, title = "Updated")

        assertEquals(true, updated.favorite)
        assertEquals("Updated", updated.title)
        assertEquals(1L, updated.id) // ID unchanged
    }
}
