package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preference store for reader-related settings such as reading mode, keep-screen-on, and scale.
 * Exposes reactive [Flow] properties and suspend setter functions backed by DataStore.
 */
class ReaderPreferences(private val dataStore: DataStore<Preferences>) {

    // --- Reading Mode ---

    /** Reader display mode ordinal — matches [app.otakureader.feature.reader.model.ReaderMode]:
     *  0 = SINGLE_PAGE, 1 = DUAL_PAGE, 2 = WEBTOON, 3 = SMART_PANELS. */
    val readerMode: Flow<Int> = dataStore.data.map { it[Keys.READER_MODE] ?: 0 }
    suspend fun setReaderMode(value: Int) = dataStore.edit { it[Keys.READER_MODE] = value }

    // --- Screen ---

    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[Keys.KEEP_SCREEN_ON] ?: true }
    suspend fun setKeepScreenOn(value: Boolean) = dataStore.edit { it[Keys.KEEP_SCREEN_ON] = value }

    // --- Scale ---

    val readerScale: Flow<Int> = dataStore.data.map { it[Keys.READER_SCALE] ?: 0 }
    suspend fun setReaderScale(value: Int) = dataStore.edit { it[Keys.READER_SCALE] = value }

    // --- Volume keys ---

    val volumeKeysEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.VOLUME_KEYS_ENABLED] ?: false }
    suspend fun setVolumeKeysEnabled(enabled: Boolean) = dataStore.edit { it[Keys.VOLUME_KEYS_ENABLED] = enabled }

    val volumeKeysInverted: Flow<Boolean> = dataStore.data.map { it[Keys.VOLUME_KEYS_INVERTED] ?: false }
    suspend fun setVolumeKeysInverted(inverted: Boolean) = dataStore.edit { it[Keys.VOLUME_KEYS_INVERTED] = inverted }

    // --- Auto Webtoon Detection ---

    /** Automatically detect webtoon/long-strip manga and switch to webtoon mode */
    val autoWebtoonDetection: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_WEBTOON_DETECTION] ?: true }
    suspend fun setAutoWebtoonDetection(enabled: Boolean) = dataStore.edit { it[Keys.AUTO_WEBTOON_DETECTION] = enabled }

    /** Image aspect ratio threshold for webtoon detection (height/width > this = webtoon) */
    val webtoonDetectionThreshold: Flow<Float> = dataStore.data.map { it[Keys.WEBTOON_THRESHOLD]?.let { it / 100f } ?: 1.5f }
    suspend fun setWebtoonDetectionThreshold(value: Float) = dataStore.edit { it[Keys.WEBTOON_THRESHOLD] = (value * 100).toInt() }

    // --- Page Preload Customization ---

    /** Number of pages to preload before current page (0 = disable) */
    val preloadPagesBefore: Flow<Int> = dataStore.data.map { it[Keys.PRELOAD_PAGES_BEFORE] ?: 2 }
    suspend fun setPreloadPagesBefore(value: Int) = dataStore.edit { it[Keys.PRELOAD_PAGES_BEFORE] = value }

    /** Number of pages to preload after current page (0 = disable) */
    val preloadPagesAfter: Flow<Int> = dataStore.data.map { it[Keys.PRELOAD_PAGES_AFTER] ?: 3 }
    suspend fun setPreloadPagesAfter(value: Int) = dataStore.edit { it[Keys.PRELOAD_PAGES_AFTER] = value }

    // --- Smart Background ---

    /** Enable smart background that adapts to page colors */
    val smartBackground: Flow<Boolean> = dataStore.data.map { it[Keys.SMART_BACKGROUND] ?: false }
    suspend fun setSmartBackground(enabled: Boolean) = dataStore.edit { it[Keys.SMART_BACKGROUND] = enabled }

    // --- Auto Theme Color ---

    /** Enable automatic theme color extraction from manga covers */
    val autoThemeColor: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_THEME_COLOR] ?: true }
    suspend fun setAutoThemeColor(enabled: Boolean) = dataStore.edit { it[Keys.AUTO_THEME_COLOR] = enabled }

    // --- Force Disable Webtoon Zoom ---

    /** Disable zoom gestures in webtoon mode for smoother scrolling */
    val forceDisableWebtoonZoom: Flow<Boolean> = dataStore.data.map { it[Keys.FORCE_DISABLE_WEBTOON_ZOOM] ?: false }
    suspend fun setForceDisableWebtoonZoom(enabled: Boolean) = dataStore.edit { it[Keys.FORCE_DISABLE_WEBTOON_ZOOM] = enabled }

    private object Keys {
        val READER_MODE = intPreferencesKey("reader_mode_setting")
        val KEEP_SCREEN_ON = booleanPreferencesKey("reader_keep_screen_on")
        val READER_SCALE = intPreferencesKey("reader_scale")
        val VOLUME_KEYS_ENABLED = booleanPreferencesKey("reader_volume_keys_enabled")
        val VOLUME_KEYS_INVERTED = booleanPreferencesKey("reader_volume_keys_inverted")
        val AUTO_WEBTOON_DETECTION = booleanPreferencesKey("reader_auto_webtoon_detection")
        val WEBTOON_THRESHOLD = intPreferencesKey("reader_webtoon_threshold")
        val PRELOAD_PAGES_BEFORE = intPreferencesKey("reader_preload_pages_before")
        val PRELOAD_PAGES_AFTER = intPreferencesKey("reader_preload_pages_after")
        val SMART_BACKGROUND = booleanPreferencesKey("reader_smart_background")
        val AUTO_THEME_COLOR = booleanPreferencesKey("reader_auto_theme_color")
        val FORCE_DISABLE_WEBTOON_ZOOM = booleanPreferencesKey("reader_force_disable_webtoon_zoom")
    }
}
