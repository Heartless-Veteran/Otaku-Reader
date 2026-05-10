# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Project Is

Otaku Reader is a manga-only Android app (~98% complete, in bug-fixing/stabilization). It is a clean-room alternative to Mihon/Tachiyomi that reuses the Tachiyomi extension ecosystem. The developer is newer to Kotlin — always explain what was wrong and why a fix works alongside any code change.

## Commands

```bash
# Build
./gradlew assembleDebug                    # debug APK → app/build/outputs/apk/debug/

# Tests
./gradlew testDebugUnitTest               # all unit tests
./gradlew :domain:koverVerify             # coverage gate ≥60% on :domain
./gradlew :data:koverVerifyDebug          # coverage gate ≥60% on :data
./gradlew :core:database:test             # migration tests only

# Lint / style
./gradlew detekt                          # static analysis
./gradlew ktlintCheck                     # style check
./gradlew ktlintFormat                    # auto-fix style

# Security
bash scripts/check-buildconfig-security.sh   # scan BuildConfig for hardcoded credentials

# Misc
./gradlew :app:generateLicenseReport      # → docs/DEPENDENCY_LICENSES.md
./gradlew :app:kspDebugKotlin             # compile Hilt graph (fast DI error check)
```

All five of these must pass before any PR merges to `main`: security check, detekt, ktlint, unit tests, assemble.

## Architecture

Three horizontal layers (inner to outer):

```
domain/          Pure Kotlin — use cases, repository interfaces, domain models. No Android deps.
data/            Repository implementations, Room DAOs, WorkManager workers, download/backup/tracking/OPDS.
feature/*/       Compose screens + HiltViewModels per feature.
```

Shared infrastructure lives in `core/` sub-modules:
- `core/common` — utilities, `Result<T>`, `ReadTimeEstimator`, MVI base classes
- `core/database` — Room v21 with explicit migrations (v2→v21)
- `core/preferences` — DataStore wrappers (`GeneralPreferences`, `ReaderPreferences`, `DownloadPreferences`, `PendingOAuthStore`, `EncryptedOpdsCredentialStore`, etc.)
- `core/ui` — Material 3 design system, dynamic color from cover art, MVI effect collector
- `core/navigation` — type-safe Compose Navigation routes
- `core/extension` — `ExtensionLoader`, `TrustedSignatureStore` (EncryptedSharedPreferences-backed)
- `core/tachiyomi-compat` — bridges Tachiyomi APKs to `source-api` interfaces
- `source-api` — pure Kotlin contract (`Source`, `HttpSource`, `SManga`, `SChapter`, `Page`, `MangasPage`, `Filter`). No Android deps.

### Module Conventions

- **Feature modules** — `feature/<name>/`, apply `otakureader.android.feature` convention plugin. Each feature is self-contained with its own Screen, ViewModel, and MVI contract.
- **Core modules** — `core/<name>/`, shared infrastructure. Each core module has its own DI module in `<module>/di/*Module.kt`.
- **Domain module** — `domain/`, pure Kotlin library (no Android plugin). Use cases, repository interfaces, and domain models only.
- **Data module** — `data/`, repository implementations and data sources. Binds interfaces to implementations here.

### Dependency Rules

- `domain` has **zero** external deps (only Kotlin stdlib, coroutines, and `javax.inject`).
- `data` depends on `domain` + `core/*`.
- `feature/*` depends on `domain` + `core/*`.
- **No feature module may depend on another feature module.** Navigation is via `core/navigation` routes only.
- `app/` module depends on all feature modules and core modules, wires the nav graph.

## MVI Pattern

Every feature uses Model-View-Intent. There are two patterns in the codebase:

### Legacy Pattern (most existing features)
ViewModel extends `ViewModel` directly and wires state/effect manually:

```kotlin
@HiltViewModel
class LibraryViewModel @Inject constructor(...) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _effect = Channel<LibraryEffect>(Channel.BUFFERED)
    val effect: Flow<LibraryEffect> = _effect.receiveAsFlow()

    fun onEvent(event: LibraryEvent) { ... }
}
```

### Modern Pattern (newer features — Details, OPDS, Tracking)
Uses `BaseMviViewModel<S, E, F>` from `core/common` with contract objects:

