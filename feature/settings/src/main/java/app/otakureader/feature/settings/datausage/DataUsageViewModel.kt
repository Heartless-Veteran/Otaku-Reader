package app.otakureader.feature.settings.datausage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.repository.DataUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

private const val WEEK_DAYS = 7L
private const val MONTH_MONTHS = 1L

@HiltViewModel
class DataUsageViewModel @Inject constructor(
    private val repository: DataUsageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DataUsageState())
    val state: StateFlow<DataUsageState> = _state.asStateFlow()

    init {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(WEEK_DAYS).toString()
        val monthAgo = today.minusMonths(MONTH_MONTHS).toString()

        combine(
            repository.observeToday(),
            repository.observeSince(weekAgo),
            repository.observeSince(monthAgo)
        ) { todayEntries, weekEntries, monthEntries ->
            _state.update {
                it.copy(
                    isLoading = false,
                    todayEntries = todayEntries,
                    weekEntries = weekEntries,
                    monthEntries = monthEntries
                )
            }
        }.catch { _state.update { it.copy(isLoading = false) } }
        .launchIn(viewModelScope)
    }

    fun onEvent(event: DataUsageEvent) {
        when (event) {
            is DataUsageEvent.SelectTab -> _state.update { it.copy(selectedTab = event.tab) }
        }
    }
}
