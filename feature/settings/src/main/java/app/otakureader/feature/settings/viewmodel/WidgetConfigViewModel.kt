package app.otakureader.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.WidgetPreferences
import app.otakureader.domain.model.WidgetSettings
import app.otakureader.domain.usecase.GetCategoriesUseCase
import app.otakureader.feature.settings.WidgetConfigEffect
import app.otakureader.feature.settings.WidgetConfigIntent
import app.otakureader.feature.settings.WidgetConfigState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Widget Configuration screen.
 *
 * Responsibilities:
 * 1. Observe [WidgetPreferences.widgetSettings] and [GetCategoriesUseCase] reactively.
 * 2. Translate [WidgetConfigIntent] actions into DataStore writes.
 * 3. Emit [WidgetConfigEffect] one-shot events (e.g. a save-confirmation snackbar).
 *
 * Why a dedicated ViewModel instead of folding into the existing SettingsViewModel?
 * The settings ViewModel is already large (handling dozens of concerns). Following the
 * pattern established by [AppearanceViewModel] and [ReaderSettingsViewModel], each settings
 * section that has non-trivial async data gets its own focused ViewModel. This also makes
 * the widget config screen independently testable.
 */
@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    private val widgetPreferences: WidgetPreferences,
    private val getCategoriesUseCase: GetCategoriesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(WidgetConfigState())
    val state: StateFlow<WidgetConfigState> = _state.asStateFlow()

    private val _effect = Channel<WidgetConfigEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        // Combine widget settings + categories into a single state snapshot.
        // Using `combine` here means the UI automatically reacts if either changes —
        // for example if the user creates a new category while this screen is open.
        viewModelScope.launch {
            combine(
                widgetPreferences.widgetSettings,
                getCategoriesUseCase(),
            ) { settings, categories ->
                _state.update {
                    it.copy(
                        widgetSettingsList = settings,
                        categories = categories,
                        isLoading = false,
                    )
                }
            }.collect { }
        }
    }

    fun onIntent(intent: WidgetConfigIntent) {
        viewModelScope.launch {
            when (intent) {
                is WidgetConfigIntent.SetCountLimit -> updateWidget(intent.widgetType) {
                    it.copy(countLimit = intent.limit.coerceIn(1, 10))
                }
                is WidgetConfigIntent.SetTapAction -> updateWidget(intent.widgetType) {
                    it.copy(tapAction = intent.action)
                }
                is WidgetConfigIntent.SetCategoryFilter -> updateWidget(intent.widgetType) {
                    it.copy(categoryFilter = intent.categoryId)
                }
                is WidgetConfigIntent.SetShowThumbnails -> updateWidget(intent.widgetType) {
                    it.copy(showThumbnails = intent.show)
                }
            }
        }
    }

    /**
     * Applies [transform] to the [WidgetSettings] entry for [type], saves the resulting list,
     * and emits a [WidgetConfigEffect.SettingsSaved] effect.
     *
     * This helper is the single place where writes happen, which makes it easy to add
     * debouncing or validation later without touching each individual branch.
     */
    private suspend fun updateWidget(
        type: app.otakureader.domain.model.WidgetType,
        transform: (WidgetSettings) -> WidgetSettings,
    ) {
        val current = _state.value.widgetSettingsList.toMutableList()
        val index = current.indexOfFirst { it.widgetType == type }
        if (index < 0) return   // guard: should never happen with defaults() seeding

        current[index] = transform(current[index])
        widgetPreferences.setWidgetSettings(current)
        _effect.send(WidgetConfigEffect.SettingsSaved)
    }
}
