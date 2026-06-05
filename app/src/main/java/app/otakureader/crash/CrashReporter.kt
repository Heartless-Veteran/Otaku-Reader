package app.otakureader.crash

import android.content.Context
import app.otakureader.core.preferences.CrashReportingStore
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

/**
 * Thin façade around the Sentry SDK (#952).
 *
 * - Initialization is opt-in: the SDK is only started when the user has both supplied a DSN
 *   and flipped the opt-in toggle in Settings → Privacy. With no DSN the SDK stays inert and
 *   no events leave the device.
 * - Forwarding [Throwable]s goes through [Sentry.captureException]; we deliberately do NOT
 *   install Sentry's own uncaught-exception handler. The existing [CrashHandler] keeps writing
 *   the local encrypted report (used for the on-device "previous-run crash" surface) and
 *   simply CC's Sentry when reporting is enabled.
 * - PII redaction continues to happen via [CrashHandler.sanitizeTrace] for the local report.
 *   Sentry's own scrubbing handles in-flight redaction of common patterns (tokens, emails)
 *   before transmission, but callers should still avoid passing user-identifying breadcrumbs.
 */
object CrashReporter {

    @Volatile
    private var initialized = false

    /**
     * Initialize Sentry if the user has opted in and supplied a DSN.
     * Safe to call multiple times — re-initialization with the same config is a no-op.
     */
    fun initialize(context: Context, store: CrashReportingStore) {
        if (initialized) return
        if (!store.isReportingEnabled()) return
        val dsn = store.dsn
        try {
            val appContext = context.applicationContext
            val packageInfo = try {
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            } catch (_: Throwable) {
                null
            }
            val versionName = packageInfo?.versionName ?: "unknown"
            val isDebuggable = (
                appContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
                ) != 0
            SentryAndroid.init(appContext) { options ->
                options.dsn = dsn
                options.environment = if (isDebuggable) "debug" else "release"
                options.release = "${appContext.packageName}@$versionName"
                // We forward via captureException() ourselves; don't double-report.
                options.isEnableUncaughtExceptionHandler = false
                // Don't attach the user's device ID — keep crash data minimal.
                options.isSendDefaultPii = false
                // No NDK / ANR uploads in the first phase — bring those in once the
                // baseline DSN-only flow is validated against the user's chosen backend.
                options.isAnrEnabled = false
            }
            initialized = true
        } catch (_: Throwable) {
            // Sentry init must never crash the app. A bad DSN just keeps reporting disabled.
            initialized = false
        }
    }

    /**
     * Forward a captured exception to Sentry if reporting is initialized and currently enabled.
     * No-op when disabled — safe to call unconditionally from [CrashHandler].
     */
    fun captureIfEnabled(store: CrashReportingStore, throwable: Throwable) {
        if (!initialized || !store.isReportingEnabled()) return
        try {
            Sentry.captureException(throwable)
            // Flush synchronously so the report survives the imminent process death.
            Sentry.flush(FLUSH_TIMEOUT_MS)
        } catch (_: Throwable) {
            // Never propagate Sentry failures up from the crash path.
        }
    }

    private const val FLUSH_TIMEOUT_MS = 2_000L
}
