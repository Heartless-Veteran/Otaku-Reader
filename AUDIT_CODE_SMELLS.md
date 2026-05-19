# Otaku Reader — Code Smells Audit

> **Date:** 2026-05-18
> **Auditor:** Claude Code (Sonnet 4.6)
> **Scope:** LibraryViewModel, ReaderViewModel, DetailsViewModel, domain use cases, LibraryMvi, ReaderMvi, DetailsMvi, MainActivity — plus cross-cutting evidence from prior audits (DEEP_AUDIT.md, AUDIT_REPORT_2026-05-10.md)
> **Codebase size:** ~88,810 lines of Kotlin across all modules

---

## 1. Executive Summary

| Severity | Count | Description |
|----------|-------|-------------|
| **P0 — Crash Risk** | 7 | Patterns that can cause runtime crashes or data loss |
| **P1 — Tech Debt** | 18 | Architectural problems that impede correctness and testability |
| **P2 — Cleanup** | 21 | Style, readability, and minor design issues |
| **Total** | **46** | |

**Overall Grade: B−**

The codebase is architecturally sound (MVI, Hilt, coroutines, delegate pattern) and unusually free of TODO/FIXME comments. However, three ViewModel god objects dominate the risk profile, the CI build is currently broken due to invalid Compose API usage in `core/ui`, and several subtle coroutine hazards exist that can silently drop data or produce incorrect behaviour under process death or rapid UI interaction.

---

## 2. P0 — Crash Risks

> Format: **File:Line | Issue | Impact | Fix**

### P0-1 — DetailsViewModel: bare `catch (e: Exception)` swallows `CancellationException`

**File:** `feature/details/src/main/java/app/otakureader/feature/details/DetailsViewModel.kt` — multiple sites (~12 functions including `markSelectedAsRead`, `toggleFavorite`, `saveNote`, `toggleChapterRead`, `bookmarkSelectedChapters`, `markSelectedAsUnread`, `setReaderDirection`, `setReaderMode`, etc.)

**Issue:** Every `try { … } catch (e: Exception) { … }` block implicitly catches `kotlinx.coroutines.CancellationException`, which extends `CancellationException extends IllegalStateException extends RuntimeException extends Exception`. When the coroutine is cancelled (e.g. ViewModel cleared), `CancellationException` is swallowed rather than re-thrown, preventing structured cancellation from propagating. This causes the coroutine to complete its `catch` block instead of stopping, potentially writing stale state after the ViewModel has been destroyed.

**Impact:** Stale state updates to `_state` after `onCleared()`; broken coroutine cancellation; possible ANR if downstream work keeps running.

**Fix:**
```kotlin
// Before (all ~12 sites)
} catch (e: Exception) {
    _effect.send(DetailsContract.Effect.ShowError("Failed: ${e.message}"))
}

// After — re-throw CancellationException explicitly
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    _effect.send(DetailsContract.Effect.ShowError("Failed: ${e.message}"))
}
```

Or extract a helper:
```kotlin
private inline fun <T> runSafely(
    errorMessage: String,
    crossinline block: suspend () -> T
): suspend () -> Unit = {
    try { block() }
    catch (e: CancellationException) { throw e }
    catch (e: Exception) {
        _effect.send(DetailsContract.Effect.ShowError("$errorMessage: ${e.message}"))
    }
}
```

---

### P0-2 — DetailsViewModel: `savedStateHandle.get<Long>()` throws on missing argument

**File:** `DetailsViewModel.kt:54`
```kotlin
private val mangaId: Long = savedStateHandle.get<Long>(MANGA_ID_ARG) 
    ?: throw IllegalArgumentException("Manga ID is required")
```

**Issue:** `savedStateHandle.get<Long>()` returns `null` only if the key is absent. But if the calling navigation code passes the argument as an `Int` (due to a type mismatch in the NavGraph), `get<Long>()` returns `null` and the `IllegalArgumentException` crashes the screen. Additionally, if the navigation argument is missing entirely (e.g. from a malformed deep link), this throws from the `init` block — before Hilt finishes injecting the ViewModel — making the crash unrecoverable without a `NavController` pop.

**Impact:** Unhandled crash on Details screen from malformed deep links or navigation type mismatches.

**Fix:**
```kotlin
// Before
private val mangaId: Long = savedStateHandle.get<Long>(MANGA_ID_ARG) 
    ?: throw IllegalArgumentException("Manga ID is required")

// After — use checkNotNull (same as ReaderViewModel) for consistency,
// or defer to state with graceful error
private val mangaId: Long = checkNotNull(savedStateHandle["mangaId"]) {
    "DetailsViewModel requires a mangaId navigation argument"
}
```
Use `checkNotNull(savedStateHandle["mangaId"])` (the same idiom used in `ReaderViewModel`) so the error message is consistent and the crash is still immediately visible during development.

---

### P0-3 — ReaderViewModel: nested `viewModelScope.launch` inside `observePageBookmarks`

**File:** `ReaderViewModel.kt` — `observePageBookmarks()` function
```kotlin
private fun observePageBookmarks() {
    viewModelScope.launch {
        var previousJob: Job? = null
        _state.collect { state ->
            previousJob?.cancel()
            previousJob = viewModelScope.launch {       // <-- nested launch
                pageBookmarkRepository.isPageBookmarked(chapterId, state.currentPage)
                    .collect { isBookmarked ->
                        _state.update { it.copy(isCurrentPageBookmarked = isBookmarked) }
                    }
            }
        }
    }
}
```

**Issue:** `_state.collect` is a terminal operator that never completes while the scope is alive. The outer `launch` runs forever collecting from `_state`, and each emission spawns a new `viewModelScope.launch` for the bookmark check. If `_state` emits rapidly (every page turn), each previous inner `Job` is cancelled, which is correct — but if `state.currentPage` changes faster than `previousJob?.cancel()` returns (e.g. during animation), multiple inner coroutines can be briefly alive simultaneously and issue concurrent `_state.update` calls. More critically, collecting `_state` inside a `_state` collector means state updates triggered by the inner coroutine feedback into the outer collector, creating potential for livelock under high-frequency page changes.

**Impact:** Concurrent/redundant bookmark state writes; potential livelock on fast page flipping.

**Fix:** Use `flatMapLatest` on a derived flow instead of manual job management:
```kotlin
private fun observePageBookmarks() {
    _state
        .map { it.currentPage }
        .distinctUntilChanged()
        .flatMapLatest { page ->
            pageBookmarkRepository.isPageBookmarked(chapterId, page)
        }
        .onEach { isBookmarked ->
            _state.update { it.copy(isCurrentPageBookmarked = isBookmarked) }
        }
        .launchIn(viewModelScope)
}
```

---

### P0-4 — DetailsViewModel: `fetchThumbnailsForDownloadedChapters` silently catches all exceptions including `CancellationException`

**File:** `DetailsViewModel.kt` — `fetchThumbnailsForDownloadedChapters()`
```kotlin
} catch (_: Exception) {
    // Silently fail — thumbnails are optional
}
```

**Issue:** The blank-identifier catch `catch (_: Exception)` catches `CancellationException`. Inside a `supervisorScope`, this prevents cooperative cancellation from propagating. When the ViewModel scope is cancelled, the async blocks continue running, attempting `_state.update` calls on a cleared ViewModel.

**Impact:** State updates after ViewModel destruction; potential memory leaks from long-running source network requests continuing after navigation.