```kotlin
// MVI contract file: DetailsMvi.kt
object DetailsContract {
    data class State(...) : UiState
    sealed interface Event : UiEvent { ... }
    sealed interface Effect : UiEffect { ... }
}

// ViewModel
@HiltViewModel
class DetailsViewModel @Inject constructor(...)
    : BaseMviViewModel<DetailsContract.State, DetailsContract.Event, DetailsContract.Effect>(
        initialState = DetailsContract.State()
    ) {
    override fun processEvent(event: DetailsContract.Event) { ... }
}
```

### Screen: always collectAsStateWithLifecycle, never collectAsState

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

Collect effects with `CollectAsEffect` from `core/ui`:

```kotlin
CollectAsEffect(viewModel.effect) { effect ->
    when (effect) {
        is LibraryEffect.NavigateToManga -> navigator.navigate(...)
    }
}
```

Never mutate state directly (use `_state.update { it.copy(...) }`). Never use `LiveData`. Never use `GlobalScope`.

## Naming Conventions

### Composables
- **Screen** — `<Feature>Screen.kt` (top-level route composable, e.g., `LibraryScreen()`)
- **Content** — `<Feature>Content()` (stateless body, used by Screen after collecting state)
- **Components** — Descriptive names: `MangaCard()`, `ChapterListItem()`, `FilterBottomSheet()`

### ViewModels
- `<Feature>ViewModel.kt` — always annotated `@HiltViewModel`
- State class: `<Feature>State` (or `DetailsContract.State` for modern MVI)
- Event sealed class: `<Feature>Event` (or `DetailsContract.Event`)
- Effect sealed class: `<Feature>Effect` (or `DetailsContract.Effect`)
- Entry point: `fun onEvent(event: <Feature>Event)`

### Use Cases
- `<Verb><Noun>UseCase.kt` — e.g., `GetLibraryMangaUseCase`, `ToggleFavoriteMangaUseCase`
- Single public method: `operator fun invoke(...)`
- Located in `domain/usecase/` (or `domain/usecase/<subpackage>/` for grouped use cases like OPDS)

### Repositories
- Interface: `<Noun>Repository` in `domain/repository/`
- Implementation: `<Noun>RepositoryImpl` in `data/repository/` (or `data/<feature>/repository/`)

### DI Modules
- Repository bindings: `data/di/RepositoryModule.kt` (`@Module @InstallIn(SingletonComponent::class)`)
- Use case providers: `data/di/UseCaseModule.kt` (`@Module @InstallIn(SingletonComponent::class)`)
- Feature-specific: `data/<feature>/di/<Feature>Module.kt` (e.g., `data/tracking/di/TrackingModule.kt`)
- Core modules: each has its own `<core>/<name>/di/<Name>Module.kt`

## Hilt DI Rules

- Repositories → `@Singleton`, bound in `data/di/RepositoryModule.kt`
- Use cases → `@Reusable` or unscoped
- ViewModels → `@HiltViewModel`
- All `@Binds`/`@Provides` must have matching `@InstallIn` scope
- Run `./gradlew :app:kspDebugKotlin` to catch missing bindings at compile time without running full tests

**Resolved (as of latest audit):**
- `TrackerSyncRepository` → implemented by `TrackerSyncRepositoryImpl` at `data/tracking/repository/TrackerSyncRepositoryImpl.kt`, bound via `data/tracking/di/TrackingModule.kt`
- `ExtensionManagementRepository` → bound to `SourceRepositoryImpl` in `data/di/RepositoryModule.kt` (line 106)

## Testing Conventions

### Framework & Libraries
- **JUnit 4** with `runTest { }` for coroutines
- **MockK** for mocking (not Mockito)
- **Turbine** (`app.cash.turbine`) for Flow assertions
- **StandardTestDispatcher** + `@OptIn(ExperimentalCoroutinesApi::class)` for coroutine testing

### ViewModel Tests
- Mock all dependencies with `mockk { every { prop } returns flowOf(...) }` for DataStore preferences
- Use `Dispatchers.setMain(testDispatcher)` in `@Before`, `Dispatchers.resetMain()` in `@After`
- Assert state changes via `viewModel.state.test { ... }` or direct property access after `advanceUntilIdle()`
- Test both success and error paths

### Use Case Tests
- Mock the repository interface, verify `invoke()` delegates correctly
- Test empty/success/error cases with Turbine

### Domain Architecture Tests
- `domain/src/test/java/app/otakureader/domain/ArchitectureTest.kt` enforces:
  - No Android imports in domain layer (banned: `android.`, `androidx.`, `com.google.android.`)
  - Domain data classes must live in `domain/model/`
  - Domain repository package contains only interfaces
  - Use cases must exist in `domain/usecase/`
  - Domain build file uses Kotlin library plugin (not Android)

