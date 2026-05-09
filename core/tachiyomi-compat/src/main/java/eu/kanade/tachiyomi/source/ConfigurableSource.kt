package eu.kanade.tachiyomi.source

/**
 * Stub interface for Tachiyomi extensions that expose per-source user preferences.
 *
 * Extensions from Keiyoushi and Komikku may declare `implements ConfigurableSource`
 * so the host app can detect and surface source-level settings. Without this stub,
 * the class loader would throw NoClassDefFoundError when instantiating such extensions,
 * because the interface must exist in the host's class path for the `is ConfigurableSource`
 * check to succeed.
 *
 * The app does not currently render source preferences, but recognising the interface
 * prevents load failures for the ~200 extensions that implement it.
 */
interface ConfigurableSource