**Fix:**
```kotlin
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    // Thumbnails are optional — silently ignore source errors
}
```

---

### P0-5 — MainActivity: `lifecycleScope.launch` delay inside `CrashReportDialog` Copy button

**File:** `MainActivity.kt` — `CrashReportDialog` composable
```kotlin
(context as? ComponentActivity)?.lifecycleScope?.launch {
    delay(15_000)
    if (clipboard.primaryClipDescription?.label == CRASH_REPORT_CLIP_LABEL) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
```

**Issue:** `context as? ComponentActivity` is a side-effectful cast inside a composable, violating Compose's principle that composables should be side-effect-free. The `lifecycleScope.launch` launches a coroutine from inside a click handler using an Activity cast that may be `null` if the context is a wrapper. If the Activity is rotated or recreated within the 15-second window, the coroutine continues running against the original (now-destroyed) lifecycle scope. The clipboard clear also runs on any Activity instance, not the one that copied.

**Impact:** Coroutine on a destroyed lifecycle; potential clipboard clear on wrong instance.

**Fix:** Move the clipboard-clear logic to a `LaunchedEffect` keyed on a `clipboardCopied` state flag, or hoist it to the ViewModel/Activity's `lifecycleScope` with proper lifecycle awareness.

---

### P0-6 — ReaderViewModel: `sharePage()` is a no-op silently

**File:** `ReaderViewModel.kt`
```kotlin
/**
 * Schedule auto-save of reading progress with debouncing...
 */
private fun sharePage() {
    // Implementation for sharing current page
}
```

**Issue:** The KDoc comment on `sharePage()` is copy-pasted from `scheduleProgressSave()` and describes a completely different function. The function itself is empty. `ReaderEvent.SharePage` is a public event reachable from the UI. When a user taps "Share Page" from the reader menu, nothing happens — no error, no feedback, nothing.

**Impact:** Silent feature failure; user confusion; KDoc mismatch makes the code actively misleading to maintainers.

**Fix:** Either implement the share functionality using `Intent.ACTION_SEND` with a page screenshot/URL, or send a `ShowSnackbar` effect indicating the feature is not yet available:
```kotlin
private fun sharePage() {
    viewModelScope.launch {
        _effect.send(ReaderEffect.ShowSnackbar("Share page is not yet implemented"))
    }
}
```

---

### P0-7 — Broken CI: `core/ui` compilation errors block all builds

**File:** `core/ui/` — multiple files per DEEP_AUDIT.md
- `drawCircle(shadow = ...)` — invalid Compose Canvas API (5 occurrences)
- `GenericShape`/`addPath` — unresolved symbols in `InkRevealModifier.kt`
- Missing `@Composable` annotation on private helper in `GradientMeshOrbs.kt`

**Issue:** 12+ files in the `core/ui` module fail to compile. This makes every PR build red and prevents any developer from verifying that their changes compile cleanly. These are not runtime bugs — they are compile-time failures.

**Impact:** The entire CI pipeline (Build, Unit Tests, Detekt) is broken. New bugs cannot be caught by CI. The codebase effectively has no safety net until these are fixed.

**Fix:** Fix each invalid API call. For `drawCircle(shadow=...)`, remove the unsupported `shadow` parameter and implement shadow via a separate `drawCircle` with a translucent offset colour. For `GenericShape`, switch to `androidx.compose.ui.graphics.Path` with standard `addPath` semantics.

---

## 3. P1 — Tech Debt

### P1-1 — ReaderViewModel: God Object (`@Suppress("LargeClass")` in production code)

**File:** `ReaderViewModel.kt`

The class is annotated with `@Suppress("LargeClass")`. The file is 36,614 bytes. Despite the delegate refactor (DEEP_AUDIT §5), the ViewModel still:
- Owns 60+ `ReaderState` fields (counted in `ReaderMvi.kt`)
- Has a 50+ line `loadSettings()` function that performs a `.copy()` of 37 individual fields
- Directly owns `currentManga`, `currentChapter`, `autoSaveJob`, `lastPageChangeMs`, `sessionReadAt`, `sessionStartMs` — mutable state outside the state flow
- Directly calls `behaviorTracker`, `prefetchDelegate`, `discordDelegate`, `downloadAheadDelegate`, `historyDelegate`, `settingsLoaderDelegate`, `chapterLoaderDelegate` in `init {}` and `onCleared()`
- Contains `ZOOM_INCREMENT`, `BRIGHTNESS_INCREMENT`, `AUTO_SCROLL_INCREMENT`, `MIN_ZOOM`, `MAX_ZOOM`, `PROGRESS_SAVE_DELAY` as companion constants — duplicated verbatim in `ReaderEvent.companion`

**See God Object Analysis section for decomposition plan.**

---

### P1-2 — DetailsViewModel: God Object (`@Suppress("LargeClass")` in production code)

**File:** `DetailsViewModel.kt` (39,592 bytes, ~949 lines)

- 40+ event types handled in a single `onEvent()` dispatch block
- Manages: manga details, chapters, downloads, tracking, notes, reader settings, source suggestions, panorama cover, notifications, delete-after-read, chapter thumbnails, preload settings — 13 distinct responsibilities
- Uses manual `LRU LinkedHashMap` (50 entries) for thumbnail cache — this is a data-layer concern living in the ViewModel
- `fetchThumbnailsForDownloadedChapters` performs network I/O (source page list requests) directly in the ViewModel rather than through a use case
- `refreshData` simulates a network delay with `kotlinx.coroutines.delay(500)` — hardcoded magic number, no real implementation

---

### P1-3 — LibraryViewModel: 13 injected dependencies (constructor over-injection)

**File:** `LibraryViewModel.kt`
```kotlin
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryManga: GetLibraryMangaUseCase,
    private val searchLibraryManga: SearchLibraryMangaUseCase,
    private val toggleFavoriteManga: ToggleFavoriteMangaUseCase,
    private val libraryPreferences: LibraryPreferences,
    private val generalPreferences: GeneralPreferences,
    private val chapterRepository: ChapterRepository,
    private val downloadRepository: DownloadRepository,
    private val trackRepository: TrackRepository,
    private val getCategories: GetCategoriesUseCase,
    private val getContinueReading: GetContinueReadingUseCase,
    private val readingGoalPreferences: ReadingGoalPreferences,
    private val statisticsRepository: StatisticsRepository,
    private val readingListRepository: ReadingListRepository,
) : ViewModel()
```

13 constructor parameters. The rule of thumb is >5–7 is a signal of SRP violation. The ViewModel is directly accessing three repositories (`chapterRepository`, `downloadRepository`, `trackRepository`) instead of going through use cases, and two preference objects that should be abstracted behind use cases or a unified settings interface.

---

### P1-4 — `observePageBookmarks` starts a `_state.collect` loop that feeds back into `_state`

Already filed as P0-3 (crash risk), but also a tech debt concern: any architecture where a state observer triggers further state updates within the same flow creates an implicit feedback loop that is fragile and hard to reason about. The `LaunchedEffect`/`flatMapLatest` pattern (see P0-3 fix) is the correct approach.

---

### P1-5 — `loadLibrary()` uses `coroutineScope { }` inside a `Flow.map { }` operator

**File:** `LibraryViewModel.kt` — `loadLibrary()`
```kotlin
getLibraryManga()
    .map { mangaList ->
        coroutineScope {
            val trackingDeferred = mangaList.map { manga ->
                async {
                    val hasEntries = trackRepository.observeEntriesForManga(manga.id)
                        .first().isNotEmpty()
                    ...
                }
            }
            ...
        }
    }
```

