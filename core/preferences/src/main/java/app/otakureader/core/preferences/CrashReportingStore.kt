package app.otakureader.core.preferences

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted storage for crash-reporting credentials and the user's opt-in toggle (#952).
 *
 * The DSN is treated like a tracker token — keystore-backed AES-GCM so it never lives in
 * plain SharedPreferences. Both fields default to "disabled" so a fresh install never
 * sends anything until the user explicitly turns it on.
 */
@Singleton
class CrashReportingStore @Inject constructor(context: Context) {

    // Created via EncryptedPrefsFactory so Keystore corruption recovers instead of
    // crash-looping the app (stored secrets are reset; the user re-authenticates).
    private val prefs: SharedPreferences by lazy {
        EncryptedPrefsFactory.create(context.applicationContext, STORE_NAME)
    }

    /** Sentry DSN supplied by the user. Empty/blank → reporting cannot start regardless of opt-in. */
    var dsn: String
        get() = prefs.getString(KEY_DSN, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_DSN, value.trim()).apply()
        }

    /** User has explicitly opted in to crash reporting. Defaults to false. */
    var optedIn: Boolean
        get() = prefs.getBoolean(KEY_OPTED_IN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_OPTED_IN, value).apply()
        }

    /** True when both a DSN is configured and the user has opted in. */
    fun isReportingEnabled(): Boolean = optedIn && dsn.isNotBlank()

    private companion object {
        const val STORE_NAME = "crash_reporting_prefs"
        const val KEY_DSN = "sentry_dsn"
        const val KEY_OPTED_IN = "opted_in"
    }
}
