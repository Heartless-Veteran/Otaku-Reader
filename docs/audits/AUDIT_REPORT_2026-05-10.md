# Otaku Reader — Full Codebase Audit Report

**Date:** 2026-05-10
**Auditor:** Aura (Parallel Agent System)
**Agents Deployed:** 11 subagents across 3 layers
**Status:** Layer 1 complete, Layer 2 in progress

---

## Executive Summary

The Otaku Reader codebase is **surprisingly clean** for a project of this size. Only **5 TODOs** across the entire codebase, solid MVI architecture, and well-organized modular structure. However, several **architectural debt items** and **functional bugs** were identified that need attention.

**Key Themes:**
1. **Widget navigation is broken** — tapping home screen widgets opens the app but not the actual manga/chapter
2. **Dual API schism** — modern `MangaSource` and legacy `HttpSource` serve identical purposes but aren't unified
3. **Reader ViewModel is a god object** — 60+ fields, 7+ delegates, needs splitting
4. **Settings is a monolith** — 10 screens share one MVI contract with 50+ fields
5. **6 orphaned use cases** — exist in domain but have no production consumers

---

## 1. App Module

### 🔴 Critical Issues

| File | Issue | Impact |
|------|-------|--------|
| `ContinueReadingWidget.kt:108` | Widget tap launches `MainActivity` with **no manga/chapter extras** | User taps a manga in widget → app opens to Library, not that manga |
| `RecentUpdatesWidget.kt:126` | Same bug — no extras passed | Same impact |
| `NowReadingWidget.kt:90` | `actionStartActivity<MainActivity>()` with no extras | Tapping "Now Reading" doesn't resume reading |
| `DeepLinkHandler.kt:70-133` | **Manifest ↔ parser mismatch** | Manifest declares deep links for MangaSee, MangaFire, Bato.to, MangaPlus — but parser only handles Mangakakalot/Manganato/Manganelo/Webtoons |
| `DeepLinkHandler.kt:103-118` | MangaDex parser only handles `/title/` | Direct `/chapter/` links from MangaDex are not handled |

### 🟡 Medium Issues

| File | Issue |
|------|-------|
| `AndroidManifest.xml:10` | `RECEIVE_BOOT_COMPLETED` declared but unused — no `BootCompletedReceiver` registered |
| `MainActivity.kt:140` | `LocaleListCompat.getEmptyLocaleList()` fallback behavior inconsistent on API < 33 |
| `proguard-rules.pro` | Missing Glance widget keep rules |

### 🟢 Low Issues

| File | Issue |
|------|-------|
| `build.gradle.kts:17-18` | `versionCode = 1`, `versionName = "1.0.0"` — placeholder values |
| `CrashLogExporter.kt:38` | `SimpleDateFormat` (thread-unsafe) — should use `DateTimeFormatter` |
| `proguard-rules.pro:7-9` | Broad `kotlinx.serialization` keep — redundant, ships own rules |

### Dead Code

| File | Finding |
|------|---------|
| `DeepLinkHandler.kt:132-152` | `isSupportedUrl()` — never called |
| `NowReadingWidget.kt:68` | `chapterNumber` field extracted but never rendered |
| `ContinueReadingWidget.kt:91` | Redundant `.take(3)` — already limited at repository |
| `RecentUpdatesWidget.kt:88` | Same redundant `.take(3)` |

---

## 2. Feature Modules

### Architecture Pattern
**MVI** across all features via `UiState`/`UiEvent`/`UiEffect`. `BaseMviViewModel` exists in `:core:common` but most ViewModels manually wire `MutableStateFlow` + `Channel`.

### Test Coverage

| Feature | Tests | Status |
|---------|-------|--------|
| reader | 7 (5 unit + 1 UI + 1 screenshot) | ✅ Well-covered |
| library | 5 (3 unit + 1 UI + 1 screenshot) | ✅ Well-covered |
| browse | 3 (2 unit + 1 androidTest) | ✅ Good |
| details | 2 (1 unit + 1 androidTest) | 🟡 Minimal |
| history | 1 unit | 🟡 Minimal |
| settings | 1 unit | 🟡 Minimal |
| updates | 1 unit | 🟡 Minimal |
| about, feed, migration, more, tracking | 0 | 🔴 None |

### 🔴 Critical Issues

