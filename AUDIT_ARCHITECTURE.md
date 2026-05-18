# AUDIT_ARCHITECTURE.md — Otaku-Reader Architecture Audit

**Audit Date:** 2026-05-18  
**Codebase Snapshot:** Kotlin 2.3.21 · AGP 9.1.1 · Compose BOM 2026.04.01 · minSdk 26 · targetSdk/compileSdk 36

---

## 1. Executive Summary

**Overall Grade: B−**

Otaku Reader is structurally sound at the macro level — Clean Architecture layer separation is respected by the build graph, MVI is consistently applied with named State/Intent/Effect contracts per feature, and the build-logic convention system enforces a uniform module shape across all fourteen feature modules. However five specific problems prevent a higher grade:

1. Three presentation-layer **god objects** (DetailsScreen, DetailsViewModel, ReaderViewModel) have grown beyond any reasonable single-responsibility boundary and represent active maintenance and testability risk.
2. **Ten import-level layer violations** where feature and app modules bypass the domain interface contract to import data-layer concretions directly.
3. The tachiyomi-compat bridge uses **`Observable.toBlocking()`** on `Dispatchers.IO` across all seven source API call sites — silently exhausts the IO thread pool under concurrent source requests.
4. **Six use cases** are declared in domain but are either unreferenced or only referenced in test scaffolding.
5. No Gradle module boundary enforcement exists to prevent future feature→data coupling.

None of these require restructuring the module graph — they are all fixable at the file level.

---

## 2. Module Dependency Graph

```
┌────────────────────────────────────────────────────────────────┐
│                           app/                                  │
│    (entry point, NavHost, Hilt root, Glance widgets)           │
└──────────────────────────┬─────────────────────────────────────┘
                           │ implementation (all modules below)
          ┌────────────────┼────────────────────────────┐
          ▼                ▼                             ▼
┌──────────────────┐  ┌────────────────────────────────────────┐
│   feature/* (14) │  │  core/* (11 submodules)                │
│                  │  │  common · network · database ·         │
│  library         │  │  preferences · ui · navigation ·       │
│  reader          │  │  extension · tachiyomi-compat ·        │
│  browse          │  │  discord · ai · ai-noop                │
│  details         │  └──────────────────┬───────────────────  ┘
│  updates         │                     │ uses
│  history         │        ┌────────────┘
│  settings        │        ▼
│  statistics      │  ┌──────────────────────┐   ┌──────────────────────┐
│  migration       │  │      domain/          │   │     source-api/       │
│  tracking        │  │  UseCases             │   │  MangaSource (modern) │
│  onboarding      │  │  Repository ifaces    │   │  HttpSource (depr.)   │
│  about           │  │  Domain models (26+)  │   └────────┬─────────────┘
│  opds            │  └──────────┬────────────┘            │ wraps
│  feed            │             │ implements               ▼
│  more            │             ▼               TachiyomiSourceAdapter
└──────┬───────────┘  ┌──────────────────────┐   (core/tachiyomi-compat)
       │              │       data/           │
       │ depends on   │  RepoImpls (13)       │
       │ (via conv.)  │  Workers, Backup,     │
       └─────────────►│  Tracking, OPDS       │
                      └──────────────────────┘

Additional:
  server/ — standalone Ktor sync server (not wired into app/ build graph)
  baselineprofile/ — depends on app/ for macro benchmark runs
  build-logic/ — standalone included build, Gradle plugin DSL only
```

### Dependency Health

| Dependency | Direction | Healthy? |
|---|---|---|
| `feature/* → domain` | via convention plugin | Yes |
| `feature/* → core/ui, core/navigation` | via convention plugin | Yes |
| `data → domain` | implements interfaces | Yes |
| `data → core/database, core/network` | Yes | Yes |
| `feature/details → data` | direct (build.gradle.kts) | **Violation** |
| `feature/reader → data` | direct (build.gradle.kts) | **Violation** |
| `feature/settings delegates → data` | import-level (4 files) | **Violation** |
| `feature/updates → data.worker` | import-level | **Violation** |
| `feature/tracking → data.tracking` | import-level | **Violation** |

---

## 3. Architecture Assessment

### Clean Architecture Compliance

**Domain layer: Clean.** No Android imports, no DI annotations beyond `@Inject`, no data-layer references. 30 use cases are pure Kotlin. 16 repository interfaces contain no implementation.

**Data layer: Clean.** Correctly implements domain interfaces. Entity-to-domain mapping via `EntityMappers.kt`. No upward dependency on feature modules.

**Presentation layer: Mostly correct.** 14 feature modules own their ViewModels, depend on domain use cases via Hilt, emit `StateFlow<State>` consumed via `collectAsStateWithLifecycle`. MVI contracts consistently named (`*Mvi.kt` with `State`, `Intent`, `Effect`).

### Layer Violations (10 files)

