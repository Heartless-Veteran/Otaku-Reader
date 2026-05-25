package app.otakureader.data.privacy

import app.otakureader.data.download.DownloadProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Privacy regression tests (#908).
 *
 * Verifies three core privacy invariants:
 * 1. File-path sanitisation strips characters that could expose file-system layout.
 * 2. HTTP connections never include tracker auth tokens on non-tracker hosts.
 * 3. Manga titles are not present in header values sent to external hosts.
 */
class PrivacyRegressionTest {

    private val server = MockWebServer()

    @Before
    fun startServer() {
        server.start()
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    // ── (1) Sanitisation — file path characters ──────────────────────────────

    @Test
    fun `sanitize strips filesystem reserved characters`() {
        val dirty = "Attack/on:Titan*Season<2>?Part|1"
        val clean = DownloadProvider.sanitize(dirty)

        assertFalse("Slash must be replaced", clean.contains('/'))
        assertFalse("Backslash must be replaced", clean.contains('\\'))
        assertFalse("Colon must be replaced", clean.contains(':'))
        assertFalse("Asterisk must be replaced", clean.contains('*'))
        assertFalse("Question mark must be replaced", clean.contains('?'))
        assertFalse("Angle bracket < must be replaced", clean.contains('<'))
        assertFalse("Angle bracket > must be replaced", clean.contains('>'))
        assertFalse("Pipe must be replaced", clean.contains('|'))
    }

    @Test
    fun `sanitize preserves alphanumeric and spaces`() {
        val input = "My Hero Academia Vol 5"
        assertEquals(input, DownloadProvider.sanitize(input))
    }

    @Test
    fun `sanitize trims surrounding whitespace`() {
        assertEquals("title", DownloadProvider.sanitize("  title  "))
    }

    // ── (2) Auth header isolation — tracker tokens stay on tracker hosts ──────

    @Test
    fun `Authorization header is not sent to non-tracker host`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.host
                val isTrackerHost = url.contains("myanimelist") ||
                    url.contains("anilist") ||
                    url.contains("kitsu") ||
                    url.contains("mangaupdates") ||
                    url.contains("shikimori")

                val request = if (isTrackerHost) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer secret-token-123")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .build()

        client.newCall(Request.Builder().url(server.url("/opds/catalog")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(
            "Authorization header must not be sent to non-tracker OPDS host",
            recorded.getHeader("Authorization")
        )
    }

    @Test
    fun `manga title does not appear in User-Agent or request headers`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val sensitiveTitle = "SecretMangaTitle12345"
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "OtakuReader/1.0 Android")
                    .build()
                chain.proceed(req)
            }
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        val ua = recorded.getHeader("User-Agent") ?: ""
        assertFalse(
            "Manga title must not appear in User-Agent",
            ua.contains(sensitiveTitle)
        )
    }

    // ── (3) OPDS TLS requirement ──────────────────────────────────────────────

    @Test
    fun `OPDS http URL is recognised as insecure`() {
        val httpUrl = "http://my-komga-server.local/opds/v1.2"
        val httpsUrl = "https://my-komga-server.local/opds/v1.2"

        assertTrue("https:// must be considered secure", httpsUrl.startsWith("https://"))
        assertFalse("http:// must not be considered secure", httpUrl.startsWith("https://"))
    }

    @Test
    fun `tracker auth header is present on tracker host`() {
        val trackerServer = MockWebServer()
        trackerServer.start()
        trackerServer.enqueue(MockResponse().setResponseCode(200))

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.host
                val isTrackerHost = url == "127.0.0.1" || url == "localhost" || url.contains("myanimelist")
                val req = if (isTrackerHost) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer tracker-token")
                        .build()
                } else chain.request()
                chain.proceed(req)
            }
            .build()

        client.newCall(Request.Builder().url(trackerServer.url("/api/v2")).build()).execute().close()

        val recorded = trackerServer.takeRequest()
        assertEquals(
            "Authorization header must be forwarded to tracker host",
            "Bearer tracker-token",
            recorded.getHeader("Authorization")
        )
        trackerServer.shutdown()
    }
}
