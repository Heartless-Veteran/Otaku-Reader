# Hilt Dependency Injection Audit

**Status:** Post-Phase 0 cleanup  
**Date:** 2026-04-28  
**Scope:** Core manga-only app (AI/sync/server modules extracted)

---

## Module Graph

```
app/
├── @HiltAndroidApp (OtakuReaderApplication)
├── di/
│   └── ImageLoaderModule.kt          # Coil SingletonImageLoader.Factory binding
├── crash/
│   └── CrashHandler.kt               # Manual install, NOT Hilt-managed
│
core/
├── common/
│   └── di/
│       └── (none — common is pure utilities)
├── database/
│   └── di/
│       └── DatabaseModule.kt         # Room database + DAO providers
├── preferences/
│   └── di/
│       └── PreferencesModule.kt      # DataStore / GeneralPreferences
├── ui/
│   └── (no DI module — theme is pure Compose)
├── extension/
│   └── di/
│       └── ExtensionModule.kt        # ExtensionLoader, ExtensionInstaller
└── navigation/
    └── (no DI module — routes are data objects)

data/
├── di/
│   ├── RepositoryModule.kt           # Binds all Repository interfaces → Impl
│   └── UseCaseModule.kt              # Provides UseCase instances
└── tracking/
    └── di/
        └── TrackingModule.kt         # Tracker APIs, TrackManager

domain/
└── (no DI module — pure interfaces + models)

feature/
├── library/
│   └── LibraryViewModel              # @HiltViewModel, injected use cases
├── browse/
│   └── BrowseViewModel               # @HiltViewModel, injected use cases
├── reader/
│   └── ReaderViewModel               # @HiltViewModel, injected use cases
├── settings/
│   └── SettingsViewModel             # @HiltViewModel, injected preferences
└── (all other features follow same pattern)
```

---

## Verified Bindings (✅ = has implementation)

| Interface | Implementation | Module | Status |
|-----------|---------------|--------|--------|
| `MangaRepository` | `MangaRepositoryImpl` | `RepositoryModule` | ✅ |
| `ChapterRepository` | `ChapterRepositoryImpl` | `RepositoryModule` | ✅ |
| `DownloadRepository` | `DownloadRepositoryImpl` | `RepositoryModule` | ✅ |
| `CategoryRepository` | `CategoryRepositoryImpl` | `RepositoryModule` | ✅ |
| `StatisticsRepository` | `StatisticsRepositoryImpl` | `RepositoryModule` | ✅ |
| `OpdsRepository` | `OpdsRepositoryImpl` | `RepositoryModule` | ✅ |
| `FeedRepository` | `FeedRepositoryImpl` | `RepositoryModule` | ✅ |
| `ReaderSettingsRepository` | `ReaderSettingsRepository` (impl) | `RepositoryModule` | ✅ |
| `PageLoader` | `PageLoaderImpl` | `RepositoryModule` | ✅ |
| `ReadingHistoryScheduler` | `WorkManagerHistoryScheduler` | `RepositoryModule` | ✅ |
| `SourceRepository` | `SourceRepositoryImpl` | `RepositoryModule` | ✅ |
| `TrackerSyncRepository` | **NO IMPL** | — | ❌ ORPHAN |
| `ExtensionManagementRepository` | **NO IMPL** | — | ❌ ORPHAN |

### Orphan Interfaces (no implementation bound)

#### 1. `TrackerSyncRepository`
- **Location:** `domain/repository/TrackerSyncRepository.kt`
- **Status:** Interface exists, no `TrackerSyncRepositoryImpl` in `data/repository/`
- **Decision:** **REMOVE** from `RepositoryModule` binding. Tracker sync is a Phase 3 feature (#731). The `tracking/` package already has `TrackManager` and `Tracker` interfaces — `TrackerSyncRepository` was an earlier abstraction that never got implemented.
- **Action:** Delete `domain/repository/TrackerSyncRepository.kt` and remove its import from `RepositoryModule.kt`

#### 2. `ExtensionManagementRepository`
- **Location:** `domain/repository/ExtensionManagementRepository.kt`
- **Status:** Interface exists, no bound implementation
- **Decision:** **KEEP interface, ADD binding**. Extension management logic is split between `core/extension/` and `data/repository/`. Either:
  - (a) Create `ExtensionManagementRepositoryImpl` in `data/repository/`, OR
  - (b) Have `SourceRepositoryImpl` implement both `SourceRepository` and `ExtensionManagementRepository`
- **Recommendation:** Option (b) — `SourceRepository` already handles source listing; extension management is the same concern.

---

## Known Issues

### Application Class — AI Panel Cache Reference
**File:** `app/src/main/java/app/otakureader/OtakuReaderApplication.kt`

```kotlin
@Inject
lateinit var panelCacheService: Lazy<PanelCacheService>  // ← AI FEATURE, SHOULD BE REMOVED
```

This service is used in `onTrimMemory()` for panel-analysis cache cleanup. Since AI modules are extracted:
- **Action:** Remove `panelCacheService` field and the `cleanupStaleEntries()` call in `onTrimMemory()`
- **Phase:** Phase 0 cleanup should have caught this — needs immediate fix

### FeedRepository — May Be Unused
The `FeedRepository` and `FeedRepositoryImpl` exist and are bound, but the "For You" UI in `feature/library` was never wired to its ViewModel (per audit). If feed features are not in the manga-only MVP:
- **Decision:** Keep the binding — feed is a legitimate browse feature (not AI). Wire it in Phase 2.

---

## Hilt Component Scopes

| Component | Scope | Usage |
|-----------|-------|-------|
| `SingletonComponent` | `@Singleton` | Repositories, use cases, database, preferences, OkHttp |
| `ViewModelComponent` | `@ViewModelScoped` | Not currently used — all VMs get singleton-scoped deps |
| `ActivityComponent` | `@ActivityScoped` | Not currently used |

**Recommendation:** Keep everything `@Singleton` for simplicity. No need for narrower scopes until we have memory pressure evidence.

---

## CI Enforcement

Add to required checks (Phase 4, #723):
- `./gradlew :app:kspDebugKotlin` — compiles the Hilt graph, catches missing bindings at build time
- This is faster than running full tests and catches DI errors immediately

---

## Checklist for Future Audits

- [ ] Every `@Inject` constructor parameter has a binding or `@Provides`
- [ ] No `@Binds` for interfaces with no implementation
- [ ] No `@Provides` for classes that don't exist (post-refactoring risk)
- [ ] No duplicate bindings (two modules binding the same interface)
- [ ] `@HiltAndroidApp` Application class is registered in manifest
- [ ] `kspDebugKotlin` passes with zero errors