### Coverage Gates
- `:domain` — ≥60% (enforced by `domain:koverVerify`)
- `:data` — ≥60% (enforced by `data:koverVerifyDebug`)
- Migration tests: `core/database` only (use `./gradlew :core:database:test`)

## Room Database Rules

- All DAO reads return `Flow<T>`, never a plain value
- `fallbackToDestructiveMigration()` is only enabled in debug builds (`BuildConfig.DEBUG`) — never in production
- Current DB version: 21. Migrations live in `core/database/`. All are additive (no destructive changes)
- Adding a migration: increment version → write `Migration(N, N+1)` → add to `.addMigrations(...)` → write a `MigrationTestHelper` test → commit the exported schema JSON

## Extension System (Non-Negotiable)

Tachiyomi extension compatibility must never be broken. Extensions (Keiyoushi ~1000, Komikku ~1000) are community-maintained APKs loaded at runtime via `DexClassLoader`. The `source-api` module mirrors Tachiyomi's source package types exactly.

Never change `source-api` public signatures without verifying against the Tachiyomi extension API spec. Never execute extension code on the main thread. Extensions get the app's OkHttp client (respects user proxy/VPN). Always validate signatures via `TrustedSignatureStore` before loading.

Flow: `ExtensionLoader` → validate signature → load DEX → instantiate `CatalogueSource` → wrap via `TachiyomiSourceAdapter` → expose as `SourceRepository`.

## DataStore / Preferences

All settings use `Preferences DataStore` — never `SharedPreferences` for new settings. Preference classes: `GeneralPreferences`, `LibraryPreferences`, `ReaderPreferences`, `DownloadPreferences`, `BackupPreferences`, `ReadingGoalPreferences`, `EncryptedApiKeyStore`, `EncryptedOpdsCredentialStore`, `PendingOAuthStore`. Every class exposes `Flow<T>` for reads and `suspend fun setXxx()` for writes.

Batch DataStore reads with `async/await` to avoid sequential blocking on cold start.

## Reader Engine

Four modes share `ReaderState` but have mode-specific composables:
- **Single Page** — `HorizontalPager` with `beyondViewportPageCount = 1`
- **Dual Page** — auto-detects spreads by aspect ratio (threshold 1.2)
- **Webtoon** — `LazyColumn` with `contentType = { "manga_page" }`, configurable gap
- **Smart Panels** — ML Kit panel detection, results cached in `LruCache<String, 50>`, falls back to single-page on failure

Reader events are grouped via sealed interfaces by domain (`PageNavigation`, `ZoomControl`, `DisplayControl`, etc.) so handlers can filter entire domains.

Performance rules: use `RGB_565` for opaque pages (2x memory reduction), preload adjacent pages, call `clearCache()` in `ViewModel.onCleared()`, cap Coil memory cache at `min(15%, 256 MB)`.

## Toolchain Version Coupling

Kotlin, KSP, Compose Compiler plugin, Compose BOM, and AGP versions are tightly coupled. The single source of truth is `gradle/libs.versions.toml`. SDK levels (`compileSdk`, `targetSdk`, `minSdk`) are also centralized there — never hardcode them in any `build.gradle.kts`. Bump the toolchain group as a single commit. Renovate groups these into one PR and never auto-merges them.

Current: Kotlin 2.3.21 · KSP 2.3.7 · AGP 9.1.1 · Compose BOM 2026.04.01 · compileSdk/targetSdk 36 · minSdk 26.

## Hard Rules

- No AI features in this repo (Gemini, ML beyond Smart Panels) — use the separate [Otaku-Reader-AI](https://github.com/HeartlessVeteran2/Otaku-Reader-AI) repo
- No Firebase, analytics, or crash tooling unless explicitly requested
- No XML layouts — pure Compose
- No `GlobalScope` — use `viewModelScope` or Hilt-injected `@ApplicationScope`
- No `LiveData`
- No hardcoded strings — use resource files
- No `!!` — use `?.let`, `?: return`, or explicit null handling
- Never call `source.fetchPageList()` directly — always route through `SourceRepository`
- No secrets in `BuildConfig` — use encrypted DataStore stores or Gradle properties

## Commit Format

```
type(scope): subject
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`. One PR per concern. Branch from `main` as `feature/…` or `fix/…`.
