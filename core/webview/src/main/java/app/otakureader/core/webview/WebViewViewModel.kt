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
) : ViewModel() {

    /** Reactive stream of the ad-block enabled flag. Collect with [collectAsStateWithLifecycle]. */
    val adBlockEnabled = webViewPreferences.adBlockEnabled

    /** Toggles the ad-block setting based on its current persisted value. */
    fun toggleAdBlock() {
        viewModelScope.launch {
            val current = webViewPreferences.adBlockEnabled.first()
            webViewPreferences.setAdBlockEnabled(!current)
        }
    }
}
