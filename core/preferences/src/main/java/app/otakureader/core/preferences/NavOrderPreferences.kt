package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Identifiers for the five main bottom-navigation tabs. */
enum class NavTab { LIBRARY, UPDATES, BROWSE, HISTORY, MORE }

// Matches Mihon/Komikku's default bottom-nav order: Library, Updates, History, Browse, More.
private val DEFAULT_ORDER = listOf(
    NavTab.LIBRARY, NavTab.UPDATES, NavTab.HISTORY, NavTab.BROWSE, NavTab.MORE
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
                // Parse stored tokens, silently dropping any unknown tab names so that a
                // downgrade (or a stored value from a future release with more tabs) never
                // crashes or produces an empty list.
                val parsed = raw.split(",").mapNotNull { token ->
                    val parts = token.trim().split(":")
                    val tab = runCatching { NavTab.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
                    NavTabPreferenceEntry(
                        tab = tab,
                        isVisible = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: true,
                    )
                }
                // De-duplicate: keep only the first occurrence of each tab (guards against
                // corrupted prefs that somehow repeated a tab name).
                val seen = mutableSetOf<NavTab>()
                val deduped = parsed.filter { seen.add(it.tab) }
                // Append any tabs that are present in the current build but absent from the
                // stored value (e.g. a new tab added in a later release). New tabs are visible
                // by default and placed at the end of the list.
                val knownTabs = deduped.map { it.tab }.toSet()
                val appended = deduped + NavTab.entries
                    .filter { it !in knownTabs }
                    .map { NavTabPreferenceEntry(it, isVisible = true) }
                appended.takeIf { it.isNotEmpty() }
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
