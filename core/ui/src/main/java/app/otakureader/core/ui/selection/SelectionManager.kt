package app.otakureader.core.ui.selection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reusable, thread-safe selection state manager for multi-select screens.
 *
 * All mutations use [MutableStateFlow.update] so they are atomic — no external
 * synchronization required. Use [snapshot] to capture the selected set before
 * launching a coroutine, then [clear] immediately so the UI reflects the cleared
 * state while the async work runs in the background.
 *
 * Usage (in a ViewModel):
 * ```kotlin
 * private val selectionManager = SelectionManager<Long>()
 *
 * // Expose to UI via derived state
 * val selectedIds: StateFlow<Set<Long>> = selectionManager.selected
 *
 * // Toggle on click when in selection mode
 * selectionManager.toggle(mangaId)
 *
 * // Atomic snapshot + clear before async work
 * val ids = selectionManager.snapshot()
 * selectionManager.clear()
 * viewModelScope.launch { /* use ids */ }
 * ```
 *
 * Adding multi-select to a new screen: inject or create a [SelectionManager]<ID>,
 * expose [selected] to the UI, and call the mutation functions from event handlers.
 */
class SelectionManager<T> {

    private val _selected = MutableStateFlow<Set<T>>(emptySet())
    val selected: StateFlow<Set<T>> = _selected.asStateFlow()

    val isActive: Boolean get() = _selected.value.isNotEmpty()
    val count: Int get() = _selected.value.size

    fun toggle(item: T) {
        _selected.update { current ->
            if (current.contains(item)) current - item else current + item
        }
    }

    fun toggleAll(items: Collection<T>) {
        _selected.update { current ->
            if (current.containsAll(items)) emptySet() else items.toSet()
        }
    }

    fun clear() {
        _selected.update { emptySet() }
    }

    fun invert(allItems: Collection<T>) {
        _selected.update { current -> allItems.toSet() - current }
    }

    /** Returns an immutable snapshot of the currently selected items. */
    fun snapshot(): Set<T> = _selected.value.toSet()
}
