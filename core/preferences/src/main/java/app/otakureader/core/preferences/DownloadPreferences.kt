package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Preference store for download settings including auto-download configuration.
 * Exposes reactive [Flow] properties and suspend setter functions backed by DataStore.
 */
class DownloadPreferences(private val dataStore: DataStore<Preferences>) {

    // --- Auto-Download ---

    /** Whether to automatically download new chapters when library update finds them. Default: false. */
    val autoDownloadEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_DOWNLOAD_ENABLED] ?: false }
    suspend fun setAutoDownloadEnabled(value: Boolean) = dataStore.edit { it[Keys.AUTO_DOWNLOAD_ENABLED] = value }

    /** Whether to download only when connected to Wi-Fi. Default: true. */
    val downloadOnlyOnWifi: Flow<Boolean> = dataStore.data.map { it[Keys.DOWNLOAD_ONLY_ON_WIFI] ?: true }
    suspend fun setDownloadOnlyOnWifi(value: Boolean) = dataStore.edit { it[Keys.DOWNLOAD_ONLY_ON_WIFI] = value }

    /** Maximum number of new chapters to auto-download per manga. Default: 3. */
    val autoDownloadLimit: Flow<Int> = dataStore.data.map { it[Keys.AUTO_DOWNLOAD_LIMIT] ?: 3 }
    suspend fun setAutoDownloadLimit(value: Int) = dataStore.edit { it[Keys.AUTO_DOWNLOAD_LIMIT] = value }

    // --- Save as CBZ ---

    /**
     * Whether to compress downloaded chapter pages into a CBZ archive.
     * Default: false (loose files are the default for compatibility).
     */
    val saveAsCbz: Flow<Boolean> = dataStore.data.map { it[Keys.SAVE_AS_CBZ] ?: false }
    suspend fun setSaveAsCbz(value: Boolean) = dataStore.edit { it[Keys.SAVE_AS_CBZ] = value }

    // --- Delete After Reading ---

    /** Whether to automatically delete downloaded chapters once finished reading. Default: false. */
    val deleteAfterReading: Flow<Boolean> = dataStore.data.map { it[Keys.DELETE_AFTER_READING] ?: false }
    suspend fun setDeleteAfterReading(value: Boolean) = dataStore.edit { it[Keys.DELETE_AFTER_READING] = value }

    /**
     * Per-manga overrides for delete-after-reading stored as a comma-separated string
     * in the form "mangaId:MODE,mangaId:MODE".
     */
    val perMangaOverrides: Flow<Map<Long, DeleteAfterReadMode>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.PER_MANGA_OVERRIDES] ?: return@map emptyMap()
        raw.split(',')
            .mapNotNull { entry ->
                val parts = entry.split(':')
                if (parts.size != 2) return@mapNotNull null
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                val mode = runCatching { DeleteAfterReadMode.valueOf(parts[1]) }.getOrNull()
                    ?: return@mapNotNull null
                id to mode
            }
            .toMap()
    }

    /** Sets a per-manga delete-after-reading override. [DeleteAfterReadMode.INHERIT] removes the override. */
    suspend fun setOverride(mangaId: Long, mode: DeleteAfterReadMode) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.PER_MANGA_OVERRIDES]
                ?.split(',')
                ?.filter { it.isNotEmpty() }
                ?.associate {
                    val parts = it.split(':')
                    parts[0].toLong() to parts[1]
                }
                ?.toMutableMap()
                ?: mutableMapOf()
            if (mode == DeleteAfterReadMode.INHERIT) {
                current.remove(mangaId)
            } else {
                current[mangaId] = mode.name
            }
            prefs[Keys.PER_MANGA_OVERRIDES] = current.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    /**
     * Returns a [Flow] that emits `true` when delete-after-reading is effectively enabled for
     * the given manga (considering both the global toggle and any per-manga override).
     */
    fun isDeleteAfterReadingEnabled(mangaId: Long): Flow<Boolean> =
        combine(deleteAfterReading, perMangaOverrides) { global, overrides ->
            when (overrides[mangaId] ?: DeleteAfterReadMode.INHERIT) {
                DeleteAfterReadMode.ENABLED -> true
                DeleteAfterReadMode.DISABLED -> false
                DeleteAfterReadMode.INHERIT -> global
            }
        }

    private object Keys {
        val AUTO_DOWNLOAD_ENABLED = booleanPreferencesKey("auto_download_enabled")
        val DOWNLOAD_ONLY_ON_WIFI = booleanPreferencesKey("download_only_on_wifi")
        val AUTO_DOWNLOAD_LIMIT = intPreferencesKey("auto_download_limit")
        val SAVE_AS_CBZ = booleanPreferencesKey("save_as_cbz")
        val DELETE_AFTER_READING = booleanPreferencesKey("delete_after_reading")
        val PER_MANGA_OVERRIDES = stringPreferencesKey("per_manga_overrides")
    }
}
