package app.otakureader.core.ui.selection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Generic selection manager that atomically mutates a [MutableStateFlow] of selected keys.
 *
 * Use in ViewModels to power multi-select UI (long-press to select, bulk actions, etc.).
 * All mutations use [MutableStateFlow.update] for thread-safe atomicity.
 *
 * Example:
 * ```
 * val selection = SelectionManager<Long>()
 *
 * // in UI
 * selection.toggle(item.id)
 *
 * // in ViewModel async action
 * val ids = selection.snapshotAndClear()
 * repo.markRead(ids)
 * ```
 */
class SelectionManager<K> {

    private val _selected = MutableStateFlow<Set<K>>(emptySet())
    val selected: StateFlow<Set<K>> = _selected.asStateFlow()

    /** Emits true whenever at least one item is selected. */
    val isActive: Flow<Boolean> = _selected.map { it.isNotEmpty() }

    /** Emits the current count of selected items. */
    val count: Flow<Int> = _selected.map { it.size }

    /** Toggle a single key: add if absent, remove if present. */
    fun toggle(key: K) {
        _selected.update { current ->
            if (current.contains(key)) current - key else current + key
        }
    }

    /** Select all keys if not all already selected; otherwise clear. */
    fun toggleAll(keys: List<K>) {
        _selected.update { current ->
            val keySet = keys.toSet()
            if (current.containsAll(keySet)) current - keySet else current + keySet
        }
    }

    /** Replace selection with exactly these keys. */
    fun setAll(keys: Set<K>) {
        _selected.value = keys.toSet()
    }

    /** Clear all selected items. */
    fun clear() {
        _selected.value = emptySet()
    }

    /** Invert selection against a full set (select unselected, deselect selected). */
    fun invert(allKeys: List<K>) {
        _selected.update { current ->
            val fullSet = allKeys.toSet()
            fullSet - current
        }
    }

    /**
     * Returns an immutable snapshot of the current selection and **atomically clears** it.
     * Use this before async operations to prevent race conditions where the user modifies
     * selection while the coroutine is suspended.
     */
    fun snapshotAndClear(): Set<K> {
        val snap = _selected.value.toSet()
        _selected.value = emptySet()
        return snap
    }

    /** Read-only snapshot without clearing. */
    fun snapshot(): Set<K> = _selected.value.toSet()

    /** Check if a key is currently selected. */
    operator fun contains(key: K): Boolean = _selected.value.contains(key)
}
