package app.otakureader.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed encrypted storage for an in-flight OAuth PKCE session.
 *
 * Before opening the browser for OAuth authorization, call [save] with the
 * tracker ID, PKCE code verifier, and CSRF state token. After the browser
 * redirects back and the code is exchanged, call [clear] to delete the session.
 * Sessions older than [SESSION_TTL_MS] are treated as expired.
 */
@Singleton
class PendingOAuthStore @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val FILE_NAME = "pending_oauth_session"
        private const val KEY_TRACKER_ID = "tracker_id"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val KEY_STATE = "state"
        private const val KEY_EXPIRES_AT = "expires_at"
        /** Sessions expire after 10 minutes — authorization codes are short-lived. */
        private const val SESSION_TTL_MS = 10 * 60 * 1000L
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Persists an OAuth PKCE session before opening the browser. */
    suspend fun save(trackerId: Int, codeVerifier: String, state: String) {
        withContext(Dispatchers.IO) {
            prefs.edit()
                .putInt(KEY_TRACKER_ID, trackerId)
                .putString(KEY_CODE_VERIFIER, codeVerifier)
                .putString(KEY_STATE, state)
                .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + SESSION_TTL_MS)
                .apply()
        }
    }

    /**
     * Returns the stored session if one exists and has not expired, or `null`.
     * An expired session is cleared automatically before returning null.
     */
    suspend fun get(): PendingOAuthSession? = withContext(Dispatchers.IO) {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt == 0L) return@withContext null
        if (System.currentTimeMillis() > expiresAt) {
            clearInternal()
            return@withContext null
        }
        val trackerId = prefs.getInt(KEY_TRACKER_ID, -1)
        val codeVerifier = prefs.getString(KEY_CODE_VERIFIER, null)
        val state = prefs.getString(KEY_STATE, null)
        if (trackerId == -1 || codeVerifier == null || state == null) {
            clearInternal()
            return@withContext null
        }
        PendingOAuthSession(trackerId, codeVerifier, state)
    }

    /** Deletes the stored session after a successful or failed code exchange. */
    suspend fun clear() {
        withContext(Dispatchers.IO) { clearInternal() }
    }

    private fun clearInternal() {
        prefs.edit()
            .remove(KEY_TRACKER_ID)
            .remove(KEY_CODE_VERIFIER)
            .remove(KEY_STATE)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }
}

data class PendingOAuthSession(
    val trackerId: Int,
    val codeVerifier: String,
    val state: String,
)
