package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed preference store for reader progress sync settings.
 *
 * Injected as a [Singleton] so that all callers observe the same DataStore
 * instance provided by [PreferencesModule].
 *
 * Keys:
 * - [serverUrl]    – base URL of the self-hosted sync server (blank = disabled)
 * - [bearerToken]  – JWT / opaque bearer token for server auth
 * - [deviceId]     – stable random UUID that identifies this installation
 */
@Singleton
class SyncSettingsStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    /** Base URL of the sync server, e.g. `https://sync.example.com`. Blank means disabled. */
    val serverUrl: Flow<String> = dataStore.data.map { it[Keys.SERVER_URL] ?: "" }
    suspend fun setServerUrl(value: String) = dataStore.edit { it[Keys.SERVER_URL] = value }

    /** Bearer token for authenticating with the sync server. */
    val bearerToken: Flow<String> = dataStore.data.map { it[Keys.BEARER_TOKEN] ?: "" }
    suspend fun setBearerToken(value: String) = dataStore.edit { it[Keys.BEARER_TOKEN] = value }

    /**
     * Stable device identifier. Returns the persisted UUID, or a blank string if not yet
     * written. Use [ensureDeviceId] to guarantee a value is generated and persisted.
     */
    val deviceId: Flow<String> = dataStore.data.map { it[Keys.DEVICE_ID] ?: "" }

    /**
     * Returns the persisted device ID, generating and persisting a new UUID if one does
     * not yet exist. Atomic: generates and writes in a single [DataStore.edit] transaction
     * so concurrent callers cannot each write a different UUID.
     */
    suspend fun ensureDeviceId(): String {
        var resultId = ""
        dataStore.edit { prefs ->
            val existing = prefs[Keys.DEVICE_ID]
            if (!existing.isNullOrBlank()) {
                resultId = existing
            } else {
                val newId = UUID.randomUUID().toString()
                prefs[Keys.DEVICE_ID] = newId
                resultId = newId
            }
        }
        return resultId
    }

    /** True when a non-blank server URL is configured. */
    val syncEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.SERVER_URL]?.isNotBlank() ?: false }

    private object Keys {
        val SERVER_URL = stringPreferencesKey("sync_server_url")
        val BEARER_TOKEN = stringPreferencesKey("sync_bearer_token")
        val DEVICE_ID = stringPreferencesKey("sync_device_id")
    }
}
