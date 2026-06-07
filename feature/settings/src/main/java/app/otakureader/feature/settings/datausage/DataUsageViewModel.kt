package app.otakureader.feature.settings.datausage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.DownloadPreferences
import app.otakureader.domain.repository.DataUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val WEEK_DAYS = 7L
private const val MONTH_MONTHS = 1L

@HiltViewModel
class DataUsageViewModel @Inject constructor(
    private val repository: DataUsageRepository,
    private val downloadPreferences: DownloadPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(DataUsageState())
    val state: StateFlow<DataUsageState> = _state.asStateFlow()

    init {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(WEEK_DAYS).toString()
        val monthAgo = today.minusMonths(MONTH_MONTHS).toString()

        // Combine usage records with the monthly budget preference so the UI reacts
        // immediately to slider changes without needing a manual refresh.
        combine(
            repository.observeToday(),
            repository.observeSince(weekAgo),
            repository.observeSince(monthAgo),
            downloadPreferences.monthlyDataBudgetMb,
        ) { todayEntries, weekEntries, monthEntries, budgetMb ->
            // Derive per-source breakdown from the month entries.
            // The "source" field isn't tracked in DataUsageRecord yet, so we group by
            // category as a stand-in. When the repository adds source tagging the grouping
            // key can be swapped without touching the UI layer.
            val breakdown = monthEntries
                .groupBy { it.category }
                .map { (category, rows) ->
                    SourceDataUsage(
                        sourceName = category.lowercase().replace('_', ' ')
                            .replaceFirstChar { it.uppercase() },
                        bytesThisMonth = rows.sumOf { it.bytes },
                    )
                }
                .sortedByDescending { it.bytesThisMonth }

            _state.update {
                it.copy(
                    isLoading = false,
                    todayEntries = todayEntries,
                    weekEntries = weekEntries,
                    monthEntries = monthEntries,
                    monthlyDataBudgetMb = budgetMb,
                    sourceBreakdown = breakdown,
                )
            }
        }.catch { _state.update { it.copy(isLoading = false) } }
        .launchIn(viewModelScope)
    }

    fun onEvent(event: DataUsageEvent) {
        when (event) {
            is DataUsageEvent.SelectTab -> _state.update { it.copy(selectedTab = event.tab) }
            is DataUsageEvent.SetMonthlyDataBudgetMb -> {
                // Optimistically update state so the slider feels responsive, then persist.
                _state.update { it.copy(monthlyDataBudgetMb = event.mb) }
                viewModelScope.launch { downloadPreferences.setMonthlyDataBudgetMb(event.mb) }
            }
        }
    }
}
