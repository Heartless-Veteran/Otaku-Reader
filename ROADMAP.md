# ROADMAP.md — Otaku-Reader 90-Day Improvement Plan

**Updated:** 2026-05-24 (full 7-phase audit)
**Previous baseline:** Audit commit `28a13cdd6e`
**Priority basis:** AUDIT_MASTER.md impact × effort rankings

## Alpha Launch Criteria (New — from 2026-05-24 audit)

> Alpha is **blocked** until both items below ship. Everything else is beta-track or later.

- [ ] **Notification system** — Implement `NotificationChannelManager` + `NewChapterNotifier` wired to `LibraryUpdateWorker`. Without this, users have no way to know when new chapters are available.  
  _Effort: 3 days_

- [ ] **Tracker auto-sync trigger** — Wire `ReaderViewModel` chapter-completion event → `TrackerSyncUseCase` → all 5 trackers. OAuth login already works; only the trigger is missing.  
  _Effort: 2 days_

**Once these two ship, all other alpha gates are already green (build ✅, security ✅, tests ✅, architecture ✅).**

---

---

## 30-Day Milestone — Stability & P0 Fixes

**Goal:** Zero P0 issues. All critical bugs and resource leaks eliminated. CI green.

### Week 1 — Quick wins (< 4 hours each)

- [ ] **Fix NeonSlider busy-loop** (`core/ui/.../NeonSlider.kt`)  
  Replace `System.currentTimeMillis()` in Canvas with `rememberInfiniteTransition` + `animateFloat`.  
  _Effort: 1 hour_

- [ ] **Fix TextShimmer stacking** (`core/ui/.../ShimmerPlaceholders.kt`)  
  Replace `Box` with `Column` or assign explicit `Modifier.fillMaxWidth().height()` per shimmer line.  
  _Effort: 30 minutes_

