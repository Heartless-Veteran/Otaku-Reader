package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preference store for scheduled/automatic backup settings.
 * Exposes reactive [Flow] properties and suspend setter functions backed by DataStore.
 */
class BackupPreferences(private val dataStore: DataStore<Preferences>) {

    /** Whether automatic periodic backups are enabled. Default: false. */
    val autoBackupEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_BACKUP_ENABLED] ?: false }
    suspend fun setAutoBackupEnabled(value: Boolean) = dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = value }

    /**
     * Backup interval in hours.
     * Supported values: 6, 12, 24, 48, 168 (weekly).
     * Default: 24 hours.
     */
    val autoBackupIntervalHours: Flow<Int> = dataStore.data.map { it[Keys.AUTO_BACKUP_INTERVAL_HOURS] ?: 24 }
    suspend fun setAutoBackupIntervalHours(value: Int) = dataStore.edit { it[Keys.AUTO_BACKUP_INTERVAL_HOURS] = value }

    /**
     * Maximum number of local automatic backup files to retain.
     * Older backups are deleted when the limit is exceeded.
     * Default: 5.
     */
    val autoBackupMaxCount: Flow<Int> = dataStore.data.map { it[Keys.AUTO_BACKUP_MAX_COUNT] ?: 5 }
    suspend fun setAutoBackupMaxCount(value: Int) = dataStore.edit { it[Keys.AUTO_BACKUP_MAX_COUNT] = value }

    /**
     * SAF tree URI where a copy of each automatic backup is written, in addition to the
     * app's private storage. Empty string means "app storage only" (the default).
     */
    val autoBackupLocationUri: Flow<String> = dataStore.data.map { it[Keys.AUTO_BACKUP_LOCATION_URI] ?: "" }
    suspend fun setAutoBackupLocationUri(value: String) = dataStore.edit { it[Keys.AUTO_BACKUP_LOCATION_URI] = value }

    /** Epoch-millis timestamp of the last successful automatic backup. 0 means never. */
    val lastAutoBackupTimestamp: Flow<Long> = dataStore.data.map { it[Keys.LAST_AUTO_BACKUP_TIMESTAMP] ?: 0L }
    suspend fun setLastAutoBackupTimestamp(value: Long) = dataStore.edit { it[Keys.LAST_AUTO_BACKUP_TIMESTAMP] = value }

    /**
     * Selected cloud backup destination key.
     * Supported values: "NONE" (default), "WEBDAV".
     */
    val cloudDestination: Flow<String> = dataStore.data.map { it[Keys.CLOUD_DESTINATION] ?: "NONE" }
    suspend fun setCloudDestination(value: String) = dataStore.edit { it[Keys.CLOUD_DESTINATION] = value }

    private object Keys {
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_INTERVAL_HOURS = intPreferencesKey("auto_backup_interval_hours")
        val AUTO_BACKUP_MAX_COUNT = intPreferencesKey("auto_backup_max_count")
        val AUTO_BACKUP_LOCATION_URI = stringPreferencesKey("auto_backup_location_uri")
        val LAST_AUTO_BACKUP_TIMESTAMP = longPreferencesKey("last_auto_backup_timestamp")
        val CLOUD_DESTINATION = stringPreferencesKey("cloud_destination")
    }
}
