package app.otakureader.feature.settings.navorder

import app.otakureader.core.preferences.NavTab

data class NavOrderState(
    val tabs: List<NavTab> = emptyList(),
)

sealed interface NavOrderEvent {
    data class MoveUp(val index: Int) : NavOrderEvent
    data class MoveDown(val index: Int) : NavOrderEvent
    data class Reset(val unit: Unit = Unit) : NavOrderEvent
}
