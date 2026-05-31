package app.otakureader.core.webview

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed preferences for the WebView module.
 *
 * The [DataStore]<[Preferences]> singleton is provided by Hilt via
 * [app.otakureader.core.preferences.di.PreferencesModule] and injected automatically
 * because this class uses `@Singleton @Inject constructor`.
 */
@Singleton
class WebViewPreferences @Inject constructor(private val dataStore: DataStore<Preferences>) {

    /** Whether the built-in ad blocker is enabled. Defaults to `true`. */
    val adBlockEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.AD_BLOCK_ENABLED] ?: true }

    suspend fun setAdBlockEnabled(value: Boolean) =
        dataStore.edit { it[Keys.AD_BLOCK_ENABLED] = value }

    private object Keys {
        val AD_BLOCK_ENABLED = booleanPreferencesKey("webview_ad_block_enabled")
    }
}
