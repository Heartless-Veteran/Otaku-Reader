# Phase 6: Testing & Coverage Audit
**Otaku Reader Android App** | Generated: 2026-05-24

---

## Executive Summary

104 unit test files across 14 modules, 6 instrumentation test files. CI enforces a 60% coverage gate on `:domain` and `:data` via Kover. The foundational test patterns (MockK, Turbine, `runTest`, `StandardTestDispatcher`) are consistently applied. **Three critical paths have zero test coverage:** the full database migration chain, OAuth token flows, and backup round-trip scenarios.

**Coverage Score: 7 / 10**

---

## Coverage Heatmap

| Module | Test Files | Status | Key Gaps |
|--------|-----------|--------|----------|
| `core/database` | 6 | Partial | Migration chain integration (25 versions), cascade delete validation |
| `core/extension` | 5 | Good | Version downgrade handling, permission manifest edge cases |
| `core/tachiyomi-compat` | 6 | Good | ClassLoader limits (instrumentation needed), `ConfigurableSource` edge cases |
| `core/ui` | 1 | Minimal | Compose component state, animation |
| `data` | 15 | Partial (gate: 60%) | OAuth tracker flow (0 tests), repository abstract method coverage |
| `domain` | 25 | Partial (gate: 60%) | Interactor composition chains, error propagation |
| `feature/browse` | 2 | Minimal | Search pagination, source switching |
| `feature/details` | 1 | Minimal | Cover caching, metadata refresh |
| `feature/library` | 4 | Good | Sort/filter state, selection management (27 test cases) |
| `feature/reader` | 6 | Good | Reader mode behavior, bookmark/progress state |
| `feature/tracking` | 1 | Minimal | Service integration, token refresh |
| `feature/migration` | 2 | Minimal | End-to-end migration workflow |
| `feature/more` | 2 | Minimal | Settings persistence |
| `baselineprofile` | 0 | None | No tests at all |

**Total:** 104 unit test files | 6 instrumentation files | 1 zero-coverage module

---

## P0 Gaps — Untested Critical Paths

### 1. Database Migration Chain (v1 → v25)

No test verifies consecutive migration across all 25 schema versions. Silent data corruption could occur during app updates; users may be unable to restore old backups.

**Stub:**
```kotlin
// core/database/src/test/.../DatabaseMigrationChainTest.kt
@Test
fun migrationChain_v1_through_v25_maintainsForeignKeyIntegrity() {
    // 1. Create v1 schema with test data
    // 2. Apply each migration sequentially v1→v25
    // 3. Verify foreign key constraints, row counts, and schema version
}
```

### 2. OAuth Token Flow — All 5 Trackers

No test covers OAuth callback handling, code_verifier validation, or token storage for any tracker (MAL, AniList, Kitsu, MangaUpdates, Shikimori).

**Stub:**
```kotlin
// data/src/test/.../AniListOAuthFlowTest.kt
@Test
fun oauthCallback_withValidCode_storesEncryptedToken() { ... }

@Test
fun oauthCallback_withStateMismatch_throwsSecurityException() { ... }

@Test
fun oauthCallback_withExpiredToken_triggersRefresh() { ... }
```

### 3. Backup Round-Trip (Create → Export → Import)

No test verifies that a backup created from live data can be fully restored. Schema evolution or serialization changes could silently corrupt backups.

**Stub:**
```kotlin
// data/src/test/.../BackupRoundTripTest.kt
@Test
fun backup_createAndRestore_preservesAllMangaWithChaptersAndCategories() {
    // 1. Insert test data (3 manga, 10 chapters, 2 categories, 1 tracker entry)
    // 2. Create backup via BackupCreator
    // 3. Clear database
    // 4. Restore via BackupRestorer
    // 5. Assert all entities match original
}
```

---

## P2 Gaps

- Repository layer polymorphism: no tests for error-path behavior in `data/` repository implementations
- `ExtensionLoader` under `TIRAMISU` API paths: partially covered (see `TachiyomiExtensionLoaderTest`); version downgrade not tested
- Coverage gate missing for `feature/*` modules — only `:domain` and `:data` are gated at 60%

---

## Test Quality Spot-Check

Reviewed: `CategoryManagementViewModelTest.kt`, `LibraryViewModelTest.kt`, `TachiyomiExtensionLoaderTest.kt`

| Check | Result |
|-------|--------|
| `runTest { }` for coroutines | ✓ All three files |
| Turbine `.test { }` for Flow | ✓ Used correctly with `awaitItem()` |
| MockK dependency isolation | ✓ `mockk()`, `every { }`, `coEvery { }`, `coVerify` |
| `StandardTestDispatcher` + `advanceUntilIdle()` | ✓ Correct scheduler control |
| Success and failure paths | ✓ Both covered in ViewModelTests |
| API 33 overload stubs (`PackageInfoFlags`) | ✓ Fixed in `TachiyomiExtensionLoaderTest` |

---

## CI Coverage Gate

`.github/workflows/ci.yml` enforces Kover 60% coverage on `:domain` and `:data`. Feature modules have no gate.

**Recommendation:** Raise `:data` gate to 70% and add a 50% gate on `:feature:library` and `:feature:reader` (highest business value).

---

## Score: 7 / 10

Strong test infrastructure and good patterns. Three critical untested paths (migrations, OAuth, backup) should be addressed before beta. Adding gates to feature modules would prevent coverage regression as the codebase grows.
