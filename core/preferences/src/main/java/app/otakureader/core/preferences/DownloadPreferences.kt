package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Preferences related to chapter downloads.
 *
 * Stores the global "delete after reading" flag and optional per-manga overrides.
 */
class DownloadPreferences(
    private val dataStore: DataStore<Preferences>
) {

    /** Global toggle for removing downloaded chapters after they are read. */
    val deleteAfterReading: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.DELETE_AFTER_READING] ?: false }

    /** Map of mangaId -> override value. */
    val perMangaOverrides: Flow<Map<Long, DeleteAfterReadMode>> = dataStore.data.map { prefs ->
        decodeOverrides(prefs[Keys.PER_MANGA_OVERRIDES].orEmpty())
    }

    suspend fun setDeleteAfterReading(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DELETE_AFTER_READING] = enabled }
    }

    /**
     * Sets the override for the given [mangaId].
     * Use [DeleteAfterReadMode.INHERIT] to clear the override and fall back to global setting.
     */
    suspend fun setOverride(mangaId: Long, mode: DeleteAfterReadMode) {
        dataStore.edit { prefs ->
            val current = decodeOverrides(prefs[Keys.PER_MANGA_OVERRIDES].orEmpty())
            val updated = when (mode) {
                DeleteAfterReadMode.INHERIT -> current - mangaId
                else -> current + (mangaId to mode)
            }
            prefs[Keys.PER_MANGA_OVERRIDES] = encodeOverrides(updated)
        }
    }

    /**
     * Returns the effective preference for the given [mangaId], combining global and override.
     */
    suspend fun isDeleteAfterReadingEnabled(mangaId: Long): Boolean {
        val global = deleteAfterReading.first()
        val overrides = perMangaOverrides.first()
        return when (overrides[mangaId]) {
            DeleteAfterReadMode.ENABLED -> true
            DeleteAfterReadMode.DISABLED -> false
            else -> global
        }
    }

    private fun encodeOverrides(map: Map<Long, DeleteAfterReadMode>): String =
        map.entries.joinToString(separator = ",") { "${it.key}:${it.value.ordinal}" }

    private fun decodeOverrides(raw: String): Map<Long, DeleteAfterReadMode> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size != 2) return@mapNotNull null
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                val mode = parts[1].toIntOrNull()
                    ?.let { DeleteAfterReadMode.entries.getOrNull(it) }
                    ?: return@mapNotNull null
                id to mode
            }
            .toMap()
    }

    private object Keys {
        val DELETE_AFTER_READING = booleanPreferencesKey("delete_after_reading")
        val PER_MANGA_OVERRIDES = stringPreferencesKey("delete_after_read_overrides")
    }
}
