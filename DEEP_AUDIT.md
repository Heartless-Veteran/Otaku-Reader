# Otaku Reader — Deep Audit Report
> Date: 2026-05-16
> Auditor: Aura
> Scope: Full codebase — architecture, security, code quality, performance, tests

---

## 📊 Executive Summary

| Category | Grade | Notes |
|----------|-------|-------|
| Architecture | **B+** | Clean modular structure, but feature screens are oversized |
| Security | **C+** | 18 Dependabot CVEs, unencrypted crash logs, unverified extensions |
| Code Quality | **B** | Low TODO count, good DI practices, but detekt rules need tuning |
| Test Coverage | **C+** | 91 test files, but core UI components have 0 coverage |
| Performance | **B** | Proper dispatcher usage, but large composables may lag |
| Database | **B** | 20 migrations, all raw SQL — brittle but functional |

**Top 5 Issues to Address:**
1. 18 dependency vulnerabilities (Dependabot)
2. Unencrypted SharedPreferences for crash reports
3. DetailsScreen.kt at 1,722 lines ( Compose recompositions)
4. 12+ compilation errors in manga/manhwa design system (core/ui)
5. Database migrations use raw `execSQL` with no validation layer

---

## 1. ARCHITECTURE

### ✅ Strengths
- **Modular design**: `app`, `data`, `domain`, `source-api` + feature modules (reader, library, browse, etc.)
- **DI with Hilt**: Proper `@Module`/`@Provides` separation in `core/*/di/` packages
- **MVI pattern**: Contract classes (`ReaderContract`, `DetailsContract`) with sealed `Effect`/`Event`/`State`
- **Clean separation**: Domain models isolated from data layer; repository pattern consistent

### ⚠️ Concerns
| # | Issue | Location | Severity |
|---|-------|----------|----------|
| 1 | **God Screen**: DetailsScreen.kt is 1,722 lines | `feature/details/DetailsScreen.kt` | 🟠 HIGH |
| 2 | **God ViewModel**: DetailsViewModel.kt is 949 lines | `feature/details/DetailsViewModel.kt` | 🟠 HIGH |
| 3 | **God ViewModel**: ReaderViewModel.kt is 862 lines | `feature/reader/ReaderViewModel.kt` | 🟡 MEDIUM |
| 4 | Tachiyomi compat layer is 700+ lines of bridge code | `core/tachiyomi-compat/.../LocalSource.kt` | 🟡 MEDIUM |
| 5 | No explicit module boundaries enforced — circular imports possible | build.gradle.kts files | 🟡 MEDIUM |

**Recommendation**: Break DetailsScreen into sub-composables (CoverSection, ChapterList, ActionBar). Same for ViewModels — extract UseCases.

---

## 2. SECURITY

Already covered in `SECURITY_AUDIT.md`. Key additions from this audit:

### 🔴 Critical
- **18 Dependabot alerts** — 9 high, 8 moderate, 1 low (OkHttp 4.12.0, Kotlin 2.3.21, Hilt 2.59.2, Room 2.8.4)

### 🟠 High
- **Extension APKs loaded via DexClassLoader** with no signature verification — any APK can be sideloaded
- **Crash logs in plain SharedPreferences** (`CrashHandler.kt`) — contains stack traces with file paths

### 🟡 Medium
- **Hardcoded OAuth URLs** in `TrackingViewModel.kt`: MAL, AniList, Shikimori
- **DeepLinkHandler.kt** hardcodes MangaDex and MangaPlus URLs
- **WRITE_EXTERNAL_STORAGE** permission declared — legacy for CBZ export, may not be needed on API 30+

### 🟢 Low
- No biometric lock, no FLAG_SECURE on reader screen
- Clipboard exposes crash reports to other apps

### ✅ Good
- Certificate pinning on 6 tracker endpoints
- Cleartext HTTP blocked (`network_security_config.xml`)
- No Firebase/Google analytics
- ProGuard/R8 + minification enabled

---

## 3. CODE QUALITY

### ✅ Strengths
- **TODO count: 1** — exceptionally clean (`TrackingApis.kt` line 370)
- **No FIXME/XXX/HACK comments** found in production code
- **Dispatcher discipline**: IO for DB/prefs, Default for computation, Main for UI
- **Null safety**: Minimal `!!` usage; safe casts (`as?`) prevalent

