package app.otakureader.data.eh

import app.otakureader.core.preferences.EhSession
import app.otakureader.domain.model.EhFavorite
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Fetches and parses all pages of the E-Hentai favorites list using stored session cookies.
 *
 * This class performs synchronous OkHttp calls — callers must dispatch on an IO dispatcher.
 * Pages are fetched in a loop until the server returns an empty page (with a [MAX_PAGES]
 * safety cap), so users with more than 25 favorites receive the complete list on every sync.
 *
 * The igneous cookie determines which host to use:
 *   - Non-empty igneous → ExHentai (exhentai.org) for ExHentai-only content.
 *   - Empty/absent igneous → E-Hentai (e-hentai.org) public content.
 *
 * @param hostOverrideForTests when non-null, bypasses [resolveHost] — used in unit tests
 *   to point requests at a [okhttp3.mockwebserver.MockWebServer].
 * @param interPageDelayMillis pause between consecutive page requests. EH aggressively
 *   rate-limits rapid scraping, so production keeps the default; tests pass 0.
 */
class EhFavoritesApi(
    private val okHttpClient: OkHttpClient,
    private val hostOverrideForTests: String? = null,
    private val interPageDelayMillis: Long = INTER_PAGE_DELAY_MS,
) {
    @Inject constructor(okHttpClient: OkHttpClient) : this(okHttpClient, null, INTER_PAGE_DELAY_MS)

    fun fetchFavorites(session: EhSession): List<EhFavorite> {
        val result = mutableListOf<EhFavorite>()
        var page = 0
        while (page < MAX_PAGES) {
            if (page > 0 && interPageDelayMillis > 0) Thread.sleep(interPageDelayMillis)
            val items = fetchFavoritesPage(session, page)
            if (items.isEmpty()) break
            result += items
            page++
        }
        return result
    }

    private fun fetchFavoritesPage(session: EhSession, page: Int): List<EhFavorite> {
        val host = hostOverrideForTests ?: resolveHost(session)
        val request = Request.Builder()
            .url("$host/favorites.php?favcat=all&page=$page")
            .header("Cookie", buildCookieHeader(session))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .get()
            .build()

        val body = okHttpClient.newCall(request).execute().use { response ->
            // IOException (not check()'s IllegalStateException) so the sync worker's
            // IOException → Result.retry() path applies to transient EH errors.
            if (!response.isSuccessful) {
                throw java.io.IOException("EH returned HTTP ${response.code}")
            }
            response.body?.string() ?: return emptyList()
        }

        return parseGalleries(body)
    }

    private fun resolveHost(session: EhSession): String =
        if (session.igneous.isNotEmpty() && session.igneous != "mystery") {
            "https://exhentai.org"
        } else {
            "https://e-hentai.org"
        }

    private fun buildCookieHeader(session: EhSession): String {
        val parts = mutableListOf(
            "ipb_member_id=${session.memberId}",
            "ipb_pass_hash=${session.passHash}",
            "sl=dm_2",
        )
        if (session.igneous.isNotEmpty()) parts.add("igneous=${session.igneous}")
        return parts.joinToString("; ")
    }

    /** Visible for testing. */
    internal fun parseGalleries(html: String): List<EhFavorite> {
        // Gallery URLs appear twice per entry (thumbnail link + title link); distinct() keeps
        // only the first occurrence, preserving table order.
        val urls = GALLERY_URL_REGEX.findAll(html)
            .map { it.groupValues[1] }
            .toList()
            .distinct()

        val titles = GALLERY_TITLE_REGEX.findAll(html)
            .map { it.groupValues[1].trim() }
            .toList()

        val thumbs = GALLERY_THUMB_REGEX.findAll(html)
            .map { it.groupValues[1] }
            .toList()

        return urls.mapIndexed { i, url ->
            EhFavorite(
                galleryUrl = url,
                title = titles.getOrElse(i) { "Unknown" },
                thumbnailUrl = thumbs.getOrNull(i),
            )
        }
    }

    private companion object {
        // Matches the gallery path from either e-hentai.org or exhentai.org URLs.
        val GALLERY_URL_REGEX = Regex(
            """href="(?:https?://(?:e-hentai|exhentai)\.org)?(/g/\d+/[a-f0-9]+/)""""
        )

        // The gallery title is always inside <div class="glink">…</div>.
        val GALLERY_TITLE_REGEX = Regex("""<div class="glink">([^<]+)</div>""")

        // EH/ExH thumbnails are served from CDN hosts (t1..t5.e-hentai.org or ehgt.org).
        val GALLERY_THUMB_REGEX = Regex(
            """<img[^>]+src="(https://(?:t\d\.e-hentai\.org|ehgt\.org)/[^"]+)"[^>]*>"""
        )

        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        // Safety cap against runaway loops if EH ever changes its pagination markup and an
        // "empty" page never occurs. Termination is normally server-driven (first empty page
        // stops the loop); 200 pages × 25 items = 5 000 favorites covers realistic accounts.
        const val MAX_PAGES = 200

        // EH rate-limits and can temporarily IP-ban clients making rapid sequential requests.
        const val INTER_PAGE_DELAY_MS = 500L
    }
}
