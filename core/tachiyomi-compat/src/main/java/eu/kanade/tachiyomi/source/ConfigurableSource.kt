package eu.kanade.tachiyomi.source

import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.model.FilterList

/**
 * Minimal stub for Tachiyomi extension compatibility.
 *
 * Extensions from Keiyoushi/Komikku that implement [ConfigurableSource] require
 * this interface in the host classloader so the app can detect and use it via
 * `is ConfigurableSource` checks. The app does not call [setupPreferenceScreen] —
 * it just needs the type to exist to prevent [NoClassDefFoundError] at load time.
 *
 * @see ExtensionLoader for the load-time type-check
 */
interface ConfigurableSource : Source {

    /**
     * Returns the readable name of the extension configuration.
     *
     * Not used by Otaku Reader — kept for API parity with Tachiyomi extensions.
     */
    fun getPreferenceScreen(): String

    /**
     * Sets up the extension's preference screen.
     *
     * Not invoked by Otaku Reader — kept for API parity with Tachiyomi extensions.
     */
    fun setupPreferenceScreen(screen: Any)
}