### ⚠️ Concerns
| # | Issue | Count | Severity |
|---|-------|-------|----------|
| 1 | `lifecycleScope`/`viewModelScope` usages | 286 | 🟡 MEDIUM |
| 2 | Private declarations (potential dead code) | 1,235 | 🟡 MEDIUM |
| 3 | `@file:Suppress("MaxLineLength")` on 15+ files | 15 | 🟡 MEDIUM |
| 4 | `drawCircle(shadow = ...)` — invalid Compose API | 5 occurrences | 🟠 HIGH |
| 5 | `GenericShape` / `addPath` — unresolved in `InkRevealModifier.kt` | 1 | 🟠 HIGH |

**Note**: Items 4-5 are from the manga/manhwa design system commits and cause CI failures.

---

## 4. TEST COVERAGE

### Statistics
- **91 test files** across all modules
- **Largest test**: `ExtensionsViewModelTest.kt` (1,013 lines)
- **Coverage gate**: ≥60% on `:domain` and `:data` modules

### ⚠️ Gaps
| # | Module | Test Coverage | Severity |
|---|--------|--------------|----------|
| 1 | `core/ui` (components, animations, backgrounds) | **0%** | 🟠 HIGH |
| 2 | `feature/reader` UI layer | Minimal | 🟡 MEDIUM |
| 3 | `feature/browse` extension screens | Partial | 🟡 MEDIUM |
| 4 | Database migrations | No migration tests | 🟡 MEDIUM |
| 5 | Tachiyomi compat layer | 1 test file | 🟡 MEDIUM |

**Recommendation**: Add screenshot/UI tests for `core/ui` components and Room migration tests.

---

## 5. DATABASE

### Schema
- **Current version**: 22 (started at v2)
- **Migrations**: 20 (`MIGRATION_2_3` through `MIGRATION_21_22`)
- **Tables**: manga, chapters, categories, reading_history, opds_servers, feed_items, feed_sources, feed_saved_searches, tracker_sync_state, sync_configuration, reading_streaks, page_bookmarks, reading_lists, reading_list_items, download_queue

### ⚠️ Concerns
| # | Issue | Severity |
|---|-------|----------|
| 1 | **All migrations use raw `execSQL()`** — no type safety, no rollback | 🟠 HIGH |
| 2 | **Migration 9→10 creates 7 tables + 7 indexes in one go** — risky if any step fails | 🟠 HIGH |
| 3 | **No migration tests** — can't verify upgrades work | 🟡 MEDIUM |
| 4 | **Foreign key constraints** present but no `ON DELETE` cascade tested | 🟡 MEDIUM |
| 5 | **String literals in SQL** (`".trimIndent()"`) — hardcoded column names | 🟢 LOW |

**Recommendation**: Add `MigrationTest` using `MigrationTestHelper` for the last 3-5 migrations.

---

## 6. PERFORMANCE

### ✅ Strengths
- **Image loading**: Coil 3.4.0 with proper caching
- **Paging**: AndroidX Paging 3.4.2 for library/lists
- **Background work**: WorkManager 2.11.2 for downloads
- **Dispatcher injection**: `CoroutineDispatchersModule` provides IO/Default/Main

### ⚠️ Concerns
| # | Issue | Location | Severity |
|---|-------|----------|----------|
| 1 | **Large composables** (>1000 lines) recompose on any state change | DetailsScreen, LibraryScreen | 🟠 HIGH |
| 2 | **Palette extraction on main thread** — `extractTheme()` uses `Dispatchers.Default` but `Palette.from()` is synchronous | `CoverThemeExtractor.kt` | 🟡 MEDIUM |
| 3 | **No LazyColumn key specified** in some lists — recomposition inefficiency | Multiple screens | 🟡 MEDIUM |
| 4 | **Canvas animations** without `rememberInfiniteTransition` label | `GradientMeshOrbs.kt` | 🟢 LOW |

---

## 7. DEPENDENCIES

