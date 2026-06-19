# MVI Undo Snackbar Pattern

## When to Use This
Any destructive bulk action in Otaku Reader must have an undo snackbar. This is a non-negotiable UX standard. Two patterns exist depending on whether the underlying delete is reversible via a toggle or requires delayed execution.

## Pattern A — Immediate-delete + Re-add

**Use when:** The deletion is a boolean toggle (e.g., `toggleFavoriteManga` removes when favorited, re-adds when not). Calling the toggle twice achieves delete-then-undo.

**Where:** `LibraryViewModel` bulk delete, `UpdatesViewModel` bulk mark-as-read.

### MVI Additions (in `FeatureMvi.kt`)
```kotlin
data class UndoDelete(val ids: Set<Long>) : FeatureEvent()
data class ShowUndoDelete(val count: Int, val ids: Set<Long>) : FeatureEffect()
```

### ViewModel
```kotlin
private fun removeSelected() {
    val ids = selection.snapshotAndClear()
    if (ids.isEmpty()) return
    viewModelScope.launch {
        ids.forEach { runCatching { toggleFavorite(it) } }     // delete immediately
        _effect.send(FeatureEffect.ShowUndoDelete(count = ids.size, ids = ids))
    }
}

private fun undoDelete(ids: Set<Long>) {
    viewModelScope.launch {
        ids.forEach { runCatching { toggleFavorite(it) } }     // re-add via same toggle
    }
}

// In onEvent():
is FeatureEvent.UndoDelete -> undoDelete(event.ids)
```

### Screen (in the effect LaunchedEffect)
```kotlin
is FeatureEffect.ShowUndoDelete -> scope.launch {
    val msg = context.resources.getQuantityString(
        R.plurals.feature_removed_count, effect.count, effect.count)
    val result = snackbarHostState.showSnackbar(
        message = msg,
        actionLabel = context.getString(R.string.undo_action),
        duration = SnackbarDuration.Short,
    )
    if (result == SnackbarResult.ActionPerformed) {
        viewModel.onEvent(FeatureEvent.UndoDelete(effect.ids))
    }
}
```

### Strings
```xml
<plurals name="feature_removed_count">
    <item quantity="one">Removed %1$d item</item>
    <item quantity="other">Removed %1$d items</item>
</plurals>
<string name="undo_action">Undo</string>
```

---

## Pattern B — Delayed-delete + Pending Filter

**Use when:** The deletion is one-way (e.g., `chapterRepository.removeFromHistory()`). Items must visually disappear immediately but the DB write is delayed 4 seconds to allow undo.

**Where:** `HistoryViewModel` batch delete.

**Key idea:** `pendingDeleteIds: MutableStateFlow<Set<Long>>` is used in the UI state combiner to filter out pending items — they disappear from the list as soon as the batch starts, without waiting for the DB.

### Extra ViewModel State
```kotlin
// Alongside other private fields
private var pendingBatchDeleteJob: Job? = null
private var pendingBatchDeleteIds: Set<Long>? = null   // tracks WHICH batch is pending
private val pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())

companion object {
    private const val UNDO_TIMEOUT_MS = 4_000L
}
```

### MVI Additions
```kotlin
data class ShowUndoBatchSnackbar(
    val messageRes: Int,
    val count: Int,
    val chapterIds: Set<Long>,
) : FeatureEffect()

data class UndoBatchRemove(val chapterIds: Set<Long>) : FeatureEvent()
```

### ViewModel — removeSelected()
```kotlin
private fun removeSelected() {
    val selectedIds = _state.value.selectedItems
    if (selectedIds.isEmpty()) return
    clearSelection()

    // Commit any previous pending batch before starting a new one
    val previousIds = pendingBatchDeleteIds
    if (previousIds != null) {
        pendingBatchDeleteJob?.cancel()
        viewModelScope.launch {
            previousIds.forEach {
                try { repo.delete(it) } catch (_: Exception) { }
            }
            pendingDeleteIds.update { it - previousIds }
        }
    }

    pendingBatchDeleteIds = selectedIds
    pendingDeleteIds.update { it + selectedIds }      // hide from UI immediately

    pendingBatchDeleteJob = viewModelScope.launch {
        _effect.send(FeatureEffect.ShowUndoBatchSnackbar(
            messageRes = R.string.feature_removed_count,
            count = selectedIds.size,
            chapterIds = selectedIds,
        ))
        delay(UNDO_TIMEOUT_MS)
        selectedIds.forEach {
            try { repo.delete(it) }
            catch (e: CancellationException) { throw e }   // don't swallow cancellation
            catch (_: Exception) { }
        }
        pendingDeleteIds.update { it - selectedIds }
        if (pendingBatchDeleteIds == selectedIds) {
            pendingBatchDeleteIds = null
            pendingBatchDeleteJob = null
        }
    }
}
```