**Issue:** `coroutineScope { }` inside a `Flow.map` operator is a blocking call that suspends the upstream flow until all deferred work completes. If `mangaList` contains 500 entries and each `async { trackRepository.observeEntriesForManga(...).first() }` takes even 5 ms, this blocks the map operator for 2.5 seconds synchronously (though parallelised via `async`). More critically, if the upstream library flow emits again while `coroutineScope` is still running (e.g. a library update arrives), the new emission is queued — but because `map` is a sequential operator, the new emission cannot be processed until the current `coroutineScope` unblocks. This creates head-of-line blocking and potentially stale UI.

**Fix:** Convert to `flatMapLatest` so that a new library emission cancels the in-flight parallel work:
```kotlin
getLibraryManga()
    .flatMapLatest { mangaList ->
        flow {
            val enriched = coroutineScope {
                // parallel tracking/download lookups
            }
            emit(enriched)
        }
    }
```

---

### P1-6 — `refreshData()` in DetailsViewModel uses `delay(500)` as a fake implementation

**File:** `DetailsViewModel.kt`
```kotlin
private fun refreshData() {
    _state.update { it.copy(isRefreshing = true) }
    viewModelScope.launch {
        kotlinx.coroutines.delay(500) // Simulate network delay
        _state.update { it.copy(isRefreshing = false) }
    }
}
```

This is placeholder code in a production ViewModel. The pull-to-refresh gesture sends `DetailsContract.Event.Refresh` → `refreshData()`, which waits 500 ms and pretends to refresh. No actual data is re-fetched from the source. Users who pull-to-refresh get the loading spinner for half a second and then the same stale data.

**Fix:** Implement actual source refresh — call `sourceRepository.getMangaDetails()` and update the manga in `mangaRepository`, then re-observe the flow. Or expose a `refreshMangaFromSource` use case.

---

### P1-7 — `DetailsViewModel.setDeleteAfterReadOverride` is `@Suppress("UnusedParameter")` dead code

**File:** `DetailsViewModel.kt`
```kotlin
@Suppress("UnusedParameter")
private fun setDeleteAfterReadOverride(mode: DeleteAfterReadMode) {
    viewModelScope.launch {
        _effect.send(DetailsContract.Effect.ShowSnackbar("Delete-after-read is no longer supported."))
    }
}
```

The feature was removed, but the entire Event/handler chain was left in place: `DetailsContract.Event.SetDeleteAfterReadOverride`, its handler in `onEvent`, the `deleteAfterReadOverride` field in `DetailsContract.State`, `globalDeleteAfterRead` field, `isDeleteAfterReadEnabled` computed property, and `observeStaticSettings()` still observing `downloadPreferences.deleteAfterReading` and `downloadPreferences.perMangaOverrides`. This is dead code coupled to live preferences observations.

**Fix:** Remove the dead event, the `@Suppress` annotation, the state fields, the preferences observation, and the `DownloadPreferences` dependency from the constructor entirely.

---

### P1-8 — `ReaderViewModel.loadSettings()` copies 37 fields individually

**File:** `ReaderViewModel.kt`
```kotlin
_state.update { current ->
    current.copy(
        mode = settingsState.mode,
        brightness = settingsState.brightness,
        keepScreenOn = settingsState.keepScreenOn,
        // ... 34 more lines
        secureScreen = settingsState.secureScreen,
    )
}
```

37 individual field assignments in a single `.copy()` call. If `ReaderState` gains a new setting field and `loadSettings()` is not updated, the field silently defaults to its initial value — the bug is invisible. The `settingsState` object already has all the fields; there should be a structured way to merge only the settings slice.

**Fix:** Extract settings into a sub-class `ReaderSettingsState` and have `ReaderState` contain it as a nested object, so `_state.update { it.copy(settings = settingsState) }` is a single call.

---

### P1-9 — `SearchLibraryMangaUseCase` is orphaned — not used by `LibraryViewModel`

**File:** `domain/src/main/java/app/otakureader/domain/usecase/SearchLibraryMangaUseCase.kt`

Per `AUDIT_REPORT_2026-05-10.md`, `SearchLibraryMangaUseCase` was identified as having no production consumer. However, inspection of `LibraryViewModel.kt` shows:
```kotlin
private val searchLibraryManga: SearchLibraryMangaUseCase,
...
searchJob = viewModelScope.launch {
    delay(300L)
    searchLibraryManga(query).collect { mangas ->
        _searchMatchingIds.value = mangas.map { it.id }.toSet()
    }
}
```

It **is** used by `LibraryViewModel`. The prior audit was incorrect on this point. However, the use case has an `@Suppress("CyclomaticComplexMethod")` suppression on `matchesCriteria()`. The cyclomatic complexity is real (12+ branches) and should be decomposed into `checkExcludeTerms()`, `checkRequiredTags()`, `checkAuthor()`, `checkStatus()`, `checkPhrases()`, `checkIncludeTerms()` helpers.

---

### P1-10 — `loadCategories()` uses `viewModelScope.launch { collect { } }` anti-pattern

**File:** `LibraryViewModel.kt`
```kotlin
private fun loadCategories() {
    viewModelScope.launch {
        getCategories()
            .collect { categories ->
                ...
                _state.update { it.copy(categories = items) }
            }
    }
}
```

Using `launch { collect { } }` instead of `.onEach { }.launchIn(viewModelScope)` is inconsistent with the rest of the file (which correctly uses `launchIn`) and creates a subtle difference: `launch { collect }` starts a new coroutine that owns the collection lifecycle, while `launchIn` ties the collection to the scope directly. Both work, but mixing the patterns makes the code harder to reason about. More importantly, if `loadCategories()` is called a second time (e.g. from `onRefresh()` — though it is not currently), multiple collectors would be active simultaneously, emitting duplicate state updates.

**Fix:** Use `.onEach { }.launchIn(viewModelScope)` for consistency.

---

### P1-11 — `observeFilteredItems()` mixes `viewModelScope.launch { collect }` and `.launchIn()` in the same function

**File:** `LibraryViewModel.kt`
```kotlin
private fun observeFilteredItems() {
    viewModelScope.launch {        // <-- collect pattern
        _state.map { it.selectedCategory }
            .distinctUntilChanged()
            .collect { categoryId -> ... }
    }

    _state.map { it.filterReadingListId }    // <-- launchIn pattern
        .distinctUntilChanged()
        .flatMapLatest { ... }
        .onEach { ... }
        .launchIn(viewModelScope)

    combine(...) { ... }           // <-- launchIn pattern
        .onEach { ... }
        .launchIn(viewModelScope)
}
```

Three collection strategies in a single function. See P1-10.

---

### P1-12 — `DetailsMvi.kt` has `@file:Suppress("MatchingDeclarationName")`

**File:** `feature/details/src/main/java/app/otakureader/feature/details/DetailsMvi.kt:1`
```kotlin
@file:Suppress("MatchingDeclarationName")
```

The suppression hides the fact that the file is named `DetailsMvi.kt` but contains `DetailsContract` as the primary declaration. This is a naming inconsistency — the file should be renamed to `DetailsContract.kt` or the object renamed to `DetailsMvi`. The suppression masks the discrepancy.

---

### P1-13 — `MangaStatus.colorValue()` hardcodes `Color(0xFF...)` literals in `DetailsMvi.kt`

