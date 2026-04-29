package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Preference store for reading speed tracking.
 * Stores the user's average reading speed (pages per minute) and per-chapter read durations.
 */
class ReadingSpeedPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object Keys {
        /** Average pages the user reads per minute. Default: 3.0 (20 sec per page). */
        val AVERAGE_PAGES_PER_MINUTE = floatPreferencesKey("reading_speed_pages_per_minute")

        /** Total pages read while tracking was active. */
        val TOTAL_PAGES_TRACKED = longPreferencesKey("reading_speed_total_pages")

        /** Total minutes spent reading while tracking was active. */
        val TOTAL_MINUTES_TRACKED = longPreferencesKey("reading_speed_total_minutes")
    }

    /** Average pages per minute, or null if not enough data yet. */
    val averagePagesPerMinute: Flow<Float?> = dataStore.data.map { prefs ->
        val ppm = prefs[AVERAGE_PAGES_PER_MINUTE]
        if (ppm != null && ppm > 0) ppm else null
    }

    /** Total pages tracked. */
    val totalPagesTracked: Flow<Long> = dataStore.data.map { it[TOTAL_PAGES_TRACKED] ?: 0L }

    /** Total minutes tracked. */
    val totalMinutesTracked: Flow<Long> = dataStore.data.map { it[TOTAL_MINUTES_TRACKED] ?: 0L }

    /**
     * Record a reading session and update the rolling average.
     *
     * @param pagesRead number of pages read in this session
     * @param minutesSpent minutes spent reading in this session
     */
    suspend fun recordSession(pagesRead: Int, minutesSpent: Float) {
        if (pagesRead <= 0 || minutesSpent <= 0) return
        dataStore.edit { prefs ->
            val totalPages = (prefs[TOTAL_PAGES_TRACKED] ?: 0L) + pagesRead
            val totalMinutes = (prefs[TOTAL_MINUTES_TRACKED] ?: 0L) + minutesSpent.toLong()
            prefs[TOTAL_PAGES_TRACKED] = totalPages
            prefs[TOTAL_MINUTES_TRACKED] = totalMinutes
            if (totalMinutes > 0) {
                prefs[AVERAGE_PAGES_PER_MINUTE] = totalPages.toFloat() / totalMinutes.toFloat()
            }
        }
    }

    /** Reset reading speed stats. */
    suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(AVERAGE_PAGES_PER_MINUTE)
            prefs.remove(TOTAL_PAGES_TRACKED)
            prefs.remove(TOTAL_MINUTES_TRACKED)
        }
    }
}