### ViewModel — undoRemove()
```kotlin
private fun undoRemove(chapterIds: Set<Long>) {
    if (pendingBatchDeleteIds != chapterIds) return   // stale undo guard
    pendingBatchDeleteJob?.cancel()
    pendingBatchDeleteJob = null
    pendingBatchDeleteIds = null
    pendingDeleteIds.update { it - chapterIds }       // restore items to UI
}

// In onEvent():
is FeatureEvent.UndoBatchRemove -> undoRemove(event.chapterIds)
```

### UI State Combiner (how items get filtered)
```kotlin
// In init { } — combine with pendingDeleteIds to hide pending items
combine(_rawItems, pendingDeleteIds) { items, pending ->
    items.filter { it.chapterId !in pending }
}.onEach { filtered ->
    _state.update { it.copy(items = filtered) }
}.launchIn(viewModelScope)
```

### Screen
```kotlin
is FeatureEffect.ShowUndoBatchSnackbar -> scope.launch {
    val msg = context.resources.getQuantityString(
        effect.messageRes, effect.count, effect.count)
    val result = snackbarHostState.showSnackbar(
        message = msg,
        actionLabel = context.getString(R.string.undo_action),
        duration = SnackbarDuration.Short,
    )
    if (result == SnackbarResult.ActionPerformed) {
        viewModel.onEvent(FeatureEvent.UndoBatchRemove(effect.chapterIds))
    }
}
```

---

## Testing Undo Patterns

### Testing Pattern A
```kotlin
@Test
fun `removeSelected deletes immediately and emits undo effect`() = runTest {
    // setup: add items to selection
    viewModel.onEvent(FeatureEvent.Select(setOf(1L, 2L)))
    
    viewModel.effect.test {
        viewModel.onEvent(FeatureEvent.RemoveSelected)
        
        // DB delete happens immediately
        coVerify { mockRepo.toggle(1L) }
        coVerify { mockRepo.toggle(2L) }
        
        // Effect emitted
        val effect = awaitItem()
        assertTrue(effect is FeatureEffect.ShowUndoDelete)
        assertEquals(2, (effect as FeatureEffect.ShowUndoDelete).count)
    }
}

@Test
fun `undoDelete re-adds deleted items`() = runTest {
    val ids = setOf(1L, 2L)
    viewModel.onEvent(FeatureEvent.UndoDelete(ids))
    
    coVerify { mockRepo.toggle(1L) }
    coVerify { mockRepo.toggle(2L) }
}
```

### Testing Pattern B
```kotlin
@Test
fun `removeSelected hides items immediately and emits undo effect`() = runTest {
    viewModel.effect.test {
        viewModel.onEvent(FeatureEvent.RemoveSelected)
        
        val effect = awaitItem()
        assertTrue(effect is FeatureEffect.ShowUndoBatchSnackbar)
        assertEquals(2, (effect as FeatureEffect.ShowUndoBatchSnackbar).count)
        
        // DB delete has NOT happened yet (still in delay)
        coVerify(exactly = 0) { mockRepo.delete(any()) }
        
        // After 4 second delay, DB delete fires
        advanceUntilIdle()
        coVerify { mockRepo.delete(1L) }
        coVerify { mockRepo.delete(2L) }
    }
}

@Test
fun `undoRemove cancels pending delete and restores items`() = runTest {
    val ids = setOf(1L, 2L)
    viewModel.onEvent(FeatureEvent.RemoveSelected)  // starts pending batch
    
    viewModel.onEvent(FeatureEvent.UndoBatchRemove(ids))  // cancel immediately
    advanceUntilIdle()
    
    // DB delete never fires
    coVerify(exactly = 0) { mockRepo.delete(any()) }
    
    // Items reappear in state
    assertFalse(viewModel.uiState.value.items.isEmpty())
}
```

---

## Checklist When Adding a New Undo Snackbar

- [ ] Add `UndoX(ids: Set<Long>)` event and `ShowUndoX(count: Int, ids: Set<Long>)` effect to MVI file
- [ ] Implement the action method in ViewModel using Pattern A or B
- [ ] Implement the undo handler in ViewModel
- [ ] Wire both into `onEvent()` / `handleActionEvent()`
- [ ] Add the `ShowUndoX` case to the effect `LaunchedEffect` in the Screen
- [ ] Add plural string resource `feature_removed_count` and `undo_action` string
- [ ] Write unit tests for: (a) effect emitted, (b) DB call timing, (c) undo cancels delete, (d) stale undo ignored (Pattern B only)