**File:** `DetailsMvi.kt`
```kotlin
fun MangaStatus.colorValue(): androidx.compose.ui.graphics.Color = when (this) {
    MangaStatus.ONGOING -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
    MangaStatus.COMPLETED -> androidx.compose.ui.graphics.Color(0xFF2196F3)
    MangaStatus.LICENSED -> androidx.compose.ui.graphics.Color(0xFFFF9800)
    MangaStatus.PUBLISHING_FINISHED -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
    MangaStatus.CANCELLED -> androidx.compose.ui.graphics.Color(0xFFF44336)
    MangaStatus.ON_HIATUS -> androidx.compose.ui.graphics.Color(0xFFFFC107)
    ...
}
```

Six hardcoded hex colour literals in a domain-adjacent file. Colour values should live in the design system (`core/ui/theme/`) and be referenced symbolically. These colours also do not respect the Material 3 dynamic colour scheme — they are fixed regardless of dark mode or user colour preferences.

---

### P1-14 — `UpdateLibraryMangaUseCase` catches bare `Exception` at the top level

**File:** `domain/src/main/java/app/otakureader/domain/usecase/UpdateLibraryMangaUseCase.kt`
```kotlin
return try {
    ...
} catch (e: Exception) {
    Result.failure(e)
}
```

Same `CancellationException` swallowing issue as P0-1. In a suspend function, catching `Exception` instead of specific exception types prevents cooperative cancellation.

**Fix:** Same as P0-1 — catch `CancellationException` first and re-throw it.

---

### P1-15 — `observeGoalProgress` in LibraryViewModel re-throws `CancellationException` correctly but uses `if (e is …) throw e` idiom instead of idiomatic Kotlin

**File:** `LibraryViewModel.kt`
```kotlin
.catch { e ->
    if (e is kotlinx.coroutines.CancellationException) throw e
    android.util.Log.w("LibraryViewModel", "observeGoalProgress failed", e)
}
```

This is correct but uses a fully-qualified class name inline, is inconsistent with other catch sites in the same file, and uses `android.util.Log` directly in a ViewModel (coupling to Android Framework). The use of `android.util.Log` directly (rather than a logging abstraction) also appears in `observeContinueReading`.

**Fix:** Import `CancellationException`, and replace `android.util.Log` with Timber or a logging interface.

---

### P1-16 — `ReaderScreen` uses `LaunchedEffect(Unit)` for `focusRequester.requestFocus()` twice

**File:** `ReaderScreen.kt`
```kotlin
LaunchedEffect(Unit) {
    focusRequester.requestFocus()
}

LaunchedEffect(state.isMenuVisible, state.isGalleryOpen) {
    focusRequester.requestFocus()
}
```

The first `LaunchedEffect(Unit)` is subsumed by the second `LaunchedEffect(state.isMenuVisible, state.isGalleryOpen)` — when the composable first enters, both effects fire (the second with initial values). This means `requestFocus()` is called twice on the first composition. While not a crash, it generates unnecessary work and the intent is unclear: is `LaunchedEffect(Unit)` for the initial focus, or is it a defensive "always focus on entry"? The duplication should be collapsed.

---

### P1-17 — `ReaderScreen` has `@Suppress("UnusedParameter")` on a public-facing composable

**File:** `ReaderScreen.kt`
```kotlin
@Composable
@Suppress("UnusedParameter")
fun ReaderScreen(
    mangaId: Long,
    chapterId: Long,
    ...
)
```

`mangaId` and `chapterId` are suppressed as unused because the ViewModel reads them from `SavedStateHandle`. The suppression is justified but the parameters should still exist in the composable signature for documentation and routing purposes. However, `@Suppress("UnusedParameter")` on a `@Composable` function signals a navigation contract confusion: if the parameters are unused by the composable, the composable should not declare them — they are an implementation detail of the ViewModel's navigation argument binding.

---

### P1-18 — `ReaderEvent.companion` duplicates constants defined in `ReaderViewModel.companion`

**File:** `ReaderMvi.kt:companion` vs `ReaderViewModel.kt:companion`
```kotlin
// ReaderMvi.kt
companion object {
    const val ZOOM_INCREMENT = 0.25f
    const val MIN_ZOOM = 0.5f
    const val MAX_ZOOM = 5.0f
    const val BRIGHTNESS_INCREMENT = 0.1f
    const val AUTO_SCROLL_INCREMENT = 50f
}

// ReaderViewModel.kt
companion object {
    private const val MIN_ZOOM = 0.5f
    private const val MAX_ZOOM = 5f
    private const val PROGRESS_SAVE_DELAY = 3000L
    const val ZOOM_INCREMENT = 0.25f
    const val BRIGHTNESS_INCREMENT = 0.1f
    const val AUTO_SCROLL_INCREMENT = 50f
}
```

`ZOOM_INCREMENT`, `MIN_ZOOM`, `MAX_ZOOM`, `BRIGHTNESS_INCREMENT`, `AUTO_SCROLL_INCREMENT` are defined in both places. If one is updated and the other is not, the ViewModel and the event contract will disagree on limits.

**Fix:** Remove the duplicates from `ReaderViewModel.companion` and reference them from `ReaderEvent.companion`, or vice versa.

---

## 4. P2 — Cleanup

### P2-1 — Magic number `50` used three times in `DetailsViewModel` thumbnail LRU cache

**File:** `DetailsViewModel.kt`
```kotlin
private val thumbnailCache = object : LinkedHashMap<Long, Pair<String?, Int>>(50, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<Long, Pair<String?, Int>>): Boolean {
        return size > 50  // magic 50 again
    }
}
```
And the `.take(10)` limit in `fetchThumbnailsForDownloadedChapters`:
```kotlin
}.take(10)
```
Three magic numbers. Define them as constants: `THUMBNAIL_CACHE_SIZE = 50`, `THUMBNAIL_PREFETCH_BATCH = 10`, initial capacity `50`, load factor `0.75f`.

---

### P2-2 — `delay(300L)` magic number in `LibraryViewModel.onSearchQueryChange`

**File:** `LibraryViewModel.kt`
```kotlin
searchJob = viewModelScope.launch {
    delay(300L)
    searchLibraryManga(query).collect { ... }
}
```
Extract: `private const val SEARCH_DEBOUNCE_MS = 300L`.

---

### P2-3 — `delay(500)` magic number in `DetailsViewModel.refreshData`

**File:** `DetailsViewModel.kt`
```kotlin
kotlinx.coroutines.delay(500) // Simulate network delay
```
This is a placeholder (P1-6) but also a magic number. Even if keeping as a stub, it should be a named constant. However it should simply be removed and replaced with real implementation.

---

### P2-4 — `@file:Suppress("MaxLineLength")` on 15+ files

Per DEEP_AUDIT.md, 15 files suppress `MaxLineLength`. Line length suppressions should be fixed by wrapping the long lines, not suppressing the lint rule. The most impactful files are `LibraryScreen.kt` (39,313 bytes), `DetailsScreen.kt` (69,217 bytes), and `ReaderMvi.kt` (26,961 bytes). Long lines in state data classes and sealed class hierarchies can be wrapped with one property per line.

---

### P2-5 — `DetailsViewModel.onEvent` has `@Suppress("CyclomaticComplexMethod")`

