package app.otakureader.core.webview

import android.webkit.CookieManager

object WebViewCookieBridge {
    fun cookiesForUrl(url: String): String =
        CookieManager.getInstance().getCookie(url) ?: ""
}
