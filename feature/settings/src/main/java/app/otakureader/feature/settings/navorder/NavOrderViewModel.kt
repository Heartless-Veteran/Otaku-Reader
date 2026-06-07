package app.otakureader.feature.settings.navorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.NavTab
import app.otakureader.core.preferences.NavOrderPreferences
import app.otakureader.core.preferences.NavTabPreferenceEntry
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
        // Collect the persisted order + visibility and map it to UI state.
        viewModelScope.launch {
            prefs.tabOrder.collect { order ->
                _state.update { it.copy(tabs = order.map { e -> NavTabEntry(e.tab, e.isVisible) }) }
            }
        }
    }

    fun onEvent(event: NavOrderEvent) {
        when (event) {
            is NavOrderEvent.MoveUp -> move(event.index, -1)
            is NavOrderEvent.MoveDown -> move(event.index, +1)
            is NavOrderEvent.MoveTab -> moveTab(event.from, event.to)
            is NavOrderEvent.ToggleTabVisibility -> toggleVisibility(event.index)
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

    /**
     * Swaps [from] and [to] in the list. Called by the drag-and-drop gesture handler once
     * the dragged item crosses the midpoint of a neighbour.
     */
    private fun moveTab(from: Int, to: Int) {
        val current = _state.value.tabs.toMutableList()
        if (from < 0 || from >= current.size || to < 0 || to >= current.size) return
        val tmp = current[from]; current[from] = current[to]; current[to] = tmp
        persist(current)
    }

    /**
     * Flips the [NavTabEntry.isVisible] flag for the entry at [index]. The tab remains in the
     * list so the user can always re-enable it — hiding only removes it from the bottom nav.
     */
    private fun toggleVisibility(index: Int) {
        val current = _state.value.tabs.toMutableList()
        if (index < 0 || index >= current.size) return
        current[index] = current[index].copy(isVisible = !current[index].isVisible)
        persist(current)
    }

    private fun reset() {
        // Restore default order with all tabs visible.
        persist(NavTab.entries.map { NavTabEntry(it, isVisible = true) })
    }

    private fun persist(order: List<NavTabEntry>) {
        _state.update { it.copy(tabs = order) }
        viewModelScope.launch {
            prefs.setTabOrder(order.map { NavTabPreferenceEntry(it.tab, it.isVisible) })
        }
    }
}
