package app.otakureader.domain.model

import kotlinx.serialization.Serializable

/**
 * Persisted configuration for a single home-screen widget instance.
 *
 * One [WidgetSettings] object is stored per [WidgetType]. The list is serialized to JSON and
 * held in [app.otakureader.core.preferences.WidgetPreferences].
 *
 * @param widgetType     Which widget these settings belong to.
 * @param categoryFilter Library category ID to filter by, or null for all categories.
 * @param countLimit     How many items the widget should show (1–10).
 * @param tapAction      What happens when the user taps a widget row.
 * @param showThumbnails Whether to show manga cover thumbnails in the widget.
 */
@Serializable
data class WidgetSettings(
    val widgetType: WidgetType,
    val categoryFilter: Long? = null,
    val countLimit: Int = 5,
    val tapAction: WidgetTapAction = WidgetTapAction.OPEN_MANGA,
    val showThumbnails: Boolean = true,
) {
    companion object {
        /** Returns a list of defaults, one per widget type. */
        fun defaults(): List<WidgetSettings> = WidgetType.entries.map { WidgetSettings(widgetType = it) }
    }
}

/**
 * The widget types shipped with Otaku Reader.
 *
 * Each value maps to a Glance widget class:
 * - [CONTINUE_READING] → ContinueReadingWidget
 * - [NEW_UPDATES]      → RecentUpdatesWidget
 * - [LIBRARY]          → HomeWidget
 * - [NOW_READING]      → NowReadingWidget
 */
@Serializable
enum class WidgetType {
    CONTINUE_READING,
    NEW_UPDATES,
    LIBRARY,
    NOW_READING,
}

/**
 * Action taken when a user taps a row inside a widget.
 *
 * - [OPEN_MANGA]  → Opens the manga detail / chapter-list screen.
 * - [OPEN_READER] → Jumps straight into the reader at the next unread chapter.
 * - [OPEN_APP]    → Opens the app at its default home screen (library).
 */
@Serializable
enum class WidgetTapAction {
    OPEN_MANGA,
    OPEN_READER,
    OPEN_APP,
}
