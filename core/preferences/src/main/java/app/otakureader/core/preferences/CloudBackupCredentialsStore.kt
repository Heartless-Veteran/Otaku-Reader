package app.otakureader.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed encrypted storage for cloud backup credentials (WebDAV).
 *
 * Credentials are stored in a dedicated EncryptedSharedPreferences file separate from
 * tracker tokens so that each concern can be managed independently.
 * All reads and writes are synchronous — callers may wrap them in [kotlinx.coroutines.Dispatchers.IO]
 * if they require a strict threading policy.
 */
@Singleton
class CloudBackupCredentialsStore @Inject constructor(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "cloud_backup_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveWebDavCredentials(url: String, username: String, password: String) {
        prefs.edit()
            .putString(KEY_WEBDAV_URL, url)
            .putString(KEY_WEBDAV_USERNAME, username)
            .putString(KEY_WEBDAV_PASSWORD, password)
            .apply()
    }

    fun getWebDavCredentials(): WebDavCredentials? {
        val url = prefs.getString(KEY_WEBDAV_URL, null) ?: return null
        val username = prefs.getString(KEY_WEBDAV_USERNAME, null) ?: return null
        val password = prefs.getString(KEY_WEBDAV_PASSWORD, null) ?: return null
        return WebDavCredentials(url = url, username = username, password = password)
    }

    fun clearWebDavCredentials() {
        prefs.edit()
            .remove(KEY_WEBDAV_URL)
            .remove(KEY_WEBDAV_USERNAME)
            .remove(KEY_WEBDAV_PASSWORD)
            .apply()
    }

    private companion object {
        const val KEY_WEBDAV_URL = "webdav_url"
        const val KEY_WEBDAV_USERNAME = "webdav_username"
        const val KEY_WEBDAV_PASSWORD = "webdav_password"
    }
}

data class WebDavCredentials(
    val url: String,
    val username: String,
    val password: String,
)