| Violating File | Imported Data Class | Correct Fix |
|---|---|---|
| `feature/details/build.gradle.kts` | `implementation(projects.data)` | Remove; gate behind use cases |
| `feature/reader/build.gradle.kts` | `implementation(projects.data)` | Remove; gate behind use cases |
| `feature/updates/UpdatesViewModel.kt` | `data.worker.LibraryUpdateWorker` | Introduce `LibraryUpdateScheduler` domain interface |
| `feature/settings/SettingsViewModel.kt` | `data.worker.ReadingReminderScheduler` | Introduce domain `ReminderScheduler` interface |
| `feature/settings/delegate/BackupSettingsDelegate.kt` | `data.backup.*` (3 classes) | Domain use cases |
| `feature/settings/delegate/LibrarySettingsDelegate.kt` | `data.worker.LibraryUpdateScheduler` | Domain interface |
| `feature/settings/delegate/ReaderSettingsDelegate.kt` | `data.repository.ReaderSettingsRepository` | Use domain `ReaderSettingsRepository` interface (already exists) |
| `feature/settings/delegate/TrackerSyncSettingsDelegate.kt` | `data.tracking.TrackManager` | Domain interface |
| `feature/tracking/TrackerOAuthViewModel.kt` | `data.tracking.TrackManager` | Domain `TrackManager` interface |
| `app/MainActivity.kt` | data layer init | Acceptable at app root |

---

## 4. God Object Register

| File | Lines | Core Problem | Split Strategy |
|---|---|---|---|
| `feature/details/DetailsScreen.kt` | **1,722** | Renders header, chapters, source suggestions, note editor, filter sheet, panorama cover, tracker status in one composable tree | Extract: `MangaHeaderSection`, `SourceSuggestionsSection`, `NoteEditorBottomSheet`, `ChapterFilterBottomSheet` |
| `feature/details/DetailsViewModel.kt` | **949** | Handles manga loading, chapter CRUD, downloads, tracker nav, note editing, per-manga reader settings, source suggestions — 9 concerns | Split into `MangaDetailViewModel`, `ChapterListViewModel`, `ChapterDownloadViewModel` + thin coordinator |
| `feature/reader/ReaderViewModel.kt` | **863** | Coordinates page loading, chapter nav, history, download-ahead, Discord, prefetch, settings, zoom — delegates extracted but ViewModel still wires all | Push chapter nav state fully into `ReaderChapterLoaderDelegate`; reduce ViewModel to dispatcher only |
| `feature/library/LibraryScreen.kt` | **967** | Grid/list rendering, category tabs, search bar, FAB, multi-select toolbar, filter chips, empty state | Extract: `LibraryCategoryTabs`, `LibrarySearchBar`, `LibraryMultiSelectToolbar`, `LibraryMangaGrid` |
| `feature/library/LibraryViewModel.kt` | **637** | Library loading, search, sort/filter, category selection, multi-select, "For You" | Extract `LibraryFilterViewModel` for sort/filter state |

---

## 5. Source API Schism

Two source contracts co-exist in `source-api/`:

| Contract | Style | Status |
|---|---|---|
| `MangaSource` | suspend functions, Otaku Reader types | **Current standard** |
| `HttpSource` | `@Deprecated`, RxJava 1.x Observables, Tachiyomi types | Retained for community extension compatibility |

**Critical: `TachiyomiSourceAdapter` uses `.toBlocking().first()` at 7 call sites.**

```kotlin
// Current — blocks Dispatchers.IO thread:
val mangasPage = tachiyomiSource.fetchPopularManga(page).toBlocking().first()

// Fix — non-blocking suspension:
private suspend fun <T> Observable<T>.awaitFirst(): T = suspendCancellableCoroutine { cont ->
    val subscription = first().subscribe(cont::resume, cont::resumeWithException)
    cont.invokeOnCancellation { subscription.unsubscribe() }
}
```

Under concurrent source requests (global search, background library update), `.toBlocking()` holds a real thread from the 64-thread IO pool per call, causing `TimeoutException` or request hangs when the pool exhausts.

---

## 6. Dead Code Inventory

| Item | File | Action |
|---|---|---|
| `GetHistoryUseCase` provided as `@Singleton` via `@Provides` in `UseCaseModule` | `data/di/UseCaseModule.kt` | Remove `@Provides`; let Hilt inject directly via constructor |
| `SearchLibraryMangaUseCase` same issue | `data/di/UseCaseModule.kt` | Remove `@Provides` |
| `TachiyomiModelsAdapter` declared `public object` but only used internally by `TachiyomiSourceAdapter` | `core/tachiyomi-compat/` | Make `internal` or merge into adapter |
| EU package stubs (`eu.kanade.tachiyomi.*`) | `core/tachiyomi-compat/` | **Do not remove** — required for extension classloading via reflection |

---

## 7. Module Boundary Enforcement Gap

The `AndroidFeatureConventionPlugin` enforces standard dependencies but **nothing prevents**:
- Adding `implementation(projects.data)` to a feature module
- Importing `app.otakureader.data.*` from a feature ViewModel

**Recommended fixes:**

1. Add Detekt `ForbiddenImport` rule:
```yaml
ForbiddenImport:
  active: true
  imports:
    - value: 'app.otakureader.data.*'
      reason: 'Use domain interfaces instead.'
  excludes: ['**/data/**', '**/app/**', '**/test/**']
```

