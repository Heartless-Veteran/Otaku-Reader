# CLAUDE.md — Otaku Reader

This file is the AI assistant reference for the Otaku Reader codebase. Read it before making any changes.

---

## What This Project Is

Otaku Reader is a production-grade Android manga reader built entirely in Kotlin and Jetpack Compose by a solo developer. It is a clean-architecture alternative to Mihon/Tachiyomi that inherits the Tachiyomi extension ecosystem. The app is in a bug-fixing and stabilization phase (~98% feature-complete).

**The developer is newer to Kotlin. Always explain what was wrong and why a fix works — never drop solutions without context.**

---

## Repository Layout

```
Otaku-Reader/
├── app/                    # Application entry point, Navigation host, Widgets, DI root
├── domain/                 # Pure business logic — UseCases, Repository interfaces
├── data/                   # Repository implementations, WorkManager workers, Backup/Sync
├── source-api/             # Tachiyomi extension API contracts (pure Kotlin/Java interfaces)
├── server/                 # Self-hosted Ktor sync server
├── baselineprofile/        # Baseline profile generation for app startup performance
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
│   ├── discord/            # Discord Rich Presence (native, no external library)
│   ├── ai/                 # Gemini client, ML Kit (full flavor only)
│   └── ai-noop/            # No-op AI stubs for FOSS flavor
└── feature/
    ├── library/            # Main manga collection, categories, filtering, "For You"
    ├── reader/             # Multi-mode reader (single/dual/webtoon/smart panels)
    ├── browse/             # Source browsing and global search (Paging 3)
    ├── details/            # Manga detail page, chapter list, tracker status
    ├── updates/            # New chapter updates list
    ├── history/            # Reading history timeline
    ├── settings/           # All app settings, backup/restore, cloud sync, tracker auth
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
| Kotlin | 2.3.20 |
| AGP | 9.1.1 |
| KSP | 2.3.6 |
| compileSdk | 36 |
| minSdk | 26 |
| targetSdk | 35 |
| JVM target | 17 |
| Compose BOM | 2026.03.01 |

### Product Flavors

| Flavor | Description |
|--------|-------------|
| `full` | Includes Gemini SDK (`core/ai`), all AI features enabled |
| `foss` | Uses `core/ai-noop` stubs, no proprietary SDKs |

Build variants: `fullDebug`, `fullRelease`, `fossDebug`, `fossRelease`. The `foss` flavor is used in CI for open-source compliance.

### Running Builds

```bash
# Assemble debug APK (FOSS flavor — fastest, no proprietary SDKs)
./gradlew :app:assembleFossDebug

# Run all unit tests
./gradlew test

# Run Detekt static analysis
./gradlew detekt

# Build release APKs (requires keystore.properties — see keystore.properties.template)
./gradlew :app:assembleFullRelease :app:assembleFossRelease
```

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
| AI (full only) | Google Generative AI 0.9.0 + ML Kit |
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
- **Do not implement AI features** (Smart Search, Smart Panels recommendations, etc.) — planned for a later phase; stubs exist in `core/ai-noop/`.
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
| `android-ci.yml` | Push/PR to `main`, `develop` | Detekt, unit tests, debug APK (FOSS) |
| `ci.yml` | Push/PR/dispatch to `main`, `develop` | Full + FOSS debug builds, unit tests |
| `release.yml` | Tag push (`v*`) | Full + FOSS release APKs, GitHub release |
| `benchmark.yml` | Manual | Baseline profile generation |
| `build_preview.yml` | PR trigger | Preview APK build |

CI uses JDK 17 for standard builds and JDK 21 for release builds. Gradle caches are managed with `actions/cache@v4`.

---

## Developer Context

- Solo developer, veteran background, newer to Kotlin — explain fixes, don't just drop code.
- Multi-agent workflow: Claude (architecture + debugging), Copilot (day-to-day), Gemini Code Assist, Kimi Claw (bulk GitHub tasks).
- Google Cloud project with Gemini API access exists for future AI features.
- **Current priority: core stability and bug fixing, not new features.**

---

## Audit Workflow

Full-systems audit using Ruflo (`ruvnet/ruflo`) multi-agent orchestration. Produces 10 deliverable files covering architecture, code quality, UI, performance, security, testing, features, master synthesis, roadmap, and patch queue.

### Ruflo Setup (Run Once Per New Session)

```bash
# Initialize Ruflo in repo root (creates .claude/agents/, .mcp.json, .claude-flow/)
npx ruflo@latest init --yes

# Initialize memory database (vector-indexed, persists between sessions)
npx ruflo@latest memory init

# Initialize audit swarm
npx ruflo@latest swarm init --name otaku-reader-audit --topology hierarchical-mesh

# Register Ruflo as MCP server in Claude Code (adds to ~/.claude/claude_code_config.json)
claude mcp add ruflo -- npx ruflo@latest mcp start

