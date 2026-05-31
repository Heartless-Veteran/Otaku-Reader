package app.otakureader.feature.settings.datausage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.repository.DataUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DataUsageViewModel @Inject constructor(
    private val repository: DataUsageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DataUsageState())
    val state: StateFlow<DataUsageState> = _state.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        val weekAgo = dateFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time)
        val monthAgo = dateFormat.format(Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time)

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
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: DataUsageEvent) {
        when (event) {
            is DataUsageEvent.SelectTab -> _state.update { it.copy(selectedTab = event.tab) }
        }
    }
}