- [ ] **Fix CoverColorExtraction ImageLoader leak** (`core/ui/.../CoverColorExtraction.kt`)  
  Replace `ImageLoader(context)` with `context.imageLoader` (singleton from Coil's `SingletonImageLoader`).  
  _Effort: 15 minutes_

- [ ] **Fix ReaderSettingsDelegate 1-line import** (`feature/settings/delegate/ReaderSettingsDelegate.kt`)  
  Change `import app.otakureader.data.repository.ReaderSettingsRepository`  
  to `import app.otakureader.domain.repository.ReaderSettingsRepository`.  
  _Effort: 5 minutes_

- [ ] **Fix MangaHeader dead Animatable** (`feature/details/.../MangaHeader.kt`)  
  Remove `bloomProgress` Animatable construction and `launch` block, or wire it to an actual draw modifier.  
  _Effort: 30 minutes_

### Week 2 — Performance and test coverage

- [ ] **Fix getFavoriteMangaWithUnreadCount query** (`core/database/.../MangaDao.kt`)  
  Rewrite with proper `GROUP BY mangaId` + `COUNT(CASE WHEN read = 0 THEN 1 END)` — eliminate O(n) Kotlin post-processing.  
  Add composite index `(mangaId, read)` on `chapters` table (new migration).  
  _Effort: 2 hours + 1 hour migration_

- [ ] **Fix DatabaseMigrationTest CI gap**  
  Add `DatabaseMigrationRobolectricTest` (Robolectric variant, runs in JVM CI job).  
  Move existing `DatabaseMigrationTest` to `:data:connectedAndroidTest` job.  
  _Effort: 3 hours_

- [ ] **Add TachiyomiBackupImporterTest stubs**  
  Cover happy path (import 100 manga, verify count), partial import (malformed JSON), and round-trip (import then export).  
  Use test fixtures based on existing `BackupRoundTripTest` pattern.  
  _Effort: 4 hours_

### Week 3 — Architecture violations

- [ ] **Remove `implementation(projects.data)` from `feature/details/build.gradle.kts`**  
  Gate all data access through domain use cases. Requires audit of which data classes are imported directly.  
  _Effort: 1–2 days_

- [ ] **Remove `implementation(projects.data)` from `feature/reader/build.gradle.kts`**  
  Same approach. Reader has the deepest direct coupling — audit `ReaderViewModel` for any `data.*` imports.  
  _Effort: 1–2 days_

- [ ] **Add Detekt ForbiddenImport rule for `app.otakureader.data.*` in feature modules**  
  Enforces the architecture boundary going forward. Prevents regression of the above two fixes.  
  ```yaml
  # detekt.yml
  ForbiddenImport:
    active: true
    imports:
      - value: 'app.otakureader.data.*'
        reason: 'Use domain interfaces instead.'
    excludes: ['**/data/**', '**/app/**', '**/test/**']
  ```
  _Effort: 2 hours_

### Week 4 — TachiyomiSourceAdapter blocking fix

- [ ] **Replace `.toBlocking().first()` with `suspendCancellableCoroutine` wrapper** (7 call sites)  
  This is the highest-risk change — touches the Tachiyomi compat layer.  
  Add `Observable<T>.awaitFirst()` extension in `TachiyomiSourceAdapter.kt`.  
  Replace all 7 call sites. Test with: global search (5 sources simultaneously), library update (10 sources).  
  _Effort: 2 hours + manual testing_

**30-Day checkpoint:** All P0 bugs fixed. Detekt boundary enforcement active. DatabaseMigration CI gap closed.

---

## 60-Day Milestone — Architecture Hardening

**Goal:** Clean Architecture fully enforced. God objects decomposed. Coverage gates on all feature modules.

### Weeks 5–6 — Layer violation cleanup

- [ ] **Fix remaining 8 import-level layer violations** (see `AUDIT_ARCHITECTURE.md` § Layer Violations)  
  Priority order:
  1. `feature/tracking/TrackerOAuthViewModel.kt` — introduce `TrackManager` domain interface
  2. `feature/settings/delegate/TrackerSyncSettingsDelegate.kt` — same
  3. `feature/settings/delegate/BackupSettingsDelegate.kt` — 3 data imports → domain use cases
  4. `feature/updates/UpdatesViewModel.kt` — introduce `LibraryUpdateScheduler` domain interface
  5. `feature/settings/SettingsViewModel.kt` — introduce `ReminderScheduler` domain interface
  6. `feature/settings/delegate/LibrarySettingsDelegate.kt` — same scheduler interface
  _Total effort: 3–4 days_

- [ ] **Remove `@Provides` from `UseCaseModule` for `GetHistoryUseCase` and `SearchLibraryMangaUseCase`**  
  Both are accidentally `@Singleton`. Let Hilt inject directly via `@Inject` constructor.  
  _Effort: 30 minutes_

### Weeks 7–8 — God object decomposition

- [ ] **Split `DetailsViewModel.kt` (949 lines) into 3 focused ViewModels**  
  - `MangaDetailViewModel` — manga loading, cover extraction, source suggestions
  - `ChapterListViewModel` — chapter list, filters, sorting, read/unread state
  - `ChapterDownloadViewModel` — download queue, download state, delete
  - Coordinator pattern: `DetailsViewModel` delegates to the three; `DetailsScreen` binds all three via `collectAsStateWithLifecycle`  
  _Effort: 3–5 days_

- [ ] **Extract composable components from `DetailsScreen.kt` (1,722 lines)**  
  - `MangaHeaderSection.kt` — cover, title, author, badges
  - `SourceSuggestionsSection.kt` — related manga carousel
  - `NoteEditorBottomSheet.kt` — per-manga notes
  - `ChapterFilterBottomSheet.kt` — filter/sort controls
  - `PanoramaCoverScreen.kt` — full-screen cover view  
  _Effort: 2–3 days_

- [ ] **Extract composable components from `LibraryScreen.kt` (967 lines)**  
  - `LibraryCategoryTabs.kt`
  - `LibrarySearchBar.kt`
  - `LibraryMultiSelectToolbar.kt`
  - `LibraryMangaGrid.kt`  
  _Effort: 2 days_

### Coverage gates

- [ ] **Add 50% coverage gate for `:feature:reader`, `:feature:tracking`, `:feature:settings`**  
  Add to Kover configuration in each module's `build.gradle.kts`.  
  Current: reader 41%, tracking (OAuth paths) 0%, settings 53%.  
  _Effort: 1 hour configuration + 1–2 days writing tests to reach gate_

- [ ] **Add TrackerOAuthViewModel tests** (AniList PKCE + MAL PKCE + state token validation)  
  _Effort: 4 hours_

**60-Day checkpoint:** All layer violations eliminated. `DetailsViewModel` and `DetailsScreen` split. Coverage gates active on reader and tracking modules.

---

## 90-Day Milestone — Feature Completeness & Polish

**Goal:** Feature score 90+/100. MangaUpdates token persistence fixed. CBZ offline read-back confirmed. Discord backend audited.

### Weeks 9–10 — Unconfirmed feature verification

- [ ] **Verify offline CBZ read-back path in `ReaderViewModel`**  
  Confirm `LocalPageLoader` exists and is wired for chapters with `downloadStatus == Downloaded`.  
  If absent: add `LocalPageLoader` that reads from `CbzCreator`-produced archives.  
  _Effort: S (audit 2 hours) or M (add loader 3–5 days if absent)_

- [ ] **Verify bidirectional tracker auto-push on chapter completion**  
  Confirm `TrackingViewModel.onChapterRead()` calls tracker `update()` automatically.  
  If absent: wire chapter-read event from reader to tracking use case.  
  _Effort: S (audit 1 hour) or M (2 days if wiring absent)_

- [ ] **Audit `core/discord/` for Discord IPC implementation**  
  If IPC backend is a stub/missing: add "coming soon" banner to `SettingsDiscordScreen.kt`.  
  If present: confirm it works end-to-end.  
  _Effort: S (0.5 days)_

### Weeks 11–12 — Security hardening

- [ ] **Fix MangaUpdates session token persistence**  
  Migrate from in-memory `sessionToken` to `TrackerTokenStore` (pattern: `AniListTracker.kt`).  
  _Effort: 0.5 days_

- [ ] **Remove `MAL_CLIENT_SECRET` from BuildConfig**  
  PKCE flow doesn't use client secret. Remove the field and the `local.properties` template entry to reduce attack surface.  
  _Effort: 1 hour_

- [ ] **Add OAuth `state` token validation in callback handler**  
  Confirm all 5 OAuth trackers validate `state` parameter on callback to prevent CSRF. Add if absent.  
  _Effort: 2 hours audit + 2 hours fix if missing_

### Dependency hygiene

- [ ] **Upgrade Kover from 0.8.3 → 0.9.x** (non-blocking, cosmetic)  
  _Effort: 1 hour_

- [ ] **Document `HttpSource` (deprecated) removal timeline**  
  Add KDoc to `HttpSource.kt` with target removal version. Create tracking issue.  
  _Effort: 1 hour_

- [ ] **Make `TachiyomiModelsAdapter` internal**  
  Change `public object TachiyomiModelsAdapter` → `internal object TachiyomiModelsAdapter`.  
  _Effort: 5 minutes_

**90-Day checkpoint:** Feature score 88+/100. Security gaps closed. All deprecation timelines documented. Discord status clarified.

---

## Backlog (Post-90 Days)

| Item | Effort | Notes |
|---|---|---|
| Text-to-Speech read-aloud mode | XL (10–15 days) | Android `TextToSpeech` service, new reader overlay, per-page text extraction |
| Split `ReaderViewModel.kt` (863 lines) — push nav state into `ReaderChapterLoaderDelegate` | M (2 days) | Prerequisite: fix layer violation first |
| Migrate `HttpSource` deprecated extensions off RxJava | XL | Wait until Tachiyomi/Mihon upstream makes the same move |
| Baseline profile re-generation after P0 fixes | S | Startup time expected to improve ~15% with ImageLoader fix |
| Server (`server/`) integration into app build graph | Unknown | Currently completely disconnected |

---

*Roadmap generated at commit `28a13cdd6e`. Timeline assumes solo developer, part-time contribution.*
