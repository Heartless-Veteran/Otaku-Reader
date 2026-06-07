package app.otakureader.feature.settings

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.Category
import app.otakureader.domain.model.WidgetSettings
import app.otakureader.domain.model.WidgetTapAction
import app.otakureader.domain.model.WidgetType

/**
 * MVI contracts for [WidgetConfigurationScreen].
 *
 * State is an immutable snapshot; all mutations go through [WidgetConfigIntent] → reducer.
 */
data class WidgetConfigState(
    /** One settings object per [WidgetType]. Ordered by [WidgetType.ordinal]. */
    val widgetSettingsList: List<WidgetSettings> = WidgetSettings.defaults(),
    /** Library categories available for the category-filter dropdown. */
    val categories: List<Category> = emptyList(),
    /** True while the initial settings are being loaded from DataStore. */
    val isLoading: Boolean = true,
) : UiState

sealed interface WidgetConfigIntent : UiEvent {
    /**
     * Updates the [countLimit] for the given [widgetType].
     * The slider emits a Float (1f–10f); the ViewModel rounds it to an Int.
     */
    data class SetCountLimit(val widgetType: WidgetType, val limit: Int) : WidgetConfigIntent

    /** Changes the tap action for one widget. */
    data class SetTapAction(val widgetType: WidgetType, val action: WidgetTapAction) : WidgetConfigIntent

    /**
     * Sets (or clears) the category filter for one widget.
     * Passing null means "All categories".
     */
    data class SetCategoryFilter(val widgetType: WidgetType, val categoryId: Long?) : WidgetConfigIntent

    /** Toggles the thumbnail visibility for one widget. */
    data class SetShowThumbnails(val widgetType: WidgetType, val show: Boolean) : WidgetConfigIntent
}

sealed interface WidgetConfigEffect : UiEffect {
    /** Shown after settings are saved to DataStore. */
    data object SettingsSaved : WidgetConfigEffect
}
