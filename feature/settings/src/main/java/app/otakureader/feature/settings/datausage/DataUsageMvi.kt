package app.otakureader.feature.settings.datausage

import app.otakureader.domain.model.DataUsageRecord

data class DataUsageState(
    val isLoading: Boolean = true,
    val todayEntries: List<DataUsageRecord> = emptyList(),
    val weekEntries: List<DataUsageRecord> = emptyList(),
    val monthEntries: List<DataUsageRecord> = emptyList(),
    val selectedTab: DataUsageTab = DataUsageTab.TODAY
)

enum class DataUsageTab { TODAY, WEEK, MONTH }

sealed interface DataUsageEvent {
    data class SelectTab(val tab: DataUsageTab) : DataUsageEvent
}
