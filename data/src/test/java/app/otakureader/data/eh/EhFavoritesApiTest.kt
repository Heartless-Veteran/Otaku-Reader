package app.otakureader.data.eh

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EhFavoritesApiTest {

    private val api = EhFavoritesApi(mockk(relaxed = true))

    @Test
    fun `parseGalleries returns empty list for empty HTML`() {
        val result = api.parseGalleries("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseGalleries extracts single gallery entry`() {
        val html = """
            <td class="glthumb">
              <a href="https://e-hentai.org/g/123456/abc12345/">
                <img src="https://t1.e-hentai.org/thumb/abc.jpg" alt="" />
              </a>
            </td>
            <td class="gl4e glname">
              <a href="https://e-hentai.org/g/123456/abc12345/">
                <div class="glink">Test Gallery Title</div>
              </a>
            </td>
        """.trimIndent()

        val result = api.parseGalleries(html)

        assertEquals(1, result.size)
        assertEquals("/g/123456/abc12345/", result[0].galleryUrl)
        assertEquals("Test Gallery Title", result[0].title)
        assertEquals("https://t1.e-hentai.org/thumb/abc.jpg", result[0].thumbnailUrl)
    }

    @Test
    fun `parseGalleries extracts multiple gallery entries`() {
        val html = buildMultiGalleryHtml(
            listOf(
                Triple("111111", "aaa11111", "Gallery One"),
                Triple("222222", "bbb22222", "Gallery Two"),
                Triple("333333", "ccc33333", "Gallery Three"),
            )
        )

        val result = api.parseGalleries(html)

        assertEquals(3, result.size)
        assertEquals("/g/111111/aaa11111/", result[0].galleryUrl)
        assertEquals("Gallery One", result[0].title)
        assertEquals("/g/222222/bbb22222/", result[1].galleryUrl)
        assertEquals("Gallery Two", result[1].title)
        assertEquals("/g/333333/ccc33333/", result[2].galleryUrl)
        assertEquals("Gallery Three", result[2].title)
    }

    @Test
    fun `parseGalleries handles exhentai URLs`() {
        val html = """
            <a href="https://exhentai.org/g/999999/def99999/">
              <img src="https://ehgt.org/thumb/xyz.jpg" alt="" />
            </a>
            <a href="https://exhentai.org/g/999999/def99999/">
              <div class="glink">ExHentai Gallery</div>
            </a>
        """.trimIndent()

        val result = api.parseGalleries(html)

        assertEquals(1, result.size)
        assertEquals("/g/999999/def99999/", result[0].galleryUrl)
        assertEquals("ExHentai Gallery", result[0].title)
    }

    @Test
    fun `parseGalleries deduplicates URL that appears twice per entry`() {
        // Each EH row has the URL in both thumbnail link and title link.
        val html = """
            <a href="https://e-hentai.org/g/555555/eee55555/">thumb</a>
            <div class="glink">Deduplicated Title</div>
            <a href="https://e-hentai.org/g/555555/eee55555/">title link</a>
        """.trimIndent()

        val result = api.parseGalleries(html)

        assertEquals(1, result.size)
        assertEquals("/g/555555/eee55555/", result[0].galleryUrl)
    }

    // --- helpers ---

    private fun buildMultiGalleryHtml(entries: List<Triple<String, String, String>>): String =
        entries.joinToString("\n") { (id, token, title) ->
            """
            <a href="https://e-hentai.org/g/$id/$token/">
              <img src="https://t1.e-hentai.org/thumb/$token.jpg" alt="" />
            </a>
            <a href="https://e-hentai.org/g/$id/$token/">
              <div class="glink">$title</div>
            </a>
            """.trimIndent()
        }
}
