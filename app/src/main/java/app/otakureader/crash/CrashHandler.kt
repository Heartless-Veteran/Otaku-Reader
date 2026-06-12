package app.otakureader.crash

import android.content.Context
import android.content.SharedPreferences
import app.otakureader.core.preferences.EncryptedPrefsFactory
import app.otakureader.core.preferences.CrashReportingStore

/**
 * Custom [Thread.UncaughtExceptionHandler] that captures fatal crashes, writes the
 * full stack trace to [SharedPreferences], and then delegates to the default handler
 * so the system can terminate the process normally.
 *
 * Install once in [app.otakureader.OtakuReaderApplication.onCreate] via [CrashHandler.install].
 * On the next launch read—and clear—any saved report with [CrashHandler.getAndClearCrashReport].
 */
object CrashHandler {

    private const val PREFS_NAME = "crash_report_prefs"
    private const val KEY_CRASH_REPORT = "crash_report"

    private const val MAX_STACK_TRACE_LENGTH = 65_536
    private const val MAX_TRACE_DEPTH = 30

    /**
     * Replace the default [Thread.UncaughtExceptionHandler] with one that persists
     * the crash report before handing control back to the original handler.
     *
     * Must be called early in [app.otakureader.OtakuReaderApplication.onCreate] so
     * crashes during Hilt graph construction or any other startup code are captured.
     */
    fun install(context: Context, crashReportingStore: CrashReportingStore? = null) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Forward to Sentry first (synchronously flushes) so the report leaves the
                // device before the system kills the process. Falls back to the local-only
                // path when no store is wired or the user hasn't opted in (#952).
                crashReportingStore?.let { CrashReporter.captureIfEnabled(it, throwable) }
                saveReport(appContext, buildReport(thread, throwable))
            } catch (_: Throwable) {
                // Never allow the crash handler itself to throw – always fall through.
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Returns the crash report that was saved during the previous run and immediately
     * removes it from [SharedPreferences] so it is shown exactly once.
     *
     * Returns `null` when no crash was recorded.
     */
    fun getAndClearCrashReport(context: Context): String? {
        val prefs = try { prefs(context) } catch (_: Throwable) { return null }
        val report = prefs.getString(KEY_CRASH_REPORT, null) ?: return null

        // commit() instead of apply() is intentional: the clear must complete
        // synchronously before continuing so the same report is not shown again
        // if the app is killed or crashes during startup.
        return if (prefs.edit().remove(KEY_CRASH_REPORT).commit()) {
            report
        } else {
            null
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val trace = throwable.stackTraceToString()
        val sanitized = sanitizeTrace(trace)
        val body = if (sanitized.length > MAX_STACK_TRACE_LENGTH) {
            sanitized.take(MAX_STACK_TRACE_LENGTH) + "\n… (truncated)"
        } else {
            sanitized
        }
        return "Thread: ${thread.name}\n\n$body"
    }

    // Matches key=value or key: value where key is a sensitive term; replaces only the value.
    private val sensitiveValuePattern = Regex(
        """(?i)(token|api[_-]?key|password|secret|credential|authorization)(\s*[:=]\s*)\S+"""
    )

    private fun sanitizeTrace(trace: String): String {
        return trace.lines()
            .take(MAX_TRACE_DEPTH)
            .joinToString("\n") { line ->
                // Replace absolute filesystem paths before the app package to avoid
                // leaking device-specific paths (e.g. /data/data/...).
                val stripped = line.replace(Regex("/[^\\s]*app\\.otakureader"), ".../app.otakureader")
                // Stack frames (lines starting with "at ") only need path stripping;
                // redacting by keyword would hide class/method names like getAccessToken.
                if (stripped.trimStart().startsWith("at ")) stripped
                else sensitiveValuePattern.replace(stripped) { m -> "${m.groupValues[1]}${m.groupValues[2]}[redacted]" }
            }
    }

    private fun saveReport(context: Context, report: String) {
        // commit() instead of apply() is intentional: the write must complete
        // synchronously before the process is killed by the default handler.
        prefs(context).edit().putString(KEY_CRASH_REPORT, report).commit()
    }

    @Volatile private var cachedPrefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        // EncryptedPrefsFactory recovers from Keystore corruption — essential here:
        // a crash handler that itself crashes hides the original crash report.
        return cachedPrefs ?: synchronized(this) {
            cachedPrefs ?: EncryptedPrefsFactory.create(context.applicationContext, PREFS_NAME)
                .also { cachedPrefs = it }
        }
    }
}