**File:** `DetailsViewModel.kt`
```kotlin
@Suppress("CyclomaticComplexMethod")
fun onEvent(event: DetailsContract.Event) {
    when (event) { ...40+ branches... }
}
```
The suppression on a `when` expression that dispatches to private functions is masking real complexity. The `when` block itself is a dispatch table, not complex logic — the real issue is the number of responsibilities. The `@Suppress` should be removed and the responsibilities decomposed (see God Object Analysis).

---

### P2-6 — `buildShareUrl()` in DetailsViewModel is private but contains business logic that belongs in a use case

**File:** `DetailsViewModel.kt`
```kotlin
private fun buildShareUrl(manga: Manga): String? {
    val url = manga.url
    return if (url.startsWith("http://") || url.startsWith("https://")) url else null
}
```
URL validation belongs in the domain layer. This logic also duplicates any similar URL validation elsewhere in the codebase.

---

### P2-7 — `ReaderViewModel.toggleSetting` has `else -> { /* Other settings not yet implemented */ }`

**File:** `ReaderViewModel.kt`
```kotlin
else -> { /* Other settings not yet implemented */ }
```
An exhaustive `when` on a sealed enum with an `else` branch means new `ReaderSetting` values added to the enum will silently do nothing. Remove the `else` branch so the compiler enforces exhaustiveness.

---

### P2-8 — `ReaderMvi.kt` has duplicate `// Action events` section header comment

**File:** `ReaderMvi.kt`
```kotlin
// ── Action events (existing)
// ──────────────────────────────────────────────────────────────────────────
// Action events (existing)
```
Two consecutive comment blocks for the same section. Remove the duplicate.

---

### P2-9 — `LibraryMvi.kt` state has `categoryFilterMangaIds` as a public field that is an internal implementation detail

**File:** `LibraryMvi.kt`
```kotlin
val categoryFilterMangaIds: Set<Long> = emptySet(), // Manga IDs in selected category
```
This field is populated by `observeFilteredItems()` in the ViewModel and exists purely to store intermediate results of a side-effectful query. It should not be part of the public `LibraryState` — it is an implementation detail of the filter pipeline, not something the UI needs to observe or render.

---

### P2-10 — `LibraryMvi.kt` state has `readingListMangaIds` as another internal implementation field

**File:** `LibraryMvi.kt`
```kotlin
val readingListMangaIds: Set<Long> = emptySet(),
```
Same issue as P2-9. Both `categoryFilterMangaIds` and `readingListMangaIds` should be private `MutableStateFlow`s in the ViewModel, not part of the UI state contract.

---

### P2-11 — `FilterSortParams` private data class in `LibraryViewModel` duplicates fields already in `LibraryState`

**File:** `LibraryViewModel.kt`
```kotlin
private data class FilterSortParams(
    val query: String,
    val searchMatchingIds: Set<Long>?,
    val filterHasNotes: Boolean,
    val sortMode: LibrarySortMode,
    val filterMode: LibraryFilterMode,
    val filterSourceId: Long?,
    val showNsfw: Boolean,
    val selectedCategory: Long?,
    val categoryMangaIds: Set<Long> = emptySet(),
    val filterReadingListId: Long? = null,
    val readingListMangaIds: Set<Long> = emptySet()
)
```
All 11 fields in `FilterSortParams` are direct copies of fields from `LibraryState`. The `combine` in `observeFilteredItems()` maps `LibraryState` → `FilterSortParams` and then maps `FilterSortParams` back into filter operations. This intermediate class adds indirection without adding type safety or clarity. The `combine` could operate on `LibraryState` directly, or the filtering could be done in a use case.

---

### P2-12 — `MangaStatus.colorValue()` extension function uses fully-qualified `androidx.compose.ui.graphics.Color`

**File:** `DetailsMvi.kt`
```kotlin
fun MangaStatus.colorValue(): androidx.compose.ui.graphics.Color = when (this) {
    MangaStatus.UNKNOWN -> androidx.compose.ui.graphics.Color.Gray
```
The fully-qualified type appears 8 times in 7 lines. Add the import at the top of the file.

---

### P2-13 — `DetailsContract.State.sortedChapters` and `groupedChapters` are computed properties that do O(n log n) work on every state access

**File:** `DetailsMvi.kt`
```kotlin
val sortedChapters: List<ChapterItem>
    get() {
        val filtered = chapterFilter.apply(chapters)
        return when (chapterSortOrder) { ... filtered.sortedBy/sortedByDescending ... }
    }

val groupedChapters: Map<String?, List<ChapterItem>>
    get() = sortedChapters.groupBy { it.volume }
```
These computed `val`s using `get()` run a sort (O(n log n)) and a group-by (O(n)) every time they are accessed. If they are accessed multiple times per recomposition frame in `DetailsScreen.kt` (1,722 lines), the cost compounds. They should be derived once in the ViewModel and stored in state, or `remember`-ed with appropriate keys in the composable.

---

### P2-14 — `LibraryViewModel.applySort` re-allocates a sorted list on every filter/sort pipeline execution

**File:** `LibraryViewModel.kt`
```kotlin
private fun applySort(items: List<LibraryMangaItem>, sortMode: LibrarySortMode): List<LibraryMangaItem> {
    return when (sortMode) {
        LibrarySortMode.ALPHABETICAL -> items.sortedBy { it.title }
        LibrarySortMode.LAST_READ -> items.sortedByDescending { it.lastRead ?: 0L }
        ...
    }
}
```
`sortedBy` creates a new list on every call. The pipeline in `applyFiltersAndSort` chains 7 list allocations: `applyCategoryFilter` → `applyNsfwFilter` → `applySearchFilter` → `applyHasNotesFilter` → `applySourceFilter` → `applyReadingListFilter` → `applyFilterMode` → `applySort`. For a library of 500 titles, this is 7 intermediate `List<LibraryMangaItem>` allocations per emission of the combine flow. Use a sequence pipeline to avoid intermediate allocations:
```kotlin
return items.asSequence()
    .let { applyFilters(it, params) }
    .sortedWith(comparatorFor(params.sortMode))
    .toList()
```

---

### P2-15 — `Chapter.toChapterItem()` extension in `DetailsMvi.kt` uses regex on every call

**File:** `DetailsMvi.kt`
```kotlin
fun Chapter.toChapterItem(...): DetailsContract.ChapterItem {
    val volumeRegex = Regex("""Vol\.?\s*(\d+)""", RegexOption.IGNORE_CASE)
    val volume = volumeRegex.find(name)?.groupValues?.get(1)?.let { "Volume $it" }
```
`Regex(...)` compiles the pattern on every invocation. For a chapter list of 200 chapters, this compiles the regex 200 times per `loadChapters()` emission. Move the pattern to a file-level companion/top-level `val`.

---

### P2-16 — `1,235 private declarations` across the codebase (DEEP_AUDIT finding)

Per DEEP_AUDIT.md: 1,235 private declarations. While many are legitimate, this count is a strong indicator of dead private functions and fields — code that exists but is never called from outside or even inside its class. This cannot be audited file-by-file here, but Detekt's `UnusedPrivateProperty` rule (currently failing in CI due to P0-7) will surface these when the CI is fixed.

---

### P2-17 — `LibraryScreen.kt` is 39,313 bytes (~1,000+ lines)

**File:** `feature/library/src/main/java/app/otakureader/feature/library/LibraryScreen.kt` (39,313 bytes)

Monolithic composable screen. Compose recomposition triggers for the entire subtree when any piece of state changes. Should be decomposed into: `LibraryTopBar`, `LibrarySearchBar`, `CategoryTabRow`, `ContinueReadingSection`, `LibraryGrid`, `LibraryFilterSheet`.

