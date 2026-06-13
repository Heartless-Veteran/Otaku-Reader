package app.otakureader.crash

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import app.otakureader.core.preferences.EncryptedPrefsFactory
import app.otakureader.core.preferences.CrashReportingStore
import java.io.File

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

    @Volatile private var installed = false
    @Volatile private var reportingStore: CrashReportingStore? = null

    /**
     * Replace the default [Thread.UncaughtExceptionHandler] with one that persists the crash
     * report before handing control back to the original handler.
     *
     * Idempotent: the first call (ideally from [Application.attachBaseContext], which runs
     * BEFORE all ContentProviders such as Sentry/FileProvider/androidx.startup) installs the
     * handler so even a ContentProvider crash is captured. A later call from onCreate simply
     * attaches the [crashReportingStore] for Sentry forwarding without re-wrapping.
     */
    fun install(context: Context, crashReportingStore: CrashReportingStore? = null) {
        // getApplicationContext() can be null during attachBaseContext — fall back to the
        // passed context, which is valid for prefs/file/MediaStore operations.
        val appContext = context.applicationContext ?: context
        if (crashReportingStore != null) reportingStore = crashReportingStore
        if (installed) return
        installed = true
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Forward to Sentry first (synchronously flushes) so the report leaves the
                // device before the system kills the process. Falls back to the local-only
                // path when no store is wired or the user hasn't opted in (#952).
                reportingStore?.let { CrashReporter.captureIfEnabled(it, throwable) }
                val report = buildReport(thread, throwable)
                saveReport(appContext, report)
                // Also write a plaintext copy to the public Downloads folder. When a crash
                // happens before any Activity can show the saved report (ContentProvider or
                // Application.onCreate), this file is the only way a non-developer (no adb)
                // can retrieve the trace — just open Downloads in the Files app.
                dumpToDownloads(appContext, report)
            } catch (_: Throwable) {
                // Never allow the crash handler itself to throw – always fall through.
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Writes the crash report to the device's public Downloads folder as a plaintext file
     * so it can be opened with any file manager without a computer. Best-effort: any failure
     * is swallowed because the crash handler must never throw.
     */
    private fun dumpToDownloads(context: Context, report: String) {
        val fileName = "otaku_crash_${System.currentTimeMillis()}.txt"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
                resolver.openOutputStream(uri)?.use { it.write(report.toByteArray()) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloads, fileName).writeText(report)
            }
        } catch (_: Throwable) {
            // Swallow: a diagnostics write must never mask or replace the original crash.
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