# Install audit-specific plugins (run in Claude Code chat after MCP registration)
# /plugin install ruflo-security-audit@ruflo
# /plugin install ruflo-testgen@ruflo
# /plugin install ruflo-docs@ruflo
# /plugin install ruflo-adr@ruflo
# /plugin install ruflo-observability@ruflo
# /plugin install ruflo-intelligence@ruflo
# /plugin install ruflo-rag-memory@ruflo
# /plugin install ruflo-jujutsu@ruflo
```

Ruflo MCP config is in `.mcp.json` (gitignored, generated via `ruflo init`). Memory DB is at `.claude/memory.db` (also gitignored).

### Audit Phases & Commands

Parallelization order: Phase 0 → Phase 1 → Phases 2/3/4/5/6/7 in parallel → Phase 8 last.

#### Phase 0: Bootstrap (already done — Ruflo initialized)

```bash
npx ruflo@latest memory search -q "otaku-reader audit"   # retrieve prior findings
npx ruflo@latest memory stats                             # check DB health
```

#### Phase 1: Architecture & Structural Mapping

**Ruflo (with MCP registered):**
```
/agent spawn architect-scout --role "Android Architecture Auditor" --tools file_read,git_log,dependency_analysis
/swarm task --agent architect-scout --task "Map Otaku-Reader architecture and identify structural anti-patterns"
/memory store --namespace otaku-reader --key architecture-map --value <output>
```

**Native Claude Code fallback:**
```
Agent(subagent_type="Explore", prompt="Read:
  core/navigation/src/main/java/app/otakureader/core/navigation/Route.kt
  domain/src/main/java/app/otakureader/domain/repository/ (all *.kt)
  data/src/main/java/app/otakureader/data/repository/ (all *.kt)
  all module build.gradle.kts files
Map dependency graph, detect circular deps, identify god objects, list orphaned use cases.")
```

Deliverable: `AUDIT_ARCHITECTURE.md`

#### Phase 2: Code Quality & Static Analysis

**Ruflo:**
```
/agent spawn code-grunt --role "Kotlin Code Quality Auditor" --tools lint_runner,complexity_scanner
/agent spawn smell-detector --role "Anti-Pattern Hunter" --tools static_analysis,git_blame
/swarm task --agents code-grunt,smell-detector --parallel --task "Audit Otaku-Reader codebase for quality smells"
```

**Native fallback:**
```
Agent(subagent_type="Explore", prompt="Read:
  feature/library/src/main/java/app/otakureader/feature/library/LibraryViewModel.kt
  feature/details/src/main/java/app/otakureader/feature/details/DetailsViewModel.kt
  feature/reader/src/main/java/app/otakureader/feature/reader/ReaderViewModel.kt
  domain/src/main/java/app/otakureader/domain/usecase/ (all *.kt)
Scan for !! operators, bare try/catch, functions >20 lines, nesting >3, LaunchedEffect misuse.")
```

Deliverable: `AUDIT_CODE_SMELLS.md`

#### Phase 3: UI/UX & Compose Audit

**Ruflo:**
```
/agent spawn ui-viper --role "Jetpack Compose UI Auditor" --tools compose_inspector,layout_analyzer
/swarm task --agent ui-viper --task "Audit Otaku-Reader UI layer for Compose anti-patterns"
```

**Native fallback:**
```
Agent(subagent_type="Explore", prompt="Read:
  feature/library/src/main/java/app/otakureader/feature/library/LibraryScreen.kt
  feature/details/src/main/java/app/otakureader/feature/details/DetailsScreen.kt
  feature/reader/src/main/java/app/otakureader/feature/reader/ReaderScreen.kt
  core/ui/src/main/java/app/otakureader/core/ui/ (theme and component files)
Audit for recomposition anti-patterns, derivedStateOf misuse, remember vs rememberSaveable,
Material3 compliance, accessibility gaps, god composables.")
```

Deliverable: `AUDIT_UI.md`

#### Phase 4: Performance & Memory

**Ruflo:**
```
/agent spawn perf-sniper --role "Android Performance Auditor" --tools profiler,heap_analyzer,apk_analyzer
/swarm task --agent perf-sniper --task "Profile Otaku-Reader for memory leaks, image OOM risk, startup performance"
```

**Native fallback:**
```
Agent(subagent_type="Explore", prompt="Read:
  core/database/src/main/java/app/otakureader/core/database/OtakuReaderDatabase.kt
  core/database/src/main/java/app/otakureader/core/database/dao/ (all DAOs)
  core/database/src/main/java/app/otakureader/core/database/migrations/ (all migrations)
  data/src/main/java/app/otakureader/data/worker/ (WorkManager workers)
  app/src/main/java/app/otakureader/di/ImageLoaderModule.kt
Identify N+1 queries, missing indexes, OOM risk, battery drain, startup cost.")
```

Deliverable: `AUDIT_PERFORMANCE.md`

#### Phase 5: Security Audit

**Ruflo:**
```
/agent spawn sec-watch --role "Android Security Auditor" --tools cve_scanner,secret_scanner,manifest_auditor
/swarm task --agent sec-watch --task "Run full security audit on Otaku-Reader including CVE scan and secret detection"
```

**Native fallback:**
```
Agent(subagent_type="Explore", prompt="Read:
  SECURITY_AUDIT.md (existing baseline)
  data/src/main/java/app/otakureader/data/tracking/ (OAuth token storage)
  core/preferences/src/main/java/app/otakureader/core/preferences/ (encrypted storage)
  app/src/main/AndroidManifest.xml
  app/src/main/res/xml/network_security_config.xml
Identify CVE status, unencrypted storage, OAuth flow gaps, exported component risks.")
```

Deliverable: `AUDIT_SECURITY.md`

#### Phase 6: Testing & Coverage

**Ruflo:**
```
/agent spawn qa-engineer --role "Test Coverage Auditor" --tools coverage_analyzer,test_generator
/swarm task --agent qa-engineer --task "Analyze Otaku-Reader test coverage and generate missing test stubs"
```

**Native fallback:**
```
Agent(subagent_type="Explore", prompt="List all */src/test/java/ directories.
Read 5 representative test files. Check .github/workflows/ci.yml for coverage gates.
Identify untested critical paths (migrations, OAuth, backup import).
Generate test stubs for top 3 P0 untested paths.")
```

Deliverable: `AUDIT_TESTING.md`

#### Phase 7: Feature Gap Analysis

**Ruflo:**
```
/agent spawn product-hunter --role "Product Feature Auditor" --tools feature_matrix,comparator
/swarm task --agent product-hunter --task "Compare Otaku-Reader feature set against standard manga reader expectations"
```

**Native fallback:**
```
Agent(subagent_type="Explore", prompt="Read:
  feature/reader/src/main/java/app/otakureader/feature/reader/ (reader modes and MVI)
  feature/settings/src/main/java/app/otakureader/feature/settings/ (reader settings)
  data/src/main/java/app/otakureader/data/backup/ (backup system)
  feature/tracking/src/main/java/app/otakureader/feature/tracking/ (trackers)
Build feature matrix vs standard manga reader checklist. Rate each: Complete/Partial/Missing.")
```

Deliverable: `AUDIT_FEATURES.md`

#### Phase 8: Master Synthesis

**Ruflo:**
```
/swarm consensus --agents architect-scout,code-grunt,smell-detector,ui-viper,perf-sniper,sec-watch,qa-engineer,product-hunter --output AUDIT_MASTER.md
/swarm goal --name "Otaku-Reader 90-Day Improvement Plan" --output ROADMAP.md
/swarm goal --name "P0 Patch Queue" --output PATCH_QUEUE.md
```

**Native fallback:** Synthesize all 7 phase outputs into AUDIT_MASTER.md (top 10 ranked by impact×effort), ROADMAP.md (30/60/90-day), PATCH_QUEUE.md (ready-to-apply P0 patches).

Deliverables: `AUDIT_MASTER.md`, `ROADMAP.md`, `PATCH_QUEUE.md`

### Ruflo Memory Commands

```bash
# Store a finding
npx ruflo@latest memory store -k "finding-key" --value "description"

# Retrieve prior findings before starting an audit phase
npx ruflo@latest memory search -q "architecture" --limit 5

# View all stored keys
npx ruflo@latest memory stats

# Export full audit bundle for archiving
npx ruflo@latest memory export --namespace otaku-reader --output audit-bundle.json
```

### Ruflo → Native Claude Code Mapping

| Ruflo Command | Native Equivalent |
|---------------|-------------------|
| `/agent spawn X --tools file_read` | `Agent(subagent_type="Explore", prompt="...")` |
| `/swarm task --agents a,b --parallel` | Multiple `Agent(...)` calls in a single message |
| `/memory store --key k --value v` | `npx ruflo@latest memory store -k k --value v` or write to plan file |
| `/swarm consensus --agents a,b --output F` | Synthesize agent outputs in a final `Write(file_path=F)` call |
| `/plugin install X@ruflo` | `Agent` with domain-specific analysis prompt |

### Audit Deliverables Checklist

| File | Phase | Contents |
|------|-------|---------|
| `AUDIT_ARCHITECTURE.md` | 1 | Module graph, god objects, dead code, dependency health |
| `AUDIT_CODE_SMELLS.md` | 2 | P0/P1/P2 issues with file:line citations, patch suggestions |
| `AUDIT_UI.md` | 3 | Compose anti-patterns, recomposition, accessibility |
| `AUDIT_PERFORMANCE.md` | 4 | DB queries, image OOM, startup, benchmark targets |
| `AUDIT_SECURITY.md` | 5 | CVE status, encryption gaps, OAuth analysis |
| `AUDIT_TESTING.md` | 6 | Coverage heatmap, generated test stubs |
| `AUDIT_FEATURES.md` | 7 | Feature gap matrix with effort estimates |
| `AUDIT_MASTER.md` | 8 | Top 10 fixes ranked by impact×effort |
| `ROADMAP.md` | 8 | 30/60/90-day milestones |
| `PATCH_QUEUE.md` | 8 | Ready-to-apply Kotlin patches for all P0 items |
