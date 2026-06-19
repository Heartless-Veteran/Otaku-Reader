# Otaku Reader — Project Overview

## Identity
- **Name:** Otaku Reader
- **Platform:** Android 8.0+ (API 26+)
- **Language:** Kotlin 2.3.21
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture + MVI
- **Status:** Production release preparation (all beta parity + hardening + polish shipped)

## Module Structure

```
app/                    Main entry point, DI wiring, navigation host, widgets
data/                   Repository implementations, downloads, backup, sync, workers
domain/                 Use cases, domain models, repository interfaces (pure Kotlin)
source-api/             Extension API contracts (what extensions implement)
core/                   Shared infrastructure
  ├── common/           Utilities, extensions, coroutine helpers
  ├── database/         Room v37 with explicit migrations
  ├── network/          OkHttp + Retrofit + Kotlinx Serialization
  ├── preferences/      DataStore wrappers, encrypted credential storage
  ├── ui/               Compose design system, Material 3 theme, Coil integration
  ├── navigation/       Type-safe Compose Navigation routing
  ├── extension/        ExtensionLoader, TrustedSignatureStore
  ├── tachiyomi-compat/ RxJava 1.x stubs + Tachiyomi interface bridges
  └── discord/          Discord Rich Presence (native, no external library)
feature/                UI + ViewModel per screen, each self-contained
  ├── library/          Main manga collection, categories, filtering, undo bulk delete
  ├── browse/           Source browsing, global search (Paging 3)
  ├── details/          Manga info, chapter list, tracker status
  ├── reader/           All reading modes + page bookmarks + smart download trigger
  ├── history/          Reading history timeline with undo batch delete
  ├── updates/          New chapter updates with undo bulk mark-as-read
  ├── settings/         All app settings, backup/restore, tracker auth
  ├── statistics/       Reading stats dashboard, streaks, heatmap
  ├── migration/        Source-to-source migration + Tachiyomi/Mihon/Komikku import
  ├── tracking/         Tracker integrations (MAL, AniList, Kitsu, MangaUpdates, Shikimori)
  ├── onboarding/       First-run setup wizard
  ├── about/            About screen, credits, version info
  ├── opds/             OPDS catalog support (Komga/Kavita)
  ├── feed/             Recommendations and activity feed
  └── more/             Bottom nav "More" section, QR sharing
```

## Key Conventions

### Architecture Pattern: MVI
Every feature follows Model-View-Intent:
- **State:** Immutable data class, `StateFlow<UiState>` from ViewModel
- **Event:** Sealed class for user/system actions
- **Effect:** One-shot side effects via `Channel<Effect>` (navigation, snackbars)
- **ViewModel:** Processes events → updates state → emits effects

Files always named: `FeatureMvi.kt`, `FeatureViewModel.kt`, `FeatureScreen.kt`.

### Dependency Injection
- Uses **Hilt** throughout
- Repository interfaces in `domain/` → implementations in `data/`
- ViewModels receive UseCases, not Repositories directly
- `@ApplicationScope` CoroutineScope for long-lived background work

### Navigation
- Compose Navigation with type-safe routes (`@Serializable` data classes)
- Deep linking support for manga and chapter pages

### Async Patterns
- `Flow` for data streams; `StateFlow` for UI state
- `viewModelScope` for ViewModel-bound work — never `GlobalScope`
- `async/await` for parallel independent DataStore reads (batch them in `loadSettings()`)
- `collectAsStateWithLifecycle()` in Compose — never `collectAsState()`

## Extension System (Non-Negotiable)
- Extensions are APKs implementing `source-api` interfaces
- Supported repos: Keiyoushi (~1000 sources), Komikku (~1000 sources)
- Loaded dynamically via `ExtensionLoader` in `core/extension/`
- `TrustedSignatureStore` validates signing certificates per extension
- **Never modify `source-api/` interfaces or `tachiyomi-compat/` stubs** — this breaks all 500+ extensions

## Reader Engine
Four reading modes:
1. **Single Page** — Horizontal pager, classic manga pagination
2. **Dual Page** — Spread view with landscape auto-detection
3. **Webtoon** — Vertical scroll with configurable gap, pre-rendered for smooth scroll
4. **Smart Panels** — ML Kit panel detection, steps through individual panels

Reader events grouped by domain (Navigation, Zoom, Display) via sealed interfaces.
Per-manga overrides store mode, direction, color filter, tint per series.

## Undo Patterns (both in use)

### Pattern A — Immediate-delete + Re-add (Library)
Delete from DB first, show snackbar. Undo calls the same boolean toggle again to re-add.
See `LibraryViewModel.removeSelectedFromLibrary()` + `undoLibraryDelete()`.

### Pattern B — Delayed-delete + Pending Filter (History, Updates)
Add IDs to `pendingDeleteIds: MutableStateFlow<Set<Long>>` immediately (UI filters them out).
Start a 4-second delay job. Undo cancels job and restores IDs. Track `pendingBatchDeleteIds`
to guard stale undo from an old snackbar cancelling the wrong batch.
See `HistoryViewModel.removeSelectedFromHistory()` + `undoBatchRemoveFromHistory()`.

## Stable Flow in Compose
Wrap flows derived inside composables with `remember(key) { ... }` to prevent resubscription on recomposition:
```kotlin
val count by remember(repository) {
    repository.observeSomething().map { ... }.distinctUntilChanged()
}.collectAsStateWithLifecycle(0)
```

## Testing
- `runTest { }` for all suspend functions
- Turbine `.test { awaitItem() }` for all Flow assertions
- MockK for mocking; `mockk<Repo>(relaxed = true)` for ignoring irrelevant calls
- `advanceUntilIdle()` after triggering a delayed-delete to run through the delay before asserting DB calls
- In-memory Room databases for DAO tests
- Roborazzi for screenshot regression tests

## When Working on This Project
- Always check if a pattern exists in another feature before inventing a new one
- Prefer `data class` states with `copy()` over mutable state
- Use `rememberSaveable` for UI state that survives config changes
- Coil 3 for images — always consider memory impact; RGB_565 for opaque pages
- Never call `source.fetchPageList()` directly — always route through `SourceRepository`
- Never stub a live UI element — if the button/preference exists, wire it to the real implementation
- Every destructive bulk action needs an undo snackbar
