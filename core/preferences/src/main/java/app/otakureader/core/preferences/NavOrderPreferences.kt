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

/**
 * Lightweight projection of a tab's persisted state: its position in the list and whether
 * the user has toggled it on or off. Kept in core so the ViewModel can convert to and from
 * the UI-layer [NavTabEntry] without the preferences module depending on the feature layer.
 */
data class NavTabPreferenceEntry(val tab: NavTab, val isVisible: Boolean)

/** Persists the user-defined bottom-nav tab order and per-tab visibility. */
class NavOrderPreferences(private val dataStore: DataStore<Preferences>) {

    /**
     * Emits the current ordered list of tabs with their visibility flags.
     *
     * Storage format (TAB_ORDER key): "LIBRARY:true,UPDATES:false,BROWSE:true,…"
     * Falls back gracefully to the default order (all visible) on any parse error or if the
     * stored list is incomplete (e.g. after a new tab is added in a future release).
     */
    val tabOrder: Flow<List<NavTabPreferenceEntry>> = dataStore.data.map { prefs ->
        prefs[Keys.TAB_ORDER]?.let { raw ->
            runCatching {
                val parsed = raw.split(",").map { token ->
                    val parts = token.trim().split(":")
                    NavTabPreferenceEntry(
                        tab = NavTab.valueOf(parts[0]),
                        isVisible = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: true,
                    )
                }
                // Only accept if every tab is represented exactly once.
                parsed.takeIf {
                    it.size == NavTab.entries.size &&
                        it.map { e -> e.tab }.toSet().size == NavTab.entries.size
                }
            }.getOrNull()
        } ?: DEFAULT_ORDER.map { NavTabPreferenceEntry(it, isVisible = true) }
    }

    /** Persists the full ordered list including visibility flags. */
    suspend fun setTabOrder(order: List<NavTabPreferenceEntry>) {
        dataStore.edit { prefs ->
            prefs[Keys.TAB_ORDER] = order.joinToString(",") { "${it.tab.name}:${it.isVisible}" }
        }
    }

    private object Keys {
        val TAB_ORDER = stringPreferencesKey("nav_tab_order")
    }
}
