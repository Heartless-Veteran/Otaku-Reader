package app.otakureader.feature.settings.datausage

import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.DataUsageRecord

data class DataUsageState(
    val isLoading: Boolean = true,
    val todayEntries: List<DataUsageRecord> = emptyList(),
    val weekEntries: List<DataUsageRecord> = emptyList(),
    val monthEntries: List<DataUsageRecord> = emptyList(),
    val selectedTab: DataUsageTab = DataUsageTab.TODAY
) : UiState

enum class DataUsageTab { TODAY, WEEK, MONTH }

sealed interface DataUsageEvent : UiEvent {
    data class SelectTab(val tab: DataUsageTab) : DataUsageEvent
}
