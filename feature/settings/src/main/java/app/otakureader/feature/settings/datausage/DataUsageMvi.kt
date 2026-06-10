package app.otakureader.feature.settings.datausage

import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.DataUsageRecord

/**
 * Per-source data consumption for the current calendar month.
 *
 * This data class lives in the feature layer rather than domain because it is derived from
 * [DataUsageRecord] by the ViewModel — there is no separate repository query for it yet.
 * When the repository grows dedicated per-source tracking, the domain model can absorb it
 * and this class can be removed.
 */
data class SourceDataUsage(val sourceName: String, val bytesThisMonth: Long)

data class DataUsageState(
    val isLoading: Boolean = true,
    val todayEntries: List<DataUsageRecord> = emptyList(),
    val weekEntries: List<DataUsageRecord> = emptyList(),
    val monthEntries: List<DataUsageRecord> = emptyList(),
    val selectedTab: DataUsageTab = DataUsageTab.TODAY,
    /**
     * Monthly data budget in MB. 0 = unlimited (no progress bar shown).
     * Sourced from [DownloadPreferences.monthlyDataBudgetMb].
     */
    val monthlyDataBudgetMb: Int = 0,
    /**
     * Per-source breakdown for the current calendar month.
     * Empty until the repository supplies source-tagged records.
     */
    val sourceBreakdown: List<SourceDataUsage> = emptyList(),
) : UiState

enum class DataUsageTab { TODAY, WEEK, MONTH }

sealed interface DataUsageEvent : UiEvent {
    data class SelectTab(val tab: DataUsageTab) : DataUsageEvent
    /** Update (and persist) the monthly data budget. [mb] == 0 means unlimited. */
    data class SetMonthlyDataBudgetMb(val mb: Int) : DataUsageEvent
}
