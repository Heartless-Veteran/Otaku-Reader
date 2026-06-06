package app.otakureader.feature.settings.navorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.NavTab
import app.otakureader.core.preferences.NavOrderPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavOrderViewModel @Inject constructor(
    private val prefs: NavOrderPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(NavOrderState())
    val state: StateFlow<NavOrderState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.tabOrder.collect { order ->
                _state.update { it.copy(tabs = order) }
            }
        }
    }

    fun onEvent(event: NavOrderEvent) {
        when (event) {
            is NavOrderEvent.MoveUp -> move(event.index, -1)
            is NavOrderEvent.MoveDown -> move(event.index, +1)
            is NavOrderEvent.Reset -> reset()
        }
    }

    private fun move(index: Int, delta: Int) {
        val current = _state.value.tabs.toMutableList()
        val target = index + delta
        if (target < 0 || target >= current.size) return
        val tmp = current[index]; current[index] = current[target]; current[target] = tmp
        persist(current)
    }

    private fun reset() {
        persist(NavTab.entries)
    }

    private fun persist(order: List<NavTab>) {
        _state.update { it.copy(tabs = order) }
        viewModelScope.launch { prefs.setTabOrder(order) }
    }
}