---

### P2-18 — `DetailsScreen.kt` is 69,217 bytes (~1,722 lines) — the largest file in the codebase

**File:** `feature/details/src/main/java/app/otakureader/feature/details/DetailsScreen.kt` (69,217 bytes)

Same issue as P2-17, more severe. DEEP_AUDIT has already identified this. Target decomposition: `DetailsCoverSection`, `DetailsInfoSection`, `DetailsActionBar`, `ChapterListSection`, `SourceSuggestionsSection`, `MangaNotesSheet`, `ReaderSettingsSheet`.

---

### P2-19 — `SharingStarted.WhileSubscribed(5_000L)` magic number in `ReaderViewModel`

**File:** `ReaderViewModel.kt`
```kotlin
private val sharing = SharingStarted.WhileSubscribed(5_000L)
```
`5_000L` is a magic number. While it is a well-known default in Android (`SharingStarted.WhileSubscribed(5000)` is the documented recommendation), it should be a named constant: `private const val STATE_SUBSCRIPTION_STOP_TIMEOUT_MS = 5_000L`.

---

### P2-20 — `handleNavigationEvent`, `handleUiStateEvent`, `handleFilterSortEvent`, `handleActionEvent` all have `else -> Unit // unreachable` branches

**File:** `LibraryViewModel.kt`
```kotlin
private fun handleNavigationEvent(event: LibraryEvent) {
    when (event) {
        is LibraryEvent.Refresh -> onRefresh()
        ...
        else -> Unit // unreachable due to outer when
    }
}
```
The outer `when` in `onEvent` routes events to the correct handler, so these `else -> Unit` branches are indeed unreachable. However, their presence silences the compiler's exhaustiveness check — if a new `LibraryEvent` subclass is added, the compiler will not warn that it is unhandled. The correct pattern is to make each `when` exhaustive without `else`:
```kotlin
private fun handleNavigationEvent(event: LibraryEvent) {
    when (event) {
        is LibraryEvent.Refresh -> onRefresh()
        is LibraryEvent.OnMangaClick -> onMangaClick(event.mangaId)
        is LibraryEvent.OnMangaLongClick -> onMangaLongClick(event.mangaId)
        is LibraryEvent.ContinueReadingClick -> onContinueReadingClick(event.mangaId, event.chapterId)
        // Do NOT add else — let the compiler enforce that new events are handled
    }
}
```
This requires making `LibraryEvent` a sealed interface and removing the `else` branches.

---

### P2-21 — `THEME_MODE_SYSTEM = 0`, `THEME_MODE_LIGHT = 1`, `THEME_MODE_DARK = 2` in `MainActivity.companion` should be an enum

**File:** `MainActivity.kt`
```kotlin
companion object {
    const val THEME_MODE_SYSTEM = 0
    const val THEME_MODE_LIGHT = 1
    const val THEME_MODE_DARK = 2
}
```
These are logically an enum. If `themeMode` is persisted as an `Int`, mapping it through `ThemeMode.entries[themeMode]` is safer and more readable than `when (themeMode) { THEME_MODE_LIGHT -> false; THEME_MODE_DARK -> true; else -> isSystemInDarkTheme() }`.

---

## 5. God Object Analysis

### 5.1 — ReaderViewModel (36,614 bytes, ~862 lines)

**Responsibilities currently bundled:**

| # | Responsibility | Current Home | Should Be |
|---|---------------|--------------|-----------|
| 1 | Page loading & chapter resolution | `ReaderViewModel` + `ReaderChapterLoaderDelegate` | `ReaderChapterLoaderDelegate` (already exists) |
| 2 | Settings loading & per-manga overrides | `ReaderViewModel.loadSettings()` | `ReaderSettingsLoaderDelegate` (already exists) |
| 3 | Reading history recording | `ReaderViewModel` + `ReaderHistoryDelegate` | `ReaderHistoryDelegate` (already exists) |
| 4 | Prefetch / preload | `ReaderViewModel` + `ReaderPrefetchDelegate` | `ReaderPrefetchDelegate` (already exists) |
| 5 | Discord Rich Presence | `ReaderViewModel` + `ReaderDiscordDelegate` | `ReaderDiscordDelegate` (already exists) |
| 6 | Download-ahead | `ReaderViewModel` + `ReaderDownloadAheadDelegate` | `ReaderDownloadAheadDelegate` (already exists) |
| 7 | **Page navigation logic** | `ReaderViewModel` directly | Should be extracted or remain — it's core reader concern |
| 8 | **Settings mutation** (37 individual toggleSetting branches) | `ReaderViewModel` directly | `ReaderSettingsViewModel` (new) |
| 9 | **Bookmark management** | `ReaderViewModel` directly | `ReaderBookmarkViewModel` (new) |
| 10 | **Zoom/brightness** | `ReaderViewModel` directly | `ReaderDisplayViewModel` (new) |

**Remaining bloat after delegates exist:**
- `loadSettings()` still copies 37 fields manually (P1-8)
- `changePage()` is 50+ lines mixing navigation, telemetry, Discord updates, download-ahead, and progress scheduling
- `toggleSetting()` is a 70-line `when` block that could be data-driven

