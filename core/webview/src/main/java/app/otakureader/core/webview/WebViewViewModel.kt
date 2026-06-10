package app.otakureader.core.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val webViewPreferences: WebViewPreferences,
    private val cookieBridge: WebViewCookieBridge,
) : ViewModel() {

    val adBlockEnabled = webViewPreferences.adBlockEnabled

    fun toggleAdBlock() {
        viewModelScope.launch {
            val current = webViewPreferences.adBlockEnabled.first()
            webViewPreferences.setAdBlockEnabled(!current)
        }
    }

    /** Returns cookies held by the WebView's [android.webkit.CookieManager] for [url]. */
    fun cookiesForUrl(url: String): String = cookieBridge.cookiesForUrl(url)
}
