package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Preference store for recent search history.
 *
 * Stores the last N unique search queries (default 20) as a JSON-serialized list.
 * Duplicate queries bubble to the top (most recent first).
 */
class SearchHistoryPreferences(
    private val dataStore: DataStore<Preferences>
) {

    /** Recent search queries, most recent first. */
    val recentSearches: Flow<List<String>> = dataStore.data
        .map { prefs ->
            val json = prefs[KEY_SEARCH_HISTORY] ?: "[]"
            try {
                Json.decodeFromString<List<String>>(json)
            } catch (_: Exception) {
                emptyList()
            }
        }

    /**
     * Add a query to search history. If it already exists, moves it to the top.
     * Trims to [maxHistory] items.
     */
    suspend fun addSearchQuery(query: String, maxHistory: Int = 20) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        dataStore.edit { prefs ->
            val json = prefs[KEY_SEARCH_HISTORY] ?: "[]"
            val existing = try {
                Json.decodeFromString<MutableList<String>>(json)
            } catch (_: Exception) {
                mutableListOf()
            }
            // Remove if exists, then add to front
            existing.removeAll { it.equals(trimmed, ignoreCase = true) }
            existing.add(0, trimmed)
            // Trim to max
            while (existing.size > maxHistory) existing.removeLast()
            prefs[KEY_SEARCH_HISTORY] = Json.encodeToString(existing)
        }
    }

    /** Remove a specific query from history. */
    suspend fun removeSearchQuery(query: String) {
        dataStore.edit { prefs ->
            val json = prefs[KEY_SEARCH_HISTORY] ?: "[]"
            val existing = try {
                Json.decodeFromString<MutableList<String>>(json)
            } catch (_: Exception) {
                mutableListOf()
            }
            existing.removeAll { it.equals(query, ignoreCase = true) }
            prefs[KEY_SEARCH_HISTORY] = Json.encodeToString(existing)
        }
    }

    /** Clear all search history. */
    suspend fun clearHistory() {
        dataStore.edit { it.remove(KEY_SEARCH_HISTORY) }
    }

    companion object {
        private val KEY_SEARCH_HISTORY = stringPreferencesKey("search_history_json")
    }
}