**Recommended split (Issue #581 target):**

```
ReaderViewModel (coordinator — ~200 lines)
├── ReaderPageViewModel (page nav, panels, chapter nav — ~150 lines)
├── ReaderDisplayViewModel (zoom, brightness, color filter, rotation — ~100 lines)
├── ReaderSettingsViewModel (all persistent toggleable settings — ~80 lines)
└── ReaderBookmarkViewModel (bookmark toggle, observe bookmark — ~50 lines)
```
Delegates remain as infrastructure. The coordinator passes delegates to child VMs via Hilt.

---

### 5.2 — DetailsViewModel (39,592 bytes, ~949 lines)

**Responsibilities currently bundled:**

| # | Responsibility | Lines (est.) |
|---|---------------|-------------|
| 1 | Manga details loading & observation | ~30 |
| 2 | Chapter list loading, enrichment, thumbnail fetching | ~80 |
| 3 | Chapter selection management | ~50 |
| 4 | Chapter read/unread/bookmark toggling | ~80 |
| 5 | Download management (enqueue, delete, export CBZ) | ~100 |
| 6 | Library (favorite) toggle | ~30 |
| 7 | Notes editor | ~40 |
| 8 | Notifications toggle | ~30 |
| 9 | Per-manga reader settings (direction, mode, color filter, bg, preload) | ~100 |
| 10 | Source suggestions (fetch popular, map to suggestions) | ~80 |
| 11 | Chapter sorting and filtering (via `sortedChapters` in State) | In state |
| 12 | Share manga | ~20 |
| 13 | Dead code: delete-after-read | ~20 |

**Recommended split:**

```
DetailsViewModel (coordinator — ~150 lines)
├── DetailsMangaViewModel (manga info, favorite, share, notes — ~150 lines)
├── DetailsChapterViewModel (chapter list, selection, read/bookmark — ~200 lines)
├── DetailsDownloadViewModel (enqueue, delete, export — ~100 lines)
└── DetailsReaderSettingsViewModel (per-manga reader settings — ~100 lines)
```
Source suggestions could move to a `DetailsSourceViewModel` or be handled by a new `GetSourceSuggestionsUseCase` called from the coordinator.

---

### 5.3 — LibraryViewModel (25,779 bytes, ~630 lines)

**Responsibilities currently bundled:**

| # | Responsibility |
|---|---------------|
| 1 | Library loading (with parallel tracking/download lookups) |
| 2 | Category loading and selection |
| 3 | Search with debounce (via `searchLibraryManga`) |
| 4 | Filter pipeline (7 filter steps, 1 sort step) |
| 5 | Manga selection management |
| 6 | Bulk actions (mark read/unread, download, remove from library) |
| 7 | Continue Reading section |
| 8 | Reading goal progress |
| 9 | Reading list filter |
| 10 | New updates count |

Less severe than Details/Reader but still 10 responsibilities. The filter pipeline (P1-5, P2-14) is the most performance-sensitive and should be extracted into a `LibraryFilterUseCase` that operates on `LibraryMangaItem` lists.

---

## 6. Coroutine Safety Issues

### 6.1 — `CancellationException` swallowing (P0-1, P0-4, P1-14)

Affected files:
- `DetailsViewModel.kt` — 12+ `catch (e: Exception)` blocks
- `DetailsViewModel.fetchThumbnailsForDownloadedChapters` — `catch (_: Exception)`
- `UpdateLibraryMangaUseCase.kt` — top-level `catch (e: Exception)`

All must add `catch (e: CancellationException) { throw e }` before the generic catch.

### 6.2 — `observePageBookmarks` feedback loop (P0-3)

`_state.collect { }` inside `viewModelScope.launch { }` that triggers `_state.update { }` creates a synchronous feedback path. Use `flatMapLatest` (see P0-3 fix).

### 6.3 — `coroutineScope { }` inside `Flow.map { }` (P1-5)

Blocks upstream flow emissions. Use `flatMapLatest` with an inner `flow { emit(coroutineScope { ... }) }`.

### 6.4 — `LaunchedEffect` in `ReaderScreen` collecting effects (correct pattern, but scope issue)

**File:** `ReaderScreen.kt`
```kotlin
LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
        when (effect) {
            is ReaderEffect.ShowSnackbar -> {
                scope.launch {              // <-- inner launch inside collect
                    snackbarHostState.showSnackbar(text)
                }
            }
        }
    }
}
```
Launching `scope.launch` inside a `collect` block means snackbar shows run on a different coroutine from the one collecting effects. If the composable leaves the composition while a snackbar is being shown, `scope` (which is `rememberCoroutineScope`) is cancelled, so the `showSnackbar` call is cancelled mid-display. This is generally acceptable but the nested launch is unnecessary — `snackbarHostState.showSnackbar` is already a suspend function and can be called directly inside `collect`.

### 6.5 — `delay(15_000)` clipboard-clear in composable click handler (P0-5)

Covered in P0-5.

### 6.6 — Missing `distinctUntilChanged` on heavy preference observers in `LibraryViewModel`

**File:** `LibraryViewModel.observeLibraryPreferences()`

Eight `launchIn` flows observe preferences without `distinctUntilChanged()`. If a preferences `DataStore` emits the same value twice (which is documented behavior during DataStore migration or if calling `setX(currentValue)`), all eight flows will trigger `_state.update` unnecessarily, cascading into the `observeFilteredItems()` combine, which will re-run the full 7-step filter pipeline. Add `.distinctUntilChanged()` before each `.onEach`.

---

## 7. Null Safety Audit

The codebase has **minimal `!!` usage** — safe casts (`as?`) and `?.let` are prevalent. No `!!` operators were found in the five primary files audited. However, the following null-adjacent risks exist:

### 7.1 — `checkNotNull(savedStateHandle["mangaId"])` in `ReaderViewModel`

**File:** `ReaderViewModel.kt:52`
```kotlin
private val mangaId: Long = checkNotNull(savedStateHandle["mangaId"])
private val chapterId: Long = checkNotNull(savedStateHandle["chapterId"])
```
`checkNotNull` throws `IllegalStateException` if the value is null — at ViewModel construction time. This is the correct pattern (fail-fast), but the exception is unhandled and will surface as a crash. A malformed deep link or incorrect navigation argument binding can trigger this. Add a `try/catch` in the NavGraph route's ViewModel creation or validate arguments at the navigation layer before navigating.

### 7.2 — `manga ?: return@launch` scattered across `DetailsViewModel` (~8 sites)

**File:** `DetailsViewModel.kt`
```kotlin
val manga = _state.value.manga ?: return@launch
```
This pattern appears in at least 8 functions (`openTracking`, `downloadChapter`, `deleteSelectedChapters`, `downloadAllChapters`, `loadSourceSuggestions`, `loadChapterThumbnail`, `shareManga`, `toggleNotifications`). The state's `manga` field is nullable (`Manga?`) because it is `null` during the loading phase. If the user somehow triggers one of these events before `loadMangaDetails()` emits (e.g. via a hardware back press during load, which recomposites the UI), the action silently no-ops with no user feedback.

**Fix:** Either:
1. Ensure these events are only dispatched when `manga != null` (disable UI elements during loading), or
2. Send a `ShowError` effect when `manga` is null: `manga ?: run { _effect.send(ShowError("Manga not loaded yet")); return@launch }`

### 7.3 — `state.value.chapters.find { it.id == chapterId }` returns nullable `ChapterItem?`

Multiple functions in `DetailsViewModel` use `.find { }` and then `?.let { }` guard:
```kotlin
val chapter = _state.value.chapters.find { it.id == chapterId }
chapter?.let { ... }
```
This is correct null-safe Kotlin. However, if `chapterId` does not exist in `_state.value.chapters`, the operation silently no-ops. For `toggleChapterRead` and `toggleChapterBookmark`, this means the user's toggle action produces no result and no error. At minimum, log a warning.

### 7.4 — `_state.value.manga?.title ?: "Manga"` fallback strings

**File:** `DetailsViewModel.kt` — `downloadSelectedChapters`, `deleteSelectedChapters`
```kotlin
val mangaTitle = manga?.title ?: "Manga"
val sourceName = manga?.sourceId?.toString() ?: ""
```
`"Manga"` as a fallback title for download directory names would create a naming conflict if multiple manga downloads fall back to the same title. The guard `val manga = _state.value.manga ?: return@launch` already ensures `manga != null` at this point, making the `?: "Manga"` unreachable. Remove the unreachable fallback or restructure so `manga` is guaranteed non-null by the time these strings are needed.

---

## 8. Suppression Audit

| File | Suppression | Justified? | Recommendation |
|------|------------|------------|----------------|
| `ReaderViewModel.kt` | `@Suppress("LargeClass")` | No — masks a god object | Remove after extracting per-domain VMs (§5.1) |
| `DetailsViewModel.kt` | `@Suppress("LargeClass")` | No — masks a god object | Remove after extracting sub-VMs (§5.2) |
| `DetailsViewModel.kt:onEvent` | `@Suppress("CyclomaticComplexMethod")` | No — dispatching is not inherently complex | Remove after decomposing responsibilities |
| `DetailsViewModel.kt:fetchThumbnails` | `@Suppress("CognitiveComplexMethod")` | Marginal — the async/nested structure is inherently complex | Move to a use case; complexity will reduce |
| `DetailsViewModel.kt:setDeleteAfterReadOverride` | `@Suppress("UnusedParameter")` | No — the method is dead code | Delete the method entirely |
| `DetailsMvi.kt` (file-level) | `@file:Suppress("MatchingDeclarationName")` | No — naming mismatch | Rename file to `DetailsContract.kt` |
| `ReaderScreen.kt` | `@Suppress("UnusedParameter")` | Marginal | Remove `mangaId`/`chapterId` params from composable or accept them |
| `SearchLibraryMangaUseCase.kt:matchesCriteria` | `@Suppress("CyclomaticComplexMethod")` | No — decomposable | Extract helper methods |
| `core/ui/*` (15 files) | `@file:Suppress("MaxLineLength")` | No | Wrap long lines to ≤120 chars |

**Total unjustified suppressions: 9** (not counting the 15 `MaxLineLength` files).

---

## 9. Patch Suggestions for P0 Items

### Patch for P0-1 & P0-4: `CancellationException` swallowing in `DetailsViewModel`

Apply to all `catch (e: Exception)` and `catch (_: Exception)` blocks:

```kotlin
// BEFORE (DetailsViewModel.kt — toggleFavorite, markSelectedAsRead, etc.)
} catch (e: Exception) {
    _effect.send(DetailsContract.Effect.ShowError("Failed to update library: ${e.message}"))
}

// AFTER
} catch (e: CancellationException) {
    throw e  // Always re-throw — structured concurrency depends on this
} catch (e: Exception) {
    _effect.send(DetailsContract.Effect.ShowError("Failed to update library: ${e.message}"))
}
```

For the blank-identifier catch in `fetchThumbnailsForDownloadedChapters`:
```kotlin
// BEFORE
} catch (_: Exception) {
    // Silently fail — thumbnails are optional
}

// AFTER
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    // Thumbnails are optional — silently ignore source errors
}
```

### Patch for P0-3: `observePageBookmarks` feedback loop in `ReaderViewModel`

```kotlin
// BEFORE
private fun observePageBookmarks() {
    viewModelScope.launch {
        var previousJob: Job? = null
        _state.collect { state ->
            previousJob?.cancel()
            previousJob = viewModelScope.launch {
                pageBookmarkRepository.isPageBookmarked(chapterId, state.currentPage)
                    .collect { isBookmarked ->
                        _state.update { it.copy(isCurrentPageBookmarked = isBookmarked) }
                    }
            }
        }
    }
}

// AFTER
private fun observePageBookmarks() {
    _state
        .map { it.currentPage }
        .distinctUntilChanged()
        .flatMapLatest { page ->
            pageBookmarkRepository.isPageBookmarked(chapterId, page)
        }
        .onEach { isBookmarked ->
            _state.update { it.copy(isCurrentPageBookmarked = isBookmarked) }
        }
        .launchIn(viewModelScope)
}
```

### Patch for P0-6: `sharePage()` silent no-op in `ReaderViewModel`

```kotlin
// BEFORE
private fun sharePage() {
    // Implementation for sharing current page
}

// AFTER — minimal fix: at least inform the user
private fun sharePage() {
    viewModelScope.launch {
        val page = _state.value.pages.getOrNull(_state.value.currentPage)
        if (page?.imageUrl == null) {
            _effect.send(ReaderEffect.ShowSnackbar("Nothing to share on this page"))
            return@launch
        }
        // TODO(#NNN): implement share intent via ACTION_SEND with page imageUrl
        _effect.send(
            ReaderEffect.ShowSnackbar(
                messageResId = R.string.reader_share_not_implemented
            )
        )
    }
}
```

### Patch for P1-14: `UpdateLibraryMangaUseCase` `CancellationException` swallowing

```kotlin
// BEFORE
return try {
    ...
} catch (e: Exception) {
    Result.failure(e)
}

// AFTER
return try {
    ...
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
```

### Patch for P1-5: `Flow.map { coroutineScope { } }` in `LibraryViewModel.loadLibrary()`

```kotlin
// BEFORE
getLibraryManga()
    .map { mangaList ->
        coroutineScope {
            val trackingDeferred = mangaList.map { manga -> async { ... } }
            val downloadDeferred = mangaList.map { manga -> async { ... } }
            val trackedIds = trackingDeferred.awaitAll().filterNotNull().toSet()
            val downloadedIds = downloadDeferred.awaitAll().filterNotNull().toSet()
            mangaList.map { manga -> manga.toLibraryItem(...) }
        }
    }
    .onEach { ... }
    .catch { ... }
    .launchIn(viewModelScope)

// AFTER — flatMapLatest cancels in-flight work when a new library emission arrives
getLibraryManga()
    .flatMapLatest { mangaList ->
        flow {
            val enriched = coroutineScope {
                val trackingDeferred = mangaList.map { manga -> async { ... } }
                val downloadDeferred = mangaList.map { manga -> async { ... } }
                val trackedIds = trackingDeferred.awaitAll().filterNotNull().toSet()
                val downloadedIds = downloadDeferred.awaitAll().filterNotNull().toSet()
                mangaList.map { manga -> manga.toLibraryItem(...) }
            }
            emit(enriched)
        }
    }
    .onEach { items ->
        _allItems.value = items
        _state.update { it.copy(isLoading = false, isRefreshing = false, error = null) }
    }
    .catch { error ->
        _state.update { it.copy(isLoading = false, isRefreshing = false, error = error.message) }
    }
    .launchIn(viewModelScope)
```

---

## 10. Prioritised Fix Order

| Priority | Item | Effort | Impact |
|----------|------|--------|--------|
| 🔴 P0 | Fix CI compilation errors (`core/ui`) | Medium | Unblocks all CI checks |
| 🔴 P0 | Fix `CancellationException` swallowing — all 12+ `DetailsViewModel` catch sites + `UpdateLibraryMangaUseCase` | Low | Correct coroutine cancellation |
| 🔴 P0 | Refactor `observePageBookmarks` to `flatMapLatest` | Low | Removes feedback loop |
| 🔴 P0 | Fix `DetailsViewModel` `savedStateHandle.get<Long>()` crash path | Low | Graceful crash on bad deep links |
| 🔴 P0 | Implement or stub `sharePage()` with user feedback | Low | No more silent failure |
| 🟠 P1 | `loadLibrary()`: change `map { coroutineScope }` to `flatMapLatest { flow }` | Low | Prevents stale data under rapid updates |
| 🟠 P1 | Remove dead code: `setDeleteAfterReadOverride` + all associated state fields | Low | Removes live preferences observation for dead feature |
| 🟠 P1 | Replace `refreshData()` stub with real source refresh | Medium | Pull-to-refresh actually refreshes |
| 🟠 P1 | Extract `ReaderSettingsState` sub-object to collapse 37-field `.copy()` | Medium | Prevents silent settings regression on new fields |
| 🟠 P1 | Deduplicate constants between `ReaderEvent.companion` and `ReaderViewModel.companion` | Trivial | Single source of truth |
| 🟡 P2 | Add `distinctUntilChanged()` to all 8 preference observers in `LibraryViewModel` | Trivial | Prevents unnecessary re-filtering |
| 🟡 P2 | Replace 7-step list-copy filter pipeline with sequence chain | Low | Performance on large libraries |
| 🟡 P2 | Move `Regex(...)` in `Chapter.toChapterItem()` to top-level constant | Trivial | Avoids recompilation per chapter |
| 🟡 P2 | Remove `else -> Unit` from handler functions; require sealed exhaustiveness | Low | Compiler-enforced event handling |
| 🟡 P2 | Remove `categoryFilterMangaIds` and `readingListMangaIds` from `LibraryState` | Low | Cleaner public state contract |

---

*Audit performed against commit `28a13cdd` (HEAD at time of audit).*
