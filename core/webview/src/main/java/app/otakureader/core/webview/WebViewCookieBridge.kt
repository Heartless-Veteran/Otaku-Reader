package app.otakureader.core.webview

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges cookies between Android's [CookieManager] (used by WebView) and
 * OkHttp's [CookieJar] (used by extension network requests).
 *
 * After a source completes a WebView challenge (e.g. Cloudflare), call
 * [syncCookiesToOkHttp] so that subsequent OkHttp requests from the same
 * source automatically include the freshly acquired cookies.
 */
@Singleton
class WebViewCookieBridge @Inject constructor() {

    /** Returns the raw cookie header string for [url] from Android's [CookieManager]. */
    fun cookiesForUrl(url: String): String =
        CookieManager.getInstance().getCookie(url) ?: ""

    /**
     * Copies all cookies that [CookieManager] holds for [url] into [cookieJar].
     *
     * Each cookie in the `Set-Cookie` / `Cookie` header format is parsed into an
     * [okhttp3.Cookie] and saved to the jar via [CookieJar.saveFromResponse].
     *
     * No-op if the URL is invalid or if there are no cookies for it.
     */
    fun syncCookiesToOkHttp(url: String, cookieJar: CookieJar) {
        val httpUrl = url.toHttpUrlOrNull() ?: return
        val raw = CookieManager.getInstance().getCookie(url) ?: return
        // CookieManager returns "name=value; name2=value2" — no domain/path metadata.
        // Cookie.parse() would default the path to the challenge URL's path and mark cookies
        // as host-only, preventing them from being sent to other paths or subdomains.
        // Use Cookie.Builder with domain(host) and path("/") so cookies apply site-wide.
        val cookies = raw.split(";").mapNotNull { part ->
            val kv = part.trim().split("=", limit = 2)
            if (kv.size != 2) return@mapNotNull null
            runCatching {
                Cookie.Builder()
                    .name(kv[0].trim())
                    .value(kv[1].trim())
                    .domain(httpUrl.host)
                    .path("/")
                    .apply { if (httpUrl.isHttps) secure() }
                    .build()
            }.getOrNull()
        }
        if (cookies.isNotEmpty()) {
            cookieJar.saveFromResponse(httpUrl, cookies)
        }
    }
}
