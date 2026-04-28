package app.otakureader.crash

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports the locally captured crash report as a text file that can be shared
 * via email, messaging, or uploaded to a GitHub issue.
 *
 * Call from Settings → Advanced → Export logs.
 */
object CrashLogExporter {

    /**
     * Exports the current crash report (if any) to a file and returns an [Intent]
     * that can be started with [Context.startActivity] to let the user choose
     * a sharing target.
     *
     * Returns null when no crash report exists.
     */
    fun export(context: Context): Intent? {
        val report = CrashHandler.getAndClearCrashReport(context) ?: return null

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val filename = "otaku_reader_crash_$timestamp.txt"
        val file = File(context.cacheDir, filename)

        file.writeText(buildString {
            appendLine("Otaku Reader Crash Report")
            appendLine("Generated: $timestamp")
            appendLine("App version: ${getAppVersion(context)}")
            appendLine("Android version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine()
            appendLine("---")
            appendLine()
            append(report)
        })

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Otaku Reader Crash Report ($timestamp)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        } catch (_: Exception) {
            "unknown"
        }
    }
}