| Feature | Issue |
|---------|-------|
| **reader** | `ReaderViewModel` is a **god object** — 60+ fields, 7+ delegates, manages page loading, history, prefetch, settings, Discord RPC, and downloads. Issue #581 already tracks splitting into domain VMs |
| **settings** | **Monolithic MVI contract** — 10 screens share one `SettingsState`/`SettingsEvent`/`SettingsEffect` with 50+ fields. Every screen observes changes from unrelated sections |
| **library** | `CategoryManagementNavigation.kt` exists in **two paths** (`library/src/...` and `library/category/src/...`) — merge artifact |

### 🟡 Medium Issues

| Feature | Issue |
|---------|-------|
| **about** | No MVI contract at all — pure Composable with callback lambdas |
| **more** | Parent `MoreScreen` is pure UI with no state management |
| **feed, migration, tracking** | Zero test coverage |

### Modules Not Found
- `feature/source` — does not exist (search lives in `feature/browse`)
- `feature/search` — does not exist

### Extra Modules Found
- `feature/onboarding`, `feature/opds`, `feature/statistics` — not in original audit request but exist

---

## 3. Source-API Module

### 🔴 Critical: Dual API Schism

The module hosts **two competing source abstractions** that are not unified:

| Aspect | `MangaSource` (Modern) | `HttpSource` (Legacy) |
|--------|------------------------|------------------------|
| Async model | Suspend/coroutines | RxJava/Observable |
| Models | `SourceManga` (immutable, `@Serializable`) | `SManga` (mutable `var`) |
| Method prefix | `fetchPopularManga`, `fetchLatestUpdates` | `getPopularManga`, `getLatestUpdates` |
| FilterList | `app.otakureader.sourceapi.FilterList` | `eu.kanade.tachiyomi.source.model.FilterList` |

**Impact:** `TachiyomiSourceAdapter` and `TachiyomiModelsAdapter` do heavy lifting to bridge the gap. This creates maintenance overhead and lossy conversions (`contentRating` dropped, `genre`/`genres` name mismatch).

### Recommendations

| Priority | Action |
|----------|--------|
| **P1** | Have `MangaSource : Source` to eliminate field duplication |
| **P1** | Add `@Deprecated` to `HttpSource` with migration path |
| **P2** | Add `contentRating` to `SourceManga` for feature parity |
| **P2** | Standardize method naming (`fetch*` or `get*`) |

---

## 4. Domain Module

### Orphaned Use Cases (No Production Consumers)

From previous audit + current verification:

| Use Case | Status |
|----------|--------|
| `DeleteChapterUseCase` | Only in DI module and tests |
| `GetLibraryUseCase` | Only in DI module and tests — **overlaps with `GetLibraryMangaUseCase`** |
| `GetVisibleCategoriesUseCase` | Only in DI module and tests |
| `SearchLibraryMangaUseCase` | **Only in domain unit tests** — sophisticated query parsing unused by UI |
| `BulkAddToLibraryUseCase` | Only in domain unit tests |
| `BulkRemoveFromLibraryUseCase` | Only in domain unit tests |

**Note:** `GetLibraryUseCase` and `GetLibraryMangaUseCase` serve nearly identical purposes. `LibraryViewModel` uses `GetLibraryMangaUseCase` directly, making `GetLibraryUseCase` redundant.

---

## 5. Data Module

### Dead Code

| Finding | Details |
|---------|---------|
| `UseCaseModule.kt` | Still provides `GetLibraryUseCase` (orphaned) |
| `GetChaptersUseCase` | Was removed from domain but verify DI module cleaned up |

