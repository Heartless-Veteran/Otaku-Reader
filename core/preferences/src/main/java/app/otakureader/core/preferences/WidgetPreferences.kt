package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.otakureader.domain.model.WidgetSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore-backed preferences for per-widget configuration.
 *
 * Settings are stored as a single JSON-encoded list keyed on [Keys.WIDGET_SETTINGS]. Using a
 * single list instead of per-widget keys makes it trivial to add new widget types in the future
 * without introducing new preference keys.
 *
 * The JSON codec is lenient so that adding new fields to [WidgetSettings] in a future release
 * does not crash users who still have the old JSON written to disk.
 */
class WidgetPreferences(private val dataStore: DataStore<Preferences>) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * The current widget settings list.
     *
     * Emits [WidgetSettings.defaults] when nothing has been saved yet (first launch or after
     * a data-clear). Never emits an empty list — every [WidgetType] always has an entry.
     */
    val widgetSettings: Flow<List<WidgetSettings>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.WIDGET_SETTINGS]
        if (raw.isNullOrBlank()) {
            WidgetSettings.defaults()
        } else {
            runCatching { json.decodeFromString<List<WidgetSettings>>(raw) }
                .getOrDefault(WidgetSettings.defaults())
        }
    }

    /**
     * Persists a new settings list.
     *
     * Callers are responsible for supplying a complete list (one entry per [WidgetType]). The
     * easiest way to update a single widget is:
     * ```kotlin
     * val current = widgetSettings.first().toMutableList()
     * val index = current.indexOfFirst { it.widgetType == type }
     * if (index >= 0) current[index] = updated
     * setWidgetSettings(current)
     * ```
     */
    suspend fun setWidgetSettings(settings: List<WidgetSettings>) {
        dataStore.edit { prefs ->
            prefs[Keys.WIDGET_SETTINGS] = json.encodeToString(settings)
        }
    }

    private object Keys {
        val WIDGET_SETTINGS = stringPreferencesKey("widget_settings")
    }
}
