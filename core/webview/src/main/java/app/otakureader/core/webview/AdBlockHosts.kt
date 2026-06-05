package app.otakureader.core.webview

import android.content.Context

/**
 * Lazily loads the ad-block host list from assets/adblock_hosts.txt.
 *
 * The file uses OISD-small format: one plain hostname per line.
 * Lines starting with '#' and blank lines are ignored.
 *
 * The set is computed once and cached for the lifetime of the process.
 */
internal object AdBlockHosts {

    @Volatile
    private var _hosts: Set<String>? = null

    fun get(context: Context): Set<String> {
        return _hosts ?: synchronized(this) {
            _hosts ?: loadHosts(context).also { _hosts = it }
        }
    }

    private fun loadHosts(context: Context): Set<String> = try {
        context.assets.open("adblock_hosts.txt").bufferedReader().use { reader ->
            reader.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toHashSet()
        }
    } catch (e: Exception) {
        emptySet()
    }
}