### Status
- Download persistence system newly added (PR #824)
- Tracking APIs well-structured (MyAnimeList, AniList, Kitsu, MangaUpdates, Shikimori)
- `TachiyomiModelsAdapter: 0 refs` — verify if still needed

---

## 6. Core Modules

### Tachiyomi-Compat Usage

| Class | References | Status |
|-------|------------|--------|
| `Page` | 35 refs | ✅ Active |
| `Source` | 25 refs | ✅ Active |
| `Filter` | 20 refs | ✅ Active |
| `FilterList` | 15 refs | ✅ Active |
| `ChildFirstPathClassLoader` | 6 refs | ✅ Active |
| `TachiyomiExtensionLoader` | 4 refs | ✅ Active |
| `SManga` | 3 refs | ✅ Active |
| `SChapter` | 2 refs | ✅ Active |
| `MangasPage` | 2 refs | ✅ Active |
| `SourceFactory` | 2 refs | ✅ Active |
| `CatalogueSource` | 2 refs | ✅ Active |
| `LocalSource` | 2 refs | ✅ Active |
| `SourceHealthMonitor` | 2 refs | ✅ Active |
| `TachiyomiSourceAdapter` | 1 ref | ✅ Active |
| `TachiyomiModelsAdapter` | **0 refs** | ⚠️ Verify if still needed |
| `TachiyomiModule` | **0 refs** | ⚠️ Verify if still needed |
| `ConfigurableSource` | **0 refs** | ⚠️ Verify if still needed |
| `UpdateStrategy` | **0 refs** | ⚠️ Verify if still needed |

---

## 7. TODO/FIXME Scan

**Only 5 TODOs found** — extremely clean codebase.

| File | Line | Type | Description | Priority |
|------|------|------|-------------|----------|
| `docs/features/reader/smart-prefetch.md` | 100 | TODO | `prefetchChapterPages()` page-level prefetch | Medium |
| `docs/security/api-key-security.md` | 172 | TODO | Migrate to Authorization Code + PKCE | **High** |
| `docs/security/api-key-security.md` | 202 | TODO | Tracker PKCE migration — Recommended | **High** |
| `docs/audits/deep-dive-audit.md` | 263 | TODO | Security Review Required — token storage (C-5) | **High** |
| `data/tracking/api/TrackingApis.kt` | 370 | TODO | Migrate to MangaUpdates OAuth 2.0 (security/C-7) | Medium |

---

## 8. Gradle Dependencies

Key versions from `libs.versions.toml`:

| Dependency | Current | Status |
|------------|---------|--------|
| Kotlin | 2.3.21 | ✅ Current |
| AGP | 9.1.1 | ✅ Current |
| Compose BOM | 2026.04.01 | ✅ Current |
| Hilt | 2.59.2 | ✅ Current |
| Room | 2.8.4 | ✅ Current |
| OkHttp | 4.12.0 | ✅ Current |
| Retrofit | 3.0.0 | ✅ Current |
| Coil | 3.4.0 | ✅ Current |

**Note:** Subagent failed to complete full dependency audit. Manual check shows versions are current. Recommend running `gradle dependencyUpdates` task for full report.

---

## 9. Stale Files for Removal

| File | Reason | Action |
|------|--------|--------|
| `domain_audit_report.md` | Old audit report, superseded by this audit | **Remove** |
| `docs/audits/` | Contains 11 audit files — verify which are still current vs. stale | **Review** |
| `docs/EXTENSION_LOADER_CONSOLIDATION.md` | May be completed work | **Verify** |

---

## 10. Recommendations (Prioritized)

### 🔴 P1 — Critical

1. **Fix widget navigation** — Pass `mangaId`/`chapterId` via Intent extras in all three widgets
2. **Sync deep link parsers** — Add parsers for MangaSee, MangaFire, Bato.to, MangaPlus or remove manifest entries
3. **Unify source APIs** — Have `MangaSource : Source`, deprecate `HttpSource`
4. **Split ReaderViewModel** — Address issue #581, extract domain-specific VMs

### 🟡 P2 — Medium

5. **Split Settings MVI contract** — Per-screen contracts instead of monolithic 50+ field state
6. **Remove orphaned use cases** — Delete or wire up: `DeleteChapterUseCase`, `GetLibraryUseCase`, `GetVisibleCategoriesUseCase`, `SearchLibraryMangaUseCase`, `BulkAddToLibraryUseCase`, `BulkRemoveFromLibraryUseCase`
7. **Remove duplicate file** — `CategoryManagementNavigation.kt` in two paths
8. **Add MangaDex `/chapter/` deep link support**
9. **Fix `versionCode`/`versionName` placeholders**

### 🟢 P3 — Low

10. **Add tests for uncovered features** — feed, migration, tracking, about, more
11. **Modernize `CrashLogExporter`** — Replace `SimpleDateFormat` with `DateTimeFormatter`
12. **ProGuard cleanup** — Remove redundant keeps, add Glance rules
13. **Remove or use `RECEIVE_BOOT_COMPLETED`** permission
14. **Clean up stale audit files** — Remove old reports

---

## 11. Stats

| Metric | Value |
|--------|-------|
| Total modules | 27+ (app, data, domain, source-api, 9 core, 14+ feature) |
| Use cases | 36 |
| Repository interfaces | 12 |
| Domain models | 26 |
| Feature screens | ~40 |
| Unit tests | ~30 |
| UI/screenshot tests | ~5 |
| TODOs | 5 |
| FIXMEs/HACKs/XXXs | 0 |

---

*Report compiled by Aura using parallel agent system (8 Layer 1 agents + 3 Layer 2 agents). Layer 2 (README, CLAUDE.md, stale file cleanup) in progress.*