2. Remove `implementation(projects.data)` from `feature/details/build.gradle.kts` and `feature/reader/build.gradle.kts`.

3. Fix one-liner: `feature/settings/delegate/ReaderSettingsDelegate.kt` — change `import app.otakureader.data.repository.ReaderSettingsRepository` → `import app.otakureader.domain.repository.ReaderSettingsRepository` (domain interface already exists).

---

## 8. Dependency Health

| Dependency | Current | Status | Notes |
|---|---|---|---|
| Kotlin | 2.3.21 | Current | |
| AGP | 9.1.1 | Current | |
| KSP | 2.3.7 | Current | |
| Compose BOM | 2026.04.01 | Current | |
| Coroutines | 1.10.2 | Current | |
| Hilt | 2.59.2 | Current | |
| Room | 2.8.4 | Current | |
| OkHttp | 4.12.0 | Watch | OkHttp 5 in alpha; 4.x maintained |
| Coil | 3.4.0 | Current | |
| RxJava | 1.3.8 | **Pinned intentionally** | EOL but required for Tachiyomi extension API — do NOT upgrade |
| RxAndroid | 1.2.1 | **Pinned intentionally** | Same constraint |
| Kover | 0.8.3 | Minor behind | 0.9.x available; non-blocking |

**All other dependencies current.** Netty pinned to 4.2.12.Final via `resolutionStrategy` with documented CVE rationale.

---

## 9. Red Flags

### P0 — Production-Impacting

| ID | Issue | File | Impact |
|---|---|---|---|
| RF-01 | `TachiyomiSourceAdapter` blocks IO threads via `.toBlocking().first()` at 7 sites | `core/tachiyomi-compat/TachiyomiSourceAdapter.kt` | IO pool exhaustion under concurrent source requests |
| RF-02 | `feature/details` has `implementation(projects.data)` — direct build-graph violation | `feature/details/build.gradle.kts` | Any data class rename breaks details without domain-boundary compile error |
| RF-03 | `feature/reader` has `implementation(projects.data)` — same violation | `feature/reader/build.gradle.kts` | Same for reader, the most user-facing screen |

### P1 — Maintainability Risk

| ID | Issue | File | Impact |
|---|---|---|---|
| RF-04 | `DetailsViewModel.kt` (949 lines) handles 9 unrelated concerns | `feature/details/DetailsViewModel.kt` | Any change risks cross-concern regression; enormous test surface |
| RF-05 | `ReaderViewModel.kt` (863 lines) orchestrates 6 delegates inline | `feature/reader/ReaderViewModel.kt` | Prefetch changes require reading all 863 lines |
| RF-06 | 10 production files bypass domain interface contract | Various feature modules | Data layer cannot be refactored without touching feature code |
| RF-07 | `UseCaseModule` provides use cases as `@Singleton` inconsistently | `data/di/UseCaseModule.kt` | Two use cases are accidentally singletons; rest are ephemeral |
| RF-08 | No module boundary enforcement in Gradle or Detekt | Convention plugins | Existing 10 violations will grow unchecked |

### P2 — Technical Debt

| ID | Issue | Impact |
|---|---|---|
| RF-09 | `LibraryScreen.kt` (967 lines) needs composable extraction | Screenshot testing impossible at component level |
| RF-10 | `HttpSource` deprecated with no removal timeline | Extensions on deprecated path get no migration signal |
| RF-11 | `TachiyomiModelsAdapter` is `public object` but is an implementation detail | Accidental external use cannot be prevented |
| RF-12 | `ReaderSettingsDelegate` imports `data.repository` instead of `domain.repository` | 1-line fix ignored |
| RF-13 | No coverage gate for `feature/*` modules | ViewModel/reducer logic has no coverage floor |

---

## 10. Top Recommendations

| Priority | Rec | Effort |
|---|---|---|
| P0 | Replace `.toBlocking().first()` in `TachiyomiSourceAdapter.kt` with `suspendCancellableCoroutine` wrapper | 1 day |
| P0 | Remove `implementation(projects.data)` from `feature/details` and `feature/reader` build files | 2–3 days |
| P0 | Fix `ReaderSettingsDelegate` 1-line import to use domain interface | 30 min |
| P1 | Add Detekt `ForbiddenImport` rule for `app.otakureader.data.*` in feature modules | 2 hours |
| P1 | Remove `@Provides` from `UseCaseModule` for `GetHistoryUseCase`/`SearchLibraryMangaUseCase` | 30 min |
| P1 | Split `DetailsViewModel.kt` into 3 focused ViewModels | 3–5 days |
| P1 | Split `DetailsScreen.kt` into 5 extracted composable files | 2–3 days |
| P1 | Complete `ReaderViewModel` delegate extraction (push nav + settings state fully into delegates) | 2 days |
| P2 | Document `HttpSource` removal timeline in KDoc; create tracking issue | 1 hour |
| P2 | Add 50% coverage gate for `:feature:reader`, `:feature:tracking`, `:feature:settings` | 1 hour |

*Audit conducted at commit `28a13cdd6e` baseline. Ruflo agent: `architect-scout` (ruflo-adr + ruflo-docs plugins).*