### Version Status
| Library | Current | Latest (approx) | Status |
|---------|---------|-----------------|--------|
| Kotlin | 2.3.21 | 2.3.21 | ⚠️ Check for patch |
| AGP | 9.1.1 | 9.1.1 | ✅ Current |
| Compose BOM | 2026.04.01 | 2026.04.01 | ✅ Current |
| OkHttp | 4.12.0 | 4.12.x | 🔴 Vulnerable |
| Room | 2.8.4 | 2.8.x | ⚠️ Check for patch |
| Hilt | 2.59.2 | 2.59.x | ⚠️ Check for patch |
| Retrofit | 3.0.0 | 3.0.0 | ✅ Current |
| Coil | 3.4.0 | 3.4.0 | ✅ Current |

### Red Flags
- **Netty 4.2.12.Final** — explicitly updated for CVEs, but verify if more patches exist
- **Kotlin Serialization 1.11.0** — check compatibility with Kotlin 2.3.21
- **Security Crypto 1.1.0** — `EncryptedSharedPreferences` has known issues on some devices

---

## 8. CI / BUILD

### Workflow Status
| Workflow | Status | Notes |
|----------|--------|-------|
| Build Debug APK | 🔴 FAIL | Compilation errors in core/ui |
| Unit Tests | 🔴 FAIL | Same compilation errors |
| Detekt | 🔴 FAIL | CyclomaticComplexity + UnusedPrivateProperty |
| Ktlint | ✅ PASS | |
| Security Check | ✅ PASS | BuildConfig scanner |
| Labeler | ✅ PASS | |
| Dependency License Report | ✅ PASS | |

### Root Cause of CI Failures
The `core/ui` module has **12+ files with broken imports/API usage** from the manga/manhwa design system commits:
- `NeonSlider`, `InkSlider`, `InkSwitch`, `GlowSwitch`, `GlowButton`, `InkButton`
- `GlassCard`, `GlitchText`, `ScreentoneBackground`, `ScanlineBackground`
- `GradientMeshOrbs` (missing `@Composable` annotation on private helper)

**These are NOT from the Keiyoushi removal** — they're from separate design system commits.

---

## 9. MANIFEST & PERMISSIONS

```
INTERNET                          ✅ Required
ACCESS_NETWORK_STATE              ✅ Required
POST_NOTIFICATIONS                ✅ Required (Android 13+)
FOREGROUND_SERVICE                ✅ Required (download manager)
FOREGROUND_SERVICE_DATA_SYNC      ✅ Required
REQUEST_INSTALL_PACKAGES          ⚠️ Needed for extensions
WRITE_EXTERNAL_STORAGE          ⚠️ Legacy, check if needed on API 30+
READ_EXTERNAL_STORAGE           ⚠️ Legacy, check if needed on API 30+
BIND_APPWIDGET (×3)              ✅ Widget receivers
```

---

## 🎯 Priority Action List

### P0 — Merge Blockers
1. Fix 12+ compilation errors in `core/ui` (merge PR #845 is currently red)
2. Update 18 Dependabot vulnerabilities

### P1 — Security
3. Encrypt crash reports with `EncryptedSharedPreferences`
4. Verify `EncryptedOpdsCredentialStore` actually uses encryption
5. Remove or justify `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE`

### P2 — Architecture
6. Split `DetailsScreen.kt` (>1000 lines) into sub-composables
7. Extract UseCases from `DetailsViewModel.kt` and `ReaderViewModel.kt`
8. Add Room migration tests for migrations 19-22

### P3 — Quality
9. Add UI tests for `core/ui` components (currently 0% coverage)
10. Add `key` parameters to `LazyColumn`/`LazyRow` items
11. Remove `@file:Suppress("MaxLineLength")` by wrapping lines properly

### P4 — Polish
12. Add `FLAG_SECURE` to reader screen only
13. Clear clipboard after crash report copy
14. Deprecate/remove `ensureDefaultRepository()` completely (currently no-op + deprecated)

---

## 📁 Files Audited

- `app/build.gradle.kts` — signing config, dependencies
- `gradle/libs.versions.toml` — dependency catalog
- `app/src/main/AndroidManifest.xml` — components, permissions
- `core/database/migrations/DatabaseMigrations.kt` — 20 migrations
- `core/extension/.../ExtensionRepoRepositoryImpl.kt` — extension repo logic
- `core/ui/` — all component/animation/theme files
- `feature/*/src/main/java/**/*.kt` — all feature screens and ViewModels
- `data/src/**/*.kt` — repository implementations
- `.github/workflows/*.yml` — CI configuration

**Total**: 88,810 lines of Kotlin across all modules.
