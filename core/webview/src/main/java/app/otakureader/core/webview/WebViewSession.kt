package app.otakureader.core.webview

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Configures a [WebView] instance with sane defaults and the appropriate [WebViewClient].
 *
 * If [adBlockEnabled] is true, an [AdBlockingWebViewClient] is installed; otherwise
 * a minimal client that still flushes cookies on page load is used.
 */
class WebViewSession(
    private val context: Context,
    private val adBlockEnabled: Boolean,
    private val onPageFinished: (url: String?) -> Unit = {},
) {
    fun configure(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // Block local file/content access from web content (default true on API < 30)
            allowFileAccess = false
            allowContentAccess = false
            // Flag malicious URLs via Google Safe Browsing
            safeBrowsingEnabled = true
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.webViewClient = if (adBlockEnabled) {
            AdBlockingWebViewClient(context, onPageFinished)
        } else {
            object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    CookieManager.getInstance().flush()
                    onPageFinished(url)
                }
            }
        }
    }
}
