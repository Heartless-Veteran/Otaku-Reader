package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Identifiers for the five main bottom-navigation tabs. */
enum class NavTab { LIBRARY, UPDATES, BROWSE, HISTORY, MORE }

private val DEFAULT_ORDER = listOf(
    NavTab.LIBRARY, NavTab.UPDATES, NavTab.BROWSE, NavTab.HISTORY, NavTab.MORE
)

/** Persists the user-defined bottom-nav tab order. */
class NavOrderPreferences(private val dataStore: DataStore<Preferences>) {

    /** Emits the current tab order. Falls back to the default on any parse error. */
    val tabOrder: Flow<List<NavTab>> = dataStore.data.map { prefs ->
        prefs[Keys.TAB_ORDER]?.let { raw ->
            runCatching {
                raw.split(",").map { NavTab.valueOf(it.trim()) }
                    .takeIf { it.size == NavTab.entries.size && it.toSet().size == NavTab.entries.size }
            }.getOrNull()
        } ?: DEFAULT_ORDER
    }

    suspend fun setTabOrder(order: List<NavTab>) {
        dataStore.edit { it[Keys.TAB_ORDER] = order.joinToString(",") { tab -> tab.name } }
    }

    private object Keys {
        val TAB_ORDER = stringPreferencesKey("nav_tab_order")
    }
}
