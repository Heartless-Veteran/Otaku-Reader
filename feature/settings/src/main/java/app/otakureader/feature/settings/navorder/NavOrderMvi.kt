package app.otakureader.feature.settings.navorder

import app.otakureader.core.preferences.NavTab

/**
 * Pairs a tab with its current visibility. Using a wrapper avoids modifying the NavTab enum,
 * which lives in core and must remain stable for extension compatibility.
 */
data class NavTabEntry(val tab: NavTab, val isVisible: Boolean = true)

data class NavOrderState(
    val tabs: List<NavTabEntry> = emptyList(),
)

sealed interface NavOrderEvent {
    data class MoveUp(val index: Int) : NavOrderEvent
    data class MoveDown(val index: Int) : NavOrderEvent
    /** Swap the entry at [from] with the entry at [to] (drag-and-drop reorder). */
    data class MoveTab(val from: Int, val to: Int) : NavOrderEvent
    /** Flip the visibility flag for the entry at [index]. */
    data class ToggleTabVisibility(val index: Int) : NavOrderEvent
    data class Reset(val unit: Unit = Unit) : NavOrderEvent
}
