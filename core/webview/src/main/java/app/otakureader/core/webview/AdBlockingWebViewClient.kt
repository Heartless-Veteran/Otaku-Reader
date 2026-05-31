package app.otakureader.core.webview

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

/**
 * A [WebViewClient] that blocks ad/tracking hosts loaded from the OISD-small
 * host list in assets. Subdomain matching is handled by walking up the host
 * hierarchy so that, e.g., "ad.doubleclick.net" is blocked when "doubleclick.net"
 * is in the list.
 */
class AdBlockingWebViewClient(
    private val context: Context,
    private val onPageFinished: (url: String?) -> Unit = {},
) : WebViewClient() {

    private val blockedHosts: Set<String> by lazy { AdBlockHosts.get(context) }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val host = request.url.host ?: return super.shouldInterceptRequest(view, request)
        if (isBlocked(host)) {
            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        CookieManager.getInstance().flush()
        onPageFinished(url)
    }

    /**
     * Returns true if [host] or any of its parent domains appears in [blockedHosts].
     * E.g. "a.b.doubleclick.net" is blocked because "doubleclick.net" is blocked.
     */
    private fun isBlocked(host: String): Boolean {
        if (blockedHosts.contains(host)) return true
        var dot = host.indexOf('.')
        while (dot != -1) {
            if (blockedHosts.contains(host.substring(dot + 1))) return true
            dot = host.indexOf('.', dot + 1)
        }
        return false
    }
}
