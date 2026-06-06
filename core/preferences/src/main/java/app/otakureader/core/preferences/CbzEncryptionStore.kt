package app.otakureader.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed encrypted storage for the CBZ archive encryption passphrase.
 *
 * Uses the same EncryptedSharedPreferences pattern as [CloudBackupCredentialsStore].
 * All reads and writes are synchronous — wrap in [kotlinx.coroutines.Dispatchers.IO]
 * if a strict threading policy is required.
 */
@Singleton
class CbzEncryptionStore @Inject constructor(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "cbz_encryption",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getPassphrase(): String? = prefs.getString(KEY_PASSPHRASE, null)

    fun setPassphrase(passphrase: String) {
        prefs.edit().putString(KEY_PASSPHRASE, passphrase).apply()
    }

    fun clearPassphrase() {
        prefs.edit().remove(KEY_PASSPHRASE).apply()
    }

    private companion object {
        const val KEY_PASSPHRASE = "cbz_passphrase"
    }
}
