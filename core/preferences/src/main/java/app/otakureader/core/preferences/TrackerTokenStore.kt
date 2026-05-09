package app.otakureader.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed encrypted storage for tracker OAuth tokens.
 *
 * Tokens are keyed by [trackerId] so each tracker manages its own slot.
 * All reads and writes are synchronous because [EncryptedSharedPreferences]
 * is itself synchronous — callers may wrap them in [kotlinx.coroutines.Dispatchers.IO]
 * if they require a strict threading policy.
 */
@Singleton
class TrackerTokenStore @Inject constructor(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "tracker_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(
        trackerId: Int,
        accessToken: String,
        refreshToken: String? = null,
        userId: Long? = null,
    ) {
        val editor = prefs.edit()
        editor.putString("access_$trackerId", accessToken)
        if (refreshToken != null) editor.putString("refresh_$trackerId", refreshToken)
        else editor.remove("refresh_$trackerId")
        if (userId != null) editor.putLong("userid_$trackerId", userId)
        else editor.remove("userid_$trackerId")
        editor.apply()
    }

    fun getTokens(trackerId: Int): TrackerTokens? {
        val accessToken = prefs.getString("access_$trackerId", null) ?: return null
        val refreshToken = prefs.getString("refresh_$trackerId", null)
        val userId = if (prefs.contains("userid_$trackerId")) {
            prefs.getLong("userid_$trackerId", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        } else null
        return TrackerTokens(accessToken = accessToken, refreshToken = refreshToken, userId = userId)
    }

    fun clearTokens(trackerId: Int) {
        prefs.edit()
            .remove("access_$trackerId")
            .remove("refresh_$trackerId")
            .remove("userid_$trackerId")
            .apply()
    }
}

data class TrackerTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: Long? = null,
)
