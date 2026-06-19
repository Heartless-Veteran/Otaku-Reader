# Android & Kotlin Conventions

## Language Rules
- **Kotlin 2.3.21** with all stable language features enabled
- Prefer `val` over `var`
- Prefer immutability — `List` over `MutableList`, `StateFlow` over mutable state
- Use `sealed class` / `sealed interface` for restricted hierarchies (events, states, results, effects)
- Use `data class` for state representations
- Avoid `!!` — use `?.let`, `?: return`, or explicit null handling; prefer `?: fallback` over null assertion

## Compose Patterns
- UI is 100% Jetpack Compose — no XML layouts except manifest/config
- Theme defined in `core/ui` — always use theme values, never hardcode colors/sizes
- `MaterialTheme.colorScheme` and `MaterialTheme.typography` for all styling
- Collect state with `collectAsStateWithLifecycle()`, not `collectAsState()` — the lifecycle-aware version avoids updates when the app is in the background

## State Management
```kotlin
// Good — immutable state, single source of truth
data class ReaderState(
    val currentPage: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

// ViewModel pattern
class ReaderViewModel @Inject constructor(...) : ViewModel() {
    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()
    
    private val _effect = Channel<ReaderEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    fun onEvent(event: ReaderEvent) {
        when (event) {
            is ReaderEvent.NextPage -> _state.update { it.copy(currentPage = it.currentPage + 1) }
            is ReaderEvent.ShowError -> viewModelScope.launch { _effect.send(ReaderEffect.ShowSnackbar(event.message)) }
        }
    }
}
```

## Stable Flows in Compose
Never create a new flow instance inside a composable body — it will resubscribe on every recomposition.
Wrap with `remember(key)`:

```kotlin
// Good — stable, recreates only when repository changes (never in practice)
val activeDownloadCount by remember(downloadRepository) {
    downloadRepository.observeDownloads()
        .map { downloads -> downloads.count { it.isActive } }
        .distinctUntilChanged()
}.collectAsStateWithLifecycle(initialValue = 0)

// Bad — new flow instance every recomposition → excessive resubscription
val activeDownloadCount by downloadRepository.observeDownloads()
    .map { it.count { d -> d.isActive } }
    .collectAsStateWithLifecycle(0)
```

## Coroutines & Flow
- `viewModelScope` for UI-bound work — never `GlobalScope`
- `CoroutineScope` injected via Hilt (`@ApplicationScope`) for background work (downloads, sync)
- `Flow` for data streams; `StateFlow` for UI state
- `async/await` for parallel independent operations (e.g., loading multiple settings)
- Never block the main thread with DataStore reads — batch them:

```kotlin
// Good — parallel DataStore reads
coroutineScope {
    val setting1 = async { dataStore.setting1.first() }
    val setting2 = async { dataStore.setting2.first() }
    ReaderSettings(
        setting1 = setting1.await(),
        setting2 = setting2.await(),
    )
}

// Bad — sequential, blocks cold start
val setting1 = dataStore.setting1.first()
val setting2 = dataStore.setting2.first()
```

## MVI Undo Snackbar Patterns
Any destructive bulk action must have an undo snackbar. Two patterns are established:

**Pattern A — Immediate-delete + Re-add** (works when the delete is a boolean toggle):
```kotlin
private fun removeSelected() {
    val ids = selection.snapshotAndClear()
    viewModelScope.launch {
        ids.forEach { runCatching { toggleFavorite(it) } }  // delete immediately
        _effect.send(Effect.ShowUndoDelete(count = ids.size, ids = ids))
    }
}
private fun undoDelete(ids: Set<Long>) {
    viewModelScope.launch { ids.forEach { runCatching { toggleFavorite(it) } } }  // re-add via toggle
}
```

**Pattern B — Delayed-delete + Pending Filter** (for non-toggle deletes like history removal):
```kotlin
private var pendingBatchDeleteJob: Job? = null
private var pendingBatchDeleteIds: Set<Long>? = null
val pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())

private fun removeSelected() {
    val ids = _state.value.selectedItems
    // Commit any previous pending batch before starting a new one
    pendingBatchDeleteIds?.let { prev ->
        pendingBatchDeleteJob?.cancel()
        viewModelScope.launch { prev.forEach { repo.delete(it) }; pendingDeleteIds.update { it - prev } }
    }
    pendingBatchDeleteIds = ids
    pendingDeleteIds.update { it + ids }  // hide from UI immediately
    pendingBatchDeleteJob = viewModelScope.launch {
        _effect.send(Effect.ShowUndoDelete(count = ids.size, ids = ids))
        delay(UNDO_TIMEOUT_MS)
        ids.forEach { repo.delete(it) }
        pendingDeleteIds.update { it - ids }
        pendingBatchDeleteIds = null
    }
}
private fun undoDelete(ids: Set<Long>) {
    if (pendingBatchDeleteIds != ids) return  // stale undo guard — different batch already started
    pendingBatchDeleteJob?.cancel()
    pendingBatchDeleteIds = null
    pendingDeleteIds.update { it - ids }  // restore items to UI
}
```

## Dependency Injection (Hilt)
```kotlin
// Repository binding
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindMangaRepository(impl: MangaRepositoryImpl): MangaRepository
}

// ViewModel
@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getMangaDetails: GetMangaDetailsUseCase,
    private val downloadChapter: DownloadChapterUseCase,
) : ViewModel() { ... }
```

## Performance Rules
- Coil memory cache capped at `min(15%, 256 MB)` — never increase without discussion
- `RGB_565` for opaque images (2x memory reduction)
- `onTrimMemory()` hooked in `OtakuReaderApplication` to clear Coil cache under memory pressure
- `SmartPrefetchManager` uses LRU cache with 500-entry hard cap
- `clearCache()` called in `ViewModel.onCleared()`
- Use `contentType` on `LazyColumn`/`LazyVerticalGrid` items for Compose slot reuse
- Wrap flow derivations inside composables with `remember(key)` to prevent resubscription

## Error Handling
- Repository layer catches exceptions and returns `Result<T>` or sealed error types
- ViewModel maps errors to user-facing messages via `Effect.ShowSnackbar`
- Never crash on network errors — show retry UI
- Extension loading failures are graceful — mark as `Untrusted` or `Error`, don't crash
- Prefer `runCatching { }` for fire-and-forget operations where failure is non-fatal

## Null Safety
- Prefer `?: fallback` over `!!` — never use `!!` unless you can prove it's impossible to be null
- Example from production: `Result.failure(lastError ?: Exception("Download failed"))` — not `lastError!!`
- Treat `!!` as a code smell that requires a comment explaining why null is impossible
