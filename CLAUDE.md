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
- `core/common` — utilities, `Result<T>`, `ReadTimeEstimator`
- `core/database` — Room v9 with explicit migrations (v2→v9)
- `core/preferences` — DataStore wrappers (`GeneralPreferences`, `ReaderPreferences`, `DownloadPreferences`, etc.)
- `core/ui` — Material 3 design system, dynamic color from cover art
- `core/navigation` — type-safe Compose Navigation routes
- `core/extension` — `ExtensionLoader`, `TrustedSignatureStore`
- `core/tachiyomi-compat` — bridges Tachiyomi APKs to `source-api` interfaces
- `source-api` — pure Kotlin contract (`Source`, `HttpSource`, `SManga`, `SChapter`, `Page`, `MangasPage`, `Filter`). No Android deps.

Dependency rule: `domain` has zero deps. `data` depends on `domain`. `feature/*` depends on `domain` + `core/*`. No feature module may depend on another feature module.

## MVI Pattern

Every feature uses Model-View-Intent:

- **State** — immutable `data class`, one per screen
- **Event** — `sealed class`/`sealed interface` representing user or system actions
- **Effect** — one-shot `SharedFlow` for navigation, snackbars
- **ViewModel** — `@HiltViewModel`, exposes `StateFlow<UiState>`, processes events via `onEvent()`

```kotlin
// ViewModel template
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryManga: GetLibraryMangaUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    fun onEvent(event: LibraryEvent) { ... }
}

// Screen: always collectAsStateWithLifecycle, never collectAsState
val state by viewModel.state.collectAsStateWithLifecycle()
```

Never mutate state directly. Never use `LiveData`. Never use `GlobalScope`.

## Hilt DI Rules

- Repositories → `@Singleton`, bound in `data/di/RepositoryModule.kt`
- Use cases → `@Reusable` or unscoped
- ViewModels → `@HiltViewModel`
- All `@Binds`/`@Provides` must have matching `@InstallIn` scope
- Run `./gradlew :app:kspDebugKotlin` to catch missing bindings at compile time without running full tests

Two orphan interfaces in `domain/repository/` (as of last audit): `TrackerSyncRepository` (no impl — delete it) and `ExtensionManagementRepository` (no binding — wire to `SourceRepositoryImpl`).

## Room Database Rules

- All DAO reads return `Flow<T>`, never a plain value
- `fallbackToDestructiveMigration()` is only enabled in debug builds (`BuildConfig.DEBUG`) — never in production
- Current DB version: 9. Migrations live in `core/database/`. All are additive (no destructive changes)
- Adding a migration: increment version → write `Migration(N, N+1)` → add to `.addMigrations(...)` → write a `MigrationTestHelper` test → commit the exported schema JSON

## Extension System (Non-Negotiable)

Tachiyomi extension compatibility must never be broken. Extensions (Keiyoushi ~1000, Komikku ~1000) are community-maintained APKs loaded at runtime via `DexClassLoader`. The `source-api` module mirrors Tachiyomi's source package types exactly.

Never change `source-api` public signatures without verifying against the Tachiyomi extension API spec. Never execute extension code on the main thread. Extensions get the app's OkHttp client (respects user proxy/VPN). Always validate signatures via `TrustedSignatureStore` before loading.

Flow: `ExtensionLoader` → validate signature → load DEX → instantiate `CatalogueSource` → wrap via `TachiyomiSourceAdapter` → expose as `SourceRepository`.

## DataStore / Preferences

All settings use `Preferences DataStore` — never `SharedPreferences` for new settings. Preference classes: `GeneralPreferences`, `LibraryPreferences`, `ReaderPreferences`, `DownloadPreferences`, `BackupPreferences`, `ReadingGoalPreferences`, `EncryptedApiKeyStore`, `EncryptedOpdsCredentialStore`. Every class exposes `Flow<T>` for reads and `suspend fun setXxx()` for writes.

Batch DataStore reads with `async/await` to avoid sequential blocking on cold start.

## Reader Engine

Four modes share `ReaderState` but have mode-specific composables:
- **Single Page** — `HorizontalPager` with `beyondBoundsPageCount = 1`
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
