package com.otakureader.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otakureader.data.model.ReaderMode
import com.otakureader.ui.theme.AppTheme
import com.otakureader.ui.theme.AccentPurple
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private object Keys {
    val THEME = stringPreferencesKey("theme")
    val ACCENT = longPreferencesKey("accent")
    val READER_MODE = stringPreferencesKey("reader_mode")
    val BRIGHTNESS = floatPreferencesKey("brightness")
    val SHOW_PROGRESS = booleanPreferencesKey("show_progress")
    val COVER_DENSITY = stringPreferencesKey("cover_density")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
}

data class AppSettings(
    val theme: AppTheme = AppTheme.DARK,
    val accent: Color = AccentPurple,
    val readerMode: ReaderMode = ReaderMode.WEBTOON,
    val brightness: Float = 0.8f,
    val showProgress: Boolean = true,
    val coverDensity: CoverDensity = CoverDensity.COMFORTABLE,
    val dynamicColor: Boolean = false,
)

enum class CoverDensity { COMPACT, COMFORTABLE, LIST }

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val dataStore = app.dataStore

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.catch { emit(emptyPreferences()) }.collect { prefs ->
                _settings.value = AppSettings(
                    theme = AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.DARK.name),
                    accent = prefs[Keys.ACCENT]?.let { Color(it.toULong()) } ?: AccentPurple,
                    readerMode = ReaderMode.valueOf(prefs[Keys.READER_MODE] ?: ReaderMode.WEBTOON.name),
                    brightness = prefs[Keys.BRIGHTNESS] ?: 0.8f,
                    showProgress = prefs[Keys.SHOW_PROGRESS] ?: true,
                    coverDensity = CoverDensity.valueOf(prefs[Keys.COVER_DENSITY] ?: CoverDensity.COMFORTABLE.name),
                    dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
                )
            }
        }
    }

    fun setTheme(theme: AppTheme) = save { it[Keys.THEME] = theme.name }
    fun setAccent(color: Color) = save { it[Keys.ACCENT] = color.value.toLong() }
    fun setReaderMode(mode: ReaderMode) = save { it[Keys.READER_MODE] = mode.name }
    fun setBrightness(v: Float) = save { it[Keys.BRIGHTNESS] = v }
    fun setShowProgress(v: Boolean) = save { it[Keys.SHOW_PROGRESS] = v }
    fun setCoverDensity(d: CoverDensity) = save { it[Keys.COVER_DENSITY] = d.name }
    fun setDynamicColor(v: Boolean) = save { it[Keys.DYNAMIC_COLOR] = v }

    private fun save(block: (MutablePreferences) -> Unit) {
        viewModelScope.launch { dataStore.edit(block) }
    }
}
