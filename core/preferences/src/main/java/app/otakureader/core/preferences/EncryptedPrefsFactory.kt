package app.otakureader.core.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Creates [EncryptedSharedPreferences] with recovery from Android Keystore corruption.
 *
 * On some devices the Keystore-backed master key or the encrypted prefs file becomes
 * undecryptable (after OS updates, backup/restore, or vendor Keystore bugs). The stock
 * `EncryptedSharedPreferences.create()` then throws on every app start — a permanent
 * crash loop. This factory recovers instead:
 *
 *  1. Try to open normally.
 *  2. On failure, delete the corrupted prefs file and retry once with a fresh file
 *     (stored secrets are lost — the user re-authenticates — but the app works).
 *  3. If even a fresh encrypted file cannot be created (Keystore itself broken),
 *     fall back to plain SharedPreferences under a distinct name so the app stays
 *     usable; secrets stored there are unencrypted, which is the lesser evil vs.
 *     a permanently crashing app.
 */
object EncryptedPrefsFactory {

    private const val TAG = "EncryptedPrefs"

    fun create(context: Context, fileName: String): SharedPreferences {
        return try {
            createEncrypted(context, fileName)
        } catch (first: Exception) {
            Log.w(TAG, "Encrypted prefs '$fileName' unreadable (${first.message}); resetting file")
            context.deleteSharedPreferences(fileName)
            try {
                createEncrypted(context, fileName)
            } catch (second: Exception) {
                Log.e(TAG, "Keystore unusable for '$fileName' (${second.message}); using plain fallback")
                context.getSharedPreferences("${fileName}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncrypted(context: Context, fileName: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
