# CLAUDE.md — Otaku Reader

This file is the AI assistant reference for the Otaku Reader codebase. Read it before making any changes.

---

## What This Project Is

Otaku Reader is a production-grade Android manga reader built entirely in Kotlin and Jetpack Compose by a solo developer. It is a clean-architecture alternative to Mihon/Tachiyomi that inherits the Tachiyomi extension ecosystem. The app is feature-complete for beta: all 35 parity issues plus the hardening batch have shipped, and the current phase is **beta APK validation**.

**Status:** Alpha shipped 2026-05-25. Beta feature parity (#926–#958) and the 2026-06 hardening batch (#1090–#1099) are merged. Project website: https://heartless-veteran.github.io/Otaku-Reader/

**The developer is newer to Kotlin. Always explain what was wrong and why a fix works — never drop solutions without context.**

---

## Repository Layout

```
Otaku-Reader/
├── app/                    # Application entry point, Navigation host, Widgets, DI root
├── domain/                 # Pure business logic — UseCases, Repository interfaces
├── data/                   # Repository implementations, WorkManager workers, Backup/Sync
├── source-api/             # Tachiyomi extension API contracts (pure Kotlin/Java interfaces)
├── server/                 # Self-hosted Ktor sync server (optional, separate repo)
├── baselineprofile/        # Baseline profile generation for app startup performance
├── website/                # VitePress project site, deployed to GitHub Pages by pages.yml
├── build-logic/            # Gradle convention plugins
├── core/
│   ├── common/             # Shared utilities, Palette API, coroutine helpers
│   ├── database/           # Room entities, DAOs, migrations
│   ├── network/            # OkHttp + Retrofit + Kotlinx Serialization setup
│   ├── preferences/        # DataStore preferences, encrypted credential storage
│   ├── ui/                 # Shared Compose components, Material 3 theme, Coil integration
│   ├── navigation/         # Type-safe Compose Navigation routing
│   ├── extension/          # Dynamic APK classloading for Tachiyomi extensions
│   ├── tachiyomi-compat/   # RxJava 1.x stubs and Tachiyomi interface bridges
│   └── discord/            # Discord Rich Presence (native, no external library)
└── feature/
    ├── library/            # Main manga collection, categories, filtering
    ├── reader/             # Multi-mode reader (single/dual/webtoon/smart panels)
    ├── browse/             # Source browsing and global search (Paging 3)
    ├── details/            # Manga detail page, chapter list, tracker status
    ├── updates/            # New chapter updates list
    ├── history/            # Reading history timeline
    ├── settings/           # All app settings, backup/restore, tracker auth
    ├── statistics/         # Reading stats dashboard
    ├── migration/          # Source migration wizard
    ├── tracking/           # Tracker integrations (MAL, AniList, Kitsu, MangaUpdates, Shikimori)
    ├── onboarding/         # First-run setup wizard
    ├── about/              # About screen, credits, version info
    ├── opds/               # OPDS catalog support (Komga/Kavita)
    ├── feed/               # Recommendations and activity feed
    └── more/               # Bottom nav "More" section
```

---

## Architecture

### Layer Overview

```
UI (Jetpack Compose)
  └─ collectAsStateWithLifecycle()
       └─ ViewModel (@HiltViewModel)
            └─ StateFlow<UiState> + SharedFlow<Effect>
                 └─ UseCases (Domain layer)
                      └─ Repository interfaces (Domain layer)
                           └─ Repository implementations (Data layer)
                                └─ Room DAOs + Retrofit + DataStore
```

### MVI Pattern — Non-Negotiable

Every screen follows Model-View-Intent:

- **State**: Immutable data class, exposed as `StateFlow<UiState>` from the ViewModel.
- **Intent**: Sealed class representing user actions. All UI changes go through an Intent → Reducer cycle.
- **Effect**: One-shot events (navigation, toasts) via a separate `SharedFlow<Effect>` or `Channel`.

Rules:
- Never mutate state directly.
- Never use `LiveData` — only `StateFlow` and `SharedFlow`.
- Composables must be stateless where possible; hoist state up.
- Collect state with `collectAsStateWithLifecycle()`, not `collectAsState()`.
- Use `LaunchedEffect` only for one-time side effects on composition.
- Use `rememberCoroutineScope()` only for user-triggered async actions.

**Pattern example** (every feature looks like this):
```
LibraryMvi.kt      — sealed class LibraryState, LibraryIntent, LibraryEffect
LibraryViewModel.kt — @HiltViewModel, produces StateFlow<LibraryState>
LibraryScreen.kt   — stateless composable consuming state
```

### Clean Architecture Layer Rules

| Layer | Contains | Rules |
|-------|----------|-------|
| `domain/` | UseCases, Repository interfaces, domain models | Pure Kotlin, no Android imports, no DI annotations beyond `@Inject` |
| `data/` | Repository implementations, DAOs (via core/database), Workers | Implements domain interfaces; entities ≠ domain models — always map |
| `feature/*` | ViewModel, Composables, MVI state | Depends on domain, never on data directly |
| `core/*` | Shared infrastructure | No feature-level dependencies |

---

## Dependency Injection (Hilt)

- All ViewModels: `@HiltViewModel`
- All Repositories: `@Singleton` (unless there is a specific reason otherwise)
- UseCases: `@Reusable` or unscoped
- Always verify `@InstallIn` scope matches the injection site
- KSP runs the Hilt processor — if a binding appears missing, check `@InstallIn` before blaming the ViewModel

---

## Database (Room)

- **Every DAO read function returns `Flow<T>`** — never a plain value.
- Migrations must be explicit. **Never use `fallbackToDestructiveMigration()` in production.**
- Entities are separate from domain models. Always write and use mapper functions.
- For tests, use in-memory Room databases — no `MigrationTestHelper`.

---

## Extension System — Non-Negotiable

**Tachiyomi extension compatibility must never be broken.**

Extensions load dynamically as APKs with classloader isolation. The interfaces in `source-api/` and `core/tachiyomi-compat/` must match the Tachiyomi/Mihon extension API exactly — this gives Otaku Reader access to 500+ community-maintained sources without any changes to those extensions.

- Do not modify interface signatures in `source-api/`.
- Do not change or remove the Tachiyomi RxJava 1.x stubs in `core/tachiyomi-compat/`.
- Reference the Komikku fork in the same GitHub org when uncertain about the extension API.

---

## Build System

### Convention Plugins (in `build-logic/`)

| Plugin ID | Applies to |
|-----------|-----------|
| `otakureader.android.application` | `app/` |
| `otakureader.android.library` | Most `core/*` and `data/`, `domain/` |
| `otakureader.android.feature` | All `feature/*` modules (auto-adds `core:ui`, `core:navigation`, `domain`) |
| `otakureader.android.hilt` | Any module needing DI |
| `otakureader.android.room` | Any module with Room entities/DAOs |
| `otakureader.android.library.compose` | Modules with Compose components |
| `otakureader.kotlin.library` | Pure JVM modules (server, domain utilities) |

### Key SDK & Kotlin Versions

| Setting | Value |
|---------|-------|
| Kotlin | 2.3.21 |
| AGP | 9.1.1 |
| KSP | 2.3.7 |
| compileSdk | 36 |
| minSdk | 26 |
| targetSdk | 36 |
| JVM target | 17 |
| Compose BOM | 2026.04.01 |

### Product Flavors

The build is now a **single flat artifact** — no product flavors. Use `./gradlew assembleDebug` or `./gradlew assembleRelease` directly.

| Build Command | Output |
|---------------|--------|
| `./gradlew :app:assembleDebug` | Debug APK (fastest, development) |
| `./gradlew :app:assembleRelease` | Signed release APK (requires keystore) |

**Note:** The `full`/`foss` flavor dimension was removed. AI features are planned for a future phase but are not in the core repo. See [Otaku-Reader-AI](https://github.com/Heartless-Veteran/Otaku-Reader-AI) (planned separate repo).

---

## Key Libraries

| Purpose | Library |
|---------|---------|
| UI | Jetpack Compose + Material 3 |
| Async | Kotlin Coroutines 1.10.2 + Flow |
| DI | Hilt 2.59.2 (KSP-processed) |
| Database | Room 2.8.4 |
| Preferences | DataStore 1.2.1 |
| Encryption | AndroidX Security Crypto 1.1.0 |
| HTTP | OkHttp 4.12.0 + Retrofit 3.0.0 |
| Serialization | Kotlinx Serialization 1.11.0 |
| Image loading | Coil 3 (3.4.0) |
| Paging | Paging 3 (3.4.2) |
| Background work | WorkManager 2.11.2 |
| Widgets | Glance 1.1.1 |
| Widgets | Glance 1.1.1 |
| Self-hosted server | Ktor 3.4.2 |
| Static analysis | Detekt 1.23.8 |

---

## Testing

### Frameworks

| Tool | Role |
|------|------|
| JUnit 4 (primary) / JUnit 5 | Test runner |
| MockK 1.14.9 | Kotlin mocking DSL |
| Turbine 1.2.1 | Flow assertion (`.test { awaitItem() }`) |
| Robolectric 4.16.1 | Android environment simulation for unit tests |
| `androidx-test` | AndroidX testing utilities |

### Patterns

```kotlin
@Test
fun `library state updates when intent received`() = runTest {
    val viewModel = LibraryViewModel(mockUseCase, testDispatcher)
    viewModel.uiState.test {
        viewModel.onIntent(LibraryIntent.LoadLibrary)
        val state = awaitItem()
        assertThat(state.manga).isNotEmpty()
    }
}
```

- Use `runTest { }` for all suspend functions.
- Use Turbine's `.test { }` for all Flow assertions.
- Mock all external dependencies with MockK (`mockk { }` or `every { }`).
- Use in-memory Room databases for DAO tests.
- Modules that need Android resources set `unitTests.isIncludeAndroidResources = true`.

---

## Common Bug Areas — Check These First

1. **Hilt binding errors** — Missing `@Provides`, wrong scope, missing `@InstallIn`. Check the DI module before assuming the ViewModel is wrong.
2. **Room DAO not connected** — DAO not injected into repo, repo not injected into UseCase. Trace the chain.
3. **MVI state not updating UI** — `StateFlow` not collected in Compose, or reducer emitting same reference. Use `copy()`.
4. **Extension loader failures** — ClassLoader issue, missing permission, or interface mismatch with Tachiyomi API.
5. **Gradle dependency conflicts** — Version mismatches between Compose BOM, Kotlin, Hilt, or KSP. Check `libs.versions.toml` first.
6. **Navigation crashes** — Missing destination, wrong argument type in NavGraph, or missing `@Serializable` on route class.
7. **Coroutine scope leaks** — `GlobalScope` used instead of `viewModelScope` or `lifecycleScope`. Always use structured concurrency.

---

## Code Style Conventions

- Prefer extension functions over utility classes.
- Use `sealed class` for UI state, intent, and effect modeling.
- Keep ViewModels thin — business logic belongs in UseCases.
- No hardcoded strings — use `strings.xml` resources.
- No magic numbers — use named constants.
- No XML layouts — this is a pure Jetpack Compose project.
- No `GlobalScope`.
- No `LiveData`.

---

## What NOT To Do

- **Do not break Tachiyomi extension compatibility** — this is the most critical constraint.
- **Do not implement AI features in core** — AI features are planned for a separate repo, not the core app.
- **Do not add Firebase analytics or crash tooling** unless explicitly requested.
- **Do not use `fallbackToDestructiveMigration()`** in Room database setup.
- **Do not use `GlobalScope`** — use `viewModelScope`, `lifecycleScope`, or a provided `CoroutineScope`.
- **Do not use `LiveData`** — StateFlow only.
- **Do not write XML layouts** — Compose only.
- **Do not mutate ViewModel state directly** — all changes through Intent → Reducer.

---

## CI/CD

| Workflow | Trigger | What It Does |
|----------|---------|--------------|
| `android-ci.yml` | Push/PR to `main`, `develop` | Detekt, unit tests, debug APK |
| `ci.yml` | Push/PR/dispatch to `main`, `develop` | Debug builds, unit tests |
| `release.yml` | Tag push (`v*`) | Signed release APK, GitHub release |
| `benchmark.yml` | Manual | Baseline profile generation |
| `build_preview.yml` | PR trigger | Preview APK build |

CI uses JDK 17 for standard builds and JDK 21 for release builds. Gradle caches are managed with `actions/cache@v4`.

---

## Developer Context

- Solo developer, veteran background, newer to Kotlin — explain fixes, don't just drop code.
- Multi-agent workflow: Claude (architecture + debugging), Copilot (day-to-day), Gemini Code Assist, Kimi Claw (bulk GitHub tasks).
- **Current priority: beta APK validation and release.** Feature parity and hardening are merged.

---

## Audit Workflow (Archived)

The full-systems audit from 2026-05-24 has been completed and its artifacts archived in `.github/audit-archive/`. The audit validated alpha readiness (all gates green) and informed the beta feature parity backlog.

**Current workflow:** Beta features are tracked as individual GitHub issues (#926–#958) rather than monolithic audit documents. See [ROADMAP.md](ROADMAP.md) for the full beta plan.

**Legacy audit files (reference only):**
- `.github/audit-archive/AUDIT_MASTER.md`
- `.github/audit-archive/AUDIT_ARCHITECTURE.md`
- `.github/audit-archive/AUDIT_CODE_SMELLS.md`
- `.github/audit-archive/AUDIT_FEATURES.md`
- `.github/audit-archive/AUDIT_PERFORMANCE.md`
- `.github/audit-archive/AUDIT_SECURITY.md`
- `.github/audit-archive/AUDIT_TESTING.md`
- `.github/audit-archive/AUDIT_UI.md`
- `.github/audit-archive/PATCH_QUEUE.md`

---

*CLAUDE.md maintained by the core team. For beta feature questions, see [ROADMAP.md](ROADMAP.md) and GitHub issues #926–#958.*
