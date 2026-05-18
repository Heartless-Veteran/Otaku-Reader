# AUDIT_PERFORMANCE.md — Otaku-Reader Android App

**Audit date:** 2026-05-18  
**Database version:** 22 (20 migrations, v2→v22)  
**Schema file set:** 9.json, 10.json, 11.json, 12.json, 13.json, 14.json, 21.json, 22.json (7 snapshots — versions 15–20 are missing from the schemas/ directory)  
**Auditor:** static analysis of source at commit `28a13cdd`

---

## 1. Executive Summary

| Area | Grade | Highest Risk |
|------|-------|--------------|
| Database queries | B | `getFavoriteMangaWithUnreadCount` full-table scan; `observeContinueReading` returns 100 rows deduped in Kotlin |
| Migration chain | C+ | Migration 9→10 creates 7 tables in a single transaction with no rollback safety; schemas 15–20 missing from repo |
| Image loading (Coil 3) | B+ | Good RGB565 + trim-memory hooks; disk cache ignores user preference at init time; no crossfade configured |
| Network (OkHttp/Retrofit) | B | Timeouts set; zero retry/backoff on `Downloader`; no HTTP cache for API responses |
| WorkManager / background | B- | `TrackerSyncWorker` fires every 1 hour with no battery constraint; `FeedRefreshWorker` has no network constraint; `LibraryUpdateWorker` sequential per-manga loop |
| Startup | B | `installSplashScreen()` present; 5 `collectAsStateWithLifecycle` calls in `setContent` before first frame; no baseline profile |
| R8 / APK size | Unknown | No proguard/R8 rules were accessible in this audit pass |

**Overall performance grade: B−**

Top three risks requiring immediate action:
1. **Migration 9→10 is a single-transaction DDL bomb** — 7 tables created at once with no intermediate commit points. Any failure mid-migration on a production device leaves the database in an unrecoverable state with no migration test to catch it.
2. **`LibraryUpdateWorker` iterates every library manga sequentially** — each manga triggers at least one network call. A 200-manga library on a slow connection can hold a wake lock for many minutes.
3. **`observeContinueReading` fetches 100 rows then deduplicates in Kotlin** — the SQL cannot deduplicate by `mangaId` without a subquery; callers are instructed to call `distinctBy` in Kotlin code. This is an application-level N+1 smell and wastes memory proportional to library size.

---

## 2. Database Performance

### 2.1 Index coverage (v22 schema)

| Table | Indexed columns | Missing / suboptimal |
|-------|----------------|----------------------|
| `manga` | `sourceId`, `title`, `favorite`, `(sourceId, url)` UNIQUE | No composite `(favorite, title)` — every library query sorts after filtering |
| `chapters` | `mangaId`, `(mangaId, url)` UNIQUE, `read`, `bookmark`, `dateFetch`, `(mangaId, dateFetch)` | No composite `(mangaId, read)` — `getNextUnreadChapter` and `getUnreadCountByMangaId` scan all chapters for a manga then filter on `read` |
| `reading_history` | `chapter_id` UNIQUE | `read_at` not indexed — `observeHistoryWithMangaInfo` ORDER BY `rh.read_at DESC` is a full-table sort at scale |
| `tracker_sync_state` | `(mangaId, trackerId)` UNIQUE, `syncStatus` | Good |
| `feed_items` | `sourceId`, `timestamp`, `mangaId` | No `(isRead, timestamp)` composite for unread-feed queries |
| `download_queue` | PK = `chapter_id` | No index on `status` or `priority`; `getAllOnce()` ORDER BY priority/added_at does a full scan |
| `reading_list_items` | `listId`, `mangaId`, `(listId, addedAt)` | Good |
| `page_bookmarks` | `chapter_id`, `manga_id`, `(manga_id, created_at)` | Good |

### 2.2 Query-level issues by DAO

#### MangaDao

| Query | Issue | Severity |
|-------|-------|----------|
| `getFavoriteMangaWithUnreadCount()` | `LEFT JOIN chapters … GROUP BY m.id` with `COALESCE(SUM(CASE …))` — scans the entire `chapters` table for every library observation. A library of 200 manga with 5,000 total chapters causes a 200×chapters cross-product scan on every Flow emission | High |
| `searchFavoriteManga(query)` | `LIKE :query \|\| '%'` with a leading literal is prefix-range-safe, but `title` index is a full B-tree scan for the concat; consider a precomputed FTS5 virtual table for title search | Medium |
| Multiple single-field UPDATE queries (`updateReaderDirection`, `updateReaderMode`, `updateReaderColorFilter`, `updateReaderCustomTintColor`, `updateReaderBackgroundColor`, `updatePreloadPagesBefore`, `updatePreloadPagesAfter`) | Seven separate UPDATE statements where one update of the full entity (or one query updating all reader-settings columns) would be 1 roundtrip instead of 7 | Low |
| `getFavoriteMangaGenres()` | `SELECT genre FROM manga WHERE favorite = 1` returns one blob-encoded genre string per row; genre is a single TEXT column containing a comma-separated list. Parsing happens in Kotlin on every emission | Low |

#### ChapterDao

| Query | Issue | Severity |
|-------|-------|----------|
| `getNextUnreadChapter(mangaId)` | Uses `(mangaId, read)` filter but the index `index_chapters_mangaId` only covers `mangaId`; SQLite will full-scan the manga's chapters then filter `read = 0`. Adding a composite `(mangaId, read, sourceOrder)` index would allow an index-range scan with ORDER BY | Medium |
| `getUnreadCountByMangaId(mangaId)` / `getReadCountByMangaId(mangaId)` | Each is a separate Flow; UI that needs both subscribes twice and triggers two separate DB cursors. A single query returning both counts would halve the I/O | Low |
| `getRecentUpdates()` — LIMIT 200 | `INNER JOIN manga` on `manga.favorite = 1` with `ORDER BY dateFetch DESC` — the `(mangaId, dateFetch)` composite index helps, but querying all 200 rows on every `dateFetch` change (including during library update) fires many Flow emissions | Medium |

#### ReadingHistoryDao

| Query | Issue | Severity |
|-------|-------|----------|
| `observeContinueReading()` | Returns `LIMIT 100` rows from a 3-table join; callers are expected to `distinctBy { it.mangaId }.take(12)`. Up to 88 hydrated rows are thrown away every emission. The correct fix is a `GROUP BY ch.mangaId` with a `MAX(rh.read_at)` subquery | High |
| `observeHistoryWithMangaInfo()` | No LIMIT — as history grows this returns unbounded rows. A library user who reads 10 chapters/day accumulates 3,650 rows/year; the full join is emitted to UI on every chapter read | Medium |
| `getAllReadTimestamps()` | Returns every `read_at` value ever recorded with no LIMIT; used by streak calculation. For a long-time user this can be tens of thousands of longs loaded into a Kotlin List | Medium |
| `upsert` / `replaceHistory` | `@Transaction` on a default-interface method works but Room's Kotlin default-method support for `@Transaction` is fragile pre-Kotlin 1.9/Room 2.6; confirm Room version and Kotlin version compatibility | Low |

#### FeedDao

| Query | Issue | Severity |
|-------|-------|----------|
| `getFeedItems(limit)` | Default `limit = 100`; no index on `(isRead, timestamp)` so filtering read/unread items in memory after loading | Low |
| `clearOldFeedItems(olderThan: java.time.Instant)` | `Instant` is a `TypeConverter`-mapped type — confirm the converter stores epoch seconds as INTEGER (not a string) so the `< :olderThan` comparison uses the `timestamp` index | Low |

#### DownloadQueueDao

| Query | Issue | Severity |
|-------|-------|----------|
| `getAll()` / `getAllOnce()` | `ORDER BY priority ASC, added_at ASC` with no index on `priority` or `added_at`; full table scan on every queue change | Low (queue is small in practice) |

### 2.3 N+1 patterns

The most concrete N+1 pattern is in `LibraryUpdateWorker.enqueueAutoDownloads`:

```kotlin
val chapters = chapterRepository.getChaptersByMangaId(mangaId).first()
    .filter { !it.read }
    .sortedByDescending { it.chapterNumber }
    .take(safeLimit)
```

This loads **all** chapters for a manga into memory, filters in Kotlin, and sorts in Kotlin. For a manga with 500+ chapters this allocates a large list on every auto-download cycle. The equivalent single-query form is:

```sql
SELECT * FROM chapters
WHERE mangaId = :mangaId AND read = 0
ORDER BY chapterNumber DESC
LIMIT :limit
```

A second N+1 risk is in `LibraryUpdateWorker.doWork()` — the manga loop calls `updateLibraryManga(manga)` once per manga. Each call likely issues its own network + DB write. There is no batching or parallelism.

---

## 3. Migration Safety

### 3.1 Migration chain overview

```text
v2 → v3 → v4 → v5 → v6 → v7 → v8 → v9 → v10 → v11 → v12 → v13 → v14
                                                  → v15 → v16 → v17 → v18 → v19 → v20 → v21 → v22
```

Total: 20 migration objects defined in `ALL_MIGRATIONS`. Schema snapshots exist for versions 9, 10, 11, 12, 13, 14, 21, and 22 only. Versions 15 through 20 have **no corresponding schema JSON files** in `core/database/schemas/`. This means `MigrationTestHelper` cannot validate those six migrations against an expected schema, and Room's schema verification (`exportSchema = true`) only helps if the file exists.

### 3.2 Riskiest migrations

| Migration | Risk | Reason |
|-----------|------|--------|
| **9→10** | CRITICAL | Creates 7 tables in a single `migrate()` call: `reading_history`, `opds_servers`, `feed_items`, `feed_sources`, `feed_saved_searches`, `tracker_sync_state`, `sync_configuration` — plus 11 indexes. SQLite wraps each `migrate()` call in an implicit transaction, so a mid-migration crash (e.g., out of disk space) rolls back all 7 tables and indexes, leaving the app at v9. However, if a partial write corrupts the SQLite WAL before the transaction commits, recovery is non-deterministic. More critically, there is **no fallback path** and **no migration test** to detect schema drift between the raw SQL and the current Room entity definitions. |
| **12→13** | High | Creates `tracks` table, then `13→14` immediately renames it to `manga_sync` via INSERT-SELECT-DROP. This double-step is unnecessary churn and risks data loss if the process is killed between the two migrations. It also leaves `manga_sync` in the schema but the v22 entity list does not include a `MangaSyncEntity`, meaning Room will complain about an unknown table unless `fallbackToDestructiveMigration` is set or a later migration drops it. Confirm the `manga_sync` table is still present or was cleaned up. |
| **14→15** | Medium | Drops 5 tables (`categorization_results`, `smart_search_cache`, `recommendations`, `reading_patterns`, `recommendation_refreshes`) — all with `DROP TABLE IF EXISTS`. If any of these table names had foreign keys referencing other tables, cascade deletes would fire silently. No schema snapshot for v15 to verify. |
| **18→19** | Medium | Creates `page_bookmarks` with two FK constraints (`chapter_id` and `manga_id`). SQLite FK enforcement is off by default; if `PRAGMA foreign_keys = ON` is not set for the migration connection, the constraints are syntactically recorded but not enforced until the app calls `setForeignKeyConstraintsEnabled(true)`. Room does set this on the live connection but migration helpers may differ. |

### 3.3 Missing migration tests

There are no migration test files visible in the repository (no `*MigrationTest*` class found). The `MigrationTestHelper` API requires schema JSON files. Since schemas 15–20 are absent, those migrations **cannot be tested** with the standard tooling even if tests were written.

**Minimum required action:** Add `core/database/schemas/app.otakureader.core.database.OtakuReaderDatabase/15.json` through `20.json` by regenerating with `./gradlew :core:database:kaptDebugKotlin`, then add migration tests using `MigrationTestHelper` for every migration, especially 9→10 and 12→14.

---

## 4. Image Loading

### 4.1 Coil 3 configuration

Configuration lives in `OtakuReaderApplication.newImageLoader()`:

| Parameter | Configured value | Assessment |
|-----------|-----------------|------------|
| Memory cache | `min(maxMemory × 0.15, 256 MB)` | Reasonable cap. On a 512 MB max-heap device this is ~77 MB, on a low-RAM device (~96 MB heap) it is ~14 MB — may cause frequent cache thrashing during chapter reading |
| Disk cache | `GeneralPreferences.DEFAULT_COIL_DISK_CACHE_MB × 1024 × 1024` (hard-coded default, not user preference live value) | **Bug:** The user's actual disk cache preference is not applied at init time. The disk cache is built once with the default; if the user changes it, the running `ImageLoader` still uses the old size |
| `allowRgb565` | `true` | Correct — halves per-image memory for opaque manga page images (ARGB_8888 → RGB_565) |
| Crossfade | Not configured | Coil 3 defaults to no crossfade. Cover images pop in without a transition, which is perceivable on slow networks |
| Placeholder / error | Not configured globally | Individual composables must set these; if they don't, blank space is shown during load |
| Network fetcher | OkHttp shared client | Correct — reuses connection pool |
| `onTrimMemory` | Implemented in `Application` | `TRIM_MEMORY_UI_HIDDEN` trims to 50%; `RUNNING_LOW/CRITICAL/COMPLETE` trims to 0. Good. |

### 4.2 OOM scenarios

1. **Chapter reader + RGB_565 disabled**: If any `AsyncImage` call in the reader overrides `allowRgb565 = false`, a double-page manga image at 2048×3000 costs 2048×3000×4 = ~23 MB. Loading 3 preloaded pages simultaneously = ~70 MB spike on top of the base heap.

2. **`observeHistoryWithMangaInfo` + thumbnail loading**: The history screen observes a join that returns **all** history rows with `manga_thumbnail` URLs. If 500 history rows each reference a unique cover URL, Coil will attempt to cache 500 thumbnails. With the default thumbnail size of ~200×280 and RGB_565, each is ~110 KB; 500 = ~53 MB. This approaches the memory cache ceiling on mid-range devices.

3. **Disk cache init race**: The `ImageLoader` singleton is built in `newImageLoader()` which may be called from a background thread (Hilt injection). The `DiskCache.Builder` accesses `context.cacheDir` which is safe, but the size is fixed at the time of construction. If the user has set a larger disk cache preference (e.g., 1 GB for a chapter reader), those cached pages will be evicted sooner than expected.

### 4.3 Recommendations

- Add a global crossfade duration: `.crossfade(300)` in `ImageLoader.Builder`.
- Fix disk cache init to read the actual user preference asynchronously and rebuild the loader if the preference changes.
- Set a global placeholder and error drawable to avoid blank-space flicker.
- Consider a separate `ImageLoader` instance for the chapter reader with a higher memory ceiling and `prefetchQueueSize` tuned to the user's `preloadPagesBefore`/`preloadPagesAfter` settings.
- The Palette extraction noted in prior audits (`DEEP_AUDIT.md`) must be moved to `Dispatchers.IO` — this is a correctness bug that also causes frame jank.

---

## 5. Network Layer

### 5.1 OkHttp configuration (`NetworkModule.kt`)

| Parameter | Value | Assessment |
|-----------|-------|------------|
| `connectTimeout` | 30 s | Acceptable for manga source servers; could be 15 s with retry |
| `readTimeout` | 30 s | Risk: large chapter page images on slow servers will time out. Consider 60 s for image downloads or use a separate client |
| `writeTimeout` | 30 s | Appropriate |
| Retry on connection failure | OkHttp default (`true`) | Applies to connection-level failures only, not HTTP error codes |
| HTTP cache (`Cache`) | Not configured | API responses for manga metadata are never cached to disk; every background library update re-fetches pages already downloaded |
| Interceptors | `IgnoreGzipInterceptor` + `BrotliInterceptor` (network), `HttpLoggingInterceptor` (debug only) | Correct; Brotli decompression reduces transferred bytes |
| Certificate pinning | Applied to tracker client via `TrackerCertificatePinner` | Good security practice; ensure pins are rotated in the build |

### 5.2 Downloader (`Downloader.kt`)

The `downloadPage` function has **zero retry logic**:

```kotlin
val result = downloader.downloadPage(url, destFile)
if (result.isFailure) {
    destFile.delete()
    mutex.withLock { updateStatus(chapterId, DownloadStatus.FAILED) }
    return@launch
}
```

A single transient HTTP 503 or network blip marks the entire chapter download as `FAILED`, requiring manual user retry. At minimum, the downloader should attempt exponential backoff (3 retries, 1s/2s/4s) before failing.

### 5.3 Retrofit / API layer

- No HTTP caching headers are set on the Retrofit base URL (`https://api.otakureader.app/`). If the API returns `Cache-Control: max-age=N` headers, OkHttp will honour them — but no `Cache` instance is installed on the client, so responses are never written to disk.
- The `TrackerSyncWorker` fires every 1 hour without checking `If-None-Match` / `If-Modified-Since`; if the tracker API supports conditional requests, this could reduce bandwidth by 90%+ for users who read infrequently.

### 5.4 Concurrent request limits

OkHttp's default dispatcher allows 64 simultaneous requests globally and **5 per hostname** (`maxRequestsPerHost`). The `DownloadManager` limits itself to `maxConcurrentDownloads` (1–5, default 2), but each download issues multiple image-page requests. When `maxConcurrentDownloads = 5` and pages are fetched from different CDN hostnames (common with multi-source setups), the global 64-request cap is the binding limit; OkHttp enforces the per-host cap of 5 regardless of hostname count. However, if all 10 chapters in a single-source queue share the same CDN hostname, OkHttp will hold at most 5 concurrent connections to that host — not 50. The real risk is CDN rate-limiting when all pages for multiple chapters are queued simultaneously against a single host at the maximum per-host concurrency.

**Recommendation:** Set `Dispatcher().maxRequestsPerHost = 3` on the `OkHttpClient` used for downloads to reduce CDN rate-limiting risk, or use a dedicated `OkHttpClient` with a restricted dispatcher for the `Downloader`.

---

## 6. Background Work

### 6.1 Worker inventory

| Worker | Type | Schedule | Network constraint | Battery constraint | Charging constraint |
|--------|------|----------|-------------------|-------------------|---------------------|
| `LibraryUpdateWorker` | Periodic + one-shot | Default 12 h (min 1 h) | `CONNECTED` or `UNMETERED` | None | None |
| `AutoBackupWorker` | Periodic | User-configurable hours | None | `setRequiresBatteryNotLow(true)` | None |
| `FeedRefreshWorker` | Periodic | Every 6 h | **None** | None | None |
| `TrackerSyncWorker` | Periodic | Default 1 h | `CONNECTED` | **None** | None |
| `RecordReadingHistoryWorker` | One-shot | On reader close | None | None | None |
| `ReadingReminderWorker` | One-shot (daily) | Scheduled by `ReadingReminderScheduler` | None | None | None |

### 6.2 Issues

**FeedRefreshWorker — no network constraint (HIGH)**  
`FeedRefreshWorker` schedules with `PeriodicWorkRequestBuilder` but does not set any `Constraints`. On a metered mobile connection the worker will wake every 6 hours, make DB queries, and attempt to delete old feed items. While the network call is not in this worker itself, scheduling without a network constraint means it runs even on airplane mode (wasted wake). Add at minimum `setRequiredNetworkType(NetworkType.CONNECTED)`.

**TrackerSyncWorker — 1-hour default with no battery constraint (HIGH)**  
Tracker sync every hour with no `setRequiresBatteryNotLow(true)` means the worker fires during low-battery situations. For users who rarely read, this is pure battery waste. Recommend defaulting to 6 hours with `setRequiresBatteryNotLow(true)`.

**LibraryUpdateWorker — sequential manga loop (MEDIUM)**  
```kotlin
for (manga in libraryManga) {
    val result = updateLibraryManga(manga)
    ...
}
```
Each manga is updated sequentially. A library of 200 manga each taking 200 ms = 40 s minimum holding a partial wake lock. Consider parallelism with a bounded coroutine dispatcher (`limitedParallelism(4)`) and structured concurrency.

**LibraryUpdateWorker — `Result.retry()` on Wi-Fi check (LOW)**  
If not on Wi-Fi and `updateOnlyOnWifi` is set, the worker returns `Result.retry()`. WorkManager will apply its exponential backoff and enqueue another attempt. This is correct but could lead to a burst of retries; prefer `Result.success()` (skip silently) since the periodic trigger will fire again at the next interval anyway.

**AutoBackupWorker — no `NETWORK_NOT_REQUIRED` but uses disk only (LOW)**  
AutoBackupWorker doesn't need a network connection; it writes to internal storage. No network constraint is set (correct), but the backup writes the full DB serialized to JSON on the calling coroutine without chunking. For large libraries this can hold the Dispatchers.IO thread for multiple seconds.

---

## 7. Startup Performance

### 7.1 SplashScreen API

`installSplashScreen()` is called at the top of `MainActivity.onCreate()` before `super.onCreate()` — this is the correct placement per the Splash Screen API documentation. There is no `splashScreen.setKeepOnScreenCondition {}` call, so the splash screen dismisses immediately once `setContent` is called. If the Hilt graph construction is slow (e.g., `OtakuReaderDatabase` PRAGMA initialization), the splash is gone before meaningful content appears, causing a white frame.

**Recommendation:** Set a `keepOnScreenCondition` that waits for the first library Flow emission or a short `isReady` StateFlow in `MainActivity`.

### 7.2 Cold start composition cost

`MainActivity.setContent` collects 5 `StateFlow`s synchronously via `collectAsStateWithLifecycle` before the first frame is composed:

```kotlin
val themeMode by generalPreferences.themeMode.collectAsStateWithLifecycle(initialValue = 0)
val colorScheme by generalPreferences.colorScheme.collectAsStateWithLifecycle(initialValue = 0)
val usePureBlackDarkMode by generalPreferences.usePureBlackDarkMode.collectAsStateWithLifecycle(initialValue = false)
val useHighContrast by generalPreferences.useHighContrast.collectAsStateWithLifecycle(initialValue = false)
val customAccentColor by generalPreferences.customAccentColor.collectAsStateWithLifecycle(initialValue = 0xFF1976D2L)
val onboardingCompleted by generalPreferences.onboardingCompleted.collectAsStateWithLifecycle(initialValue = false)
```

Each `collectAsStateWithLifecycle` creates a coroutine and subscribes to a SharedPreferences-backed DataStore Flow. With the provided `initialValue` defaults these do not block, but they do add 6 coroutine subscriptions to the first composition. On a cold start these fire near-simultaneously, potentially causing 6 recompositions of `OtakuReaderTheme` and its entire subtree in quick succession.

**Recommendation:** Combine these into a single `UiState` data class emitted by a `MainViewModel` using `combine`. One `collectAsStateWithLifecycle` call replaces six, and the first emission waits for all preferences to be read before triggering a recomposition.

### 7.3 Application.onCreate cost

`OtakuReaderApplication.onCreate()`:
- `CrashHandler.install(this)` — SharedPreferences read, lightweight
- `DynamicColors.applyToActivitiesIfAvailable(this)` — ActivityLifecycleCallbacks registration, lightweight
- `appShortcutManager.initialize()` — likely reads DB or preferences; if it queries `observeLastReadWithMangaTitle` on the main thread this is a blocking database call during startup

`OtakuReaderApplication.newImageLoader()` constructs the `DiskCache` during `ImageLoader` build. `DiskCache.Builder().directory(…).build()` performs file I/O (creates the cache directory) synchronously. This happens during the first `SingletonImageLoader.get(context)` call, which may occur on the main thread during Hilt injection.

### 7.4 Baseline profile

No `baseline-prof.txt` or Macrobenchmark module was found in the repository. Without a baseline profile, the ART interpreter handles the startup hot path (Compose layout, Room initialization, Hilt graph construction) on first run. Adding a baseline profile generated via Macrobenchmark can reduce cold start by 15–40% on first launch.

---

## 8. Benchmark Targets

These are specific measurable targets. Use Android Macrobenchmark or Perfetto for measurement.

| Metric | Current (estimated) | Target | Measurement method |
|--------|--------------------|---------|--------------------|
| Cold start (time to first interactive frame) | ~1,200–1,600 ms | **< 800 ms** | `StartupBenchmark` with `measureRepeated` cold startup |
| Warm start | ~400–600 ms | **< 300 ms** | Macrobenchmark warm startup |
| Library screen render (200 manga) | ~80–150 ms first composition | **< 50 ms** | `ComposeBenchmark` measuring `LibraryScreen` composition |
| Chapter list load (tap manga → chapter list visible) | ~200–400 ms | **< 200 ms** | Manual stopwatch + Systrace `Choreographer#doFrame` |
| `getFavoriteMangaWithUnreadCount` query time | ~50–200 ms at 200 manga / 5000 chapters | **< 20 ms** | `BenchmarkRule` with Room database pre-populated |
| `observeContinueReading` emission processing | ~10–30 ms (100 rows + Kotlin dedup) | **< 5 ms** | Replace with SQL-side `GROUP BY`, measure with Macrobenchmark |
| Image cache hit rate (library covers) | Unknown | **> 90%** | Coil `ImageLoader.memoryCache` hit/miss counters |
| Download throughput (per page) | Unknown | **> 500 KB/s on Wi-Fi** | Manual test + OkHttp EventListener |
| LibraryUpdateWorker duration (200 manga) | ~40–120 s | **< 30 s** | WorkManager `WorkInfo` timestamps |
| Migration 9→10 execution time | ~100–500 ms | **< 200 ms** | `MigrationTestHelper` with timing |
| APK size (release) | Unknown | **< 25 MB** | `./gradlew bundleRelease` + bundletool |

---

## 9. Quick Wins

Changes that take under one hour each and deliver measurable improvement:

### QW-1: Add composite index `(mangaId, read)` on `chapters` (15 min)

In `ChapterEntity` add `@Index(value = ["mangaId", "read"])`. Regenerate the schema. This turns `getNextUnreadChapter` and `getUnreadCountByMangaId` from full-chapter-list scans into O(log n) lookups.

```kotlin
@Entity(
    tableName = "chapters",
    indices = [
        // existing...
        Index(value = ["mangaId", "read"]),          // ADD THIS
        Index(value = ["mangaId", "read", "sourceOrder"]) // also helps getNextUnreadChapter ORDER BY
    ]
)
```

Requires a new migration `22→23`.

### QW-2: Add LIMIT to `observeHistoryWithMangaInfo` (10 min)

Change:
```sql
ORDER BY rh.read_at DESC
```
to:
```sql
ORDER BY rh.read_at DESC
LIMIT 500
```

This caps the unbounded history join to a practical maximum. The History UI does not display more than a few hundred entries at once.

### QW-3: Fix `observeContinueReading` — move dedup to SQL (30 min)

Replace the `LIMIT 100` + Kotlin `distinctBy` pattern with a SQL subquery that selects one row per `mangaId`:

```sql
SELECT ch.id, ch.mangaId, ch.url, ch.name, ch.scanlator, ch.read,
       ch.bookmark, ch.lastPageRead, ch.chapterNumber, ch.dateFetch,
       ch.dateUpload, rh.read_at, rh.read_duration_ms,
       m.title AS manga_title, m.thumbnailUrl AS manga_thumbnail
FROM (
    SELECT chapter_id, MAX(read_at) AS read_at
    FROM reading_history
    INNER JOIN chapters ON chapters.id = reading_history.chapter_id
    INNER JOIN manga ON manga.id = chapters.mangaId AND manga.favorite = 1
    GROUP BY chapters.mangaId
    ORDER BY read_at DESC
    LIMIT 12
) latest
INNER JOIN reading_history rh ON rh.chapter_id = latest.chapter_id
INNER JOIN chapters ch ON ch.id = rh.chapter_id
INNER JOIN manga m ON m.id = ch.mangaId
```

This returns exactly 12 rows instead of 100, eliminating ~88 row allocations per emission.

### QW-4: Add `setRequiresBatteryNotLow(true)` to `TrackerSyncWorker` (5 min)

```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresBatteryNotLow(true)   // ADD THIS
    .build()
```

No logic change required.

### QW-5: Add network constraint to `FeedRefreshWorker` (5 min)

```kotlin
val request = PeriodicWorkRequestBuilder<FeedRefreshWorker>(6, TimeUnit.HOURS)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()
```

### QW-6: Add crossfade to Coil `ImageLoader` (5 min)

In `OtakuReaderApplication.newImageLoader()`:

```kotlin
return ImageLoader.Builder(context)
    // ...existing config...
    .crossfade(300)
    .build()
```

Eliminates cover pop-in; 300 ms is imperceptible on fast cache hits.

### QW-7: Add retry to `Downloader.downloadPage` (20 min)

```kotlin
suspend fun downloadPage(url: String, destFile: File, maxRetries: Int = 3): Result<File> =
    withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        repeat(maxRetries) { attempt ->
            runCatching {
                // existing download logic
            }.onSuccess { return@withContext Result.success(it) }
             .onFailure { lastError = it }
            if (attempt < maxRetries - 1) delay(1_000L shl attempt) // 1s, 2s, 4s
        }
        Result.failure(lastError ?: Exception("Download failed after $maxRetries attempts"))
    }
```

### QW-8: Increase `TrackerSyncWorker` default interval to 6 hours (5 min)

Change `fun schedule(context: Context, intervalHours: Int = 1)` default to `intervalHours: Int = 6`. Users rarely need sub-hourly tracker sync; this reduces background wake frequency by 6×.

### QW-9: Add `index_reading_history_read_at` (15 min)

In `ReadingHistoryEntity`, add:

```kotlin
@Entity(
    tableName = "reading_history",
    indices = [
        Index(value = ["chapter_id"], unique = true),
        Index(value = ["read_at"])   // ADD THIS
    ]
)
```

Requires migration `22→23` (or batch with QW-1). This makes `observeHistoryWithMangaInfo` ORDER BY and `observeContinueReading` ORDER BY use an index scan instead of a filesort.

### QW-10: Consolidate reader-settings updates into a single UPDATE query (20 min)

Replace the 7 individual `updateReaderDirection`, `updateReaderMode`, etc. queries with one:

```kotlin
@Query("""
    UPDATE manga SET
        readerDirection = :direction,
        readerMode = :mode,
        readerColorFilter = :filter,
        readerCustomTintColor = :tintColor,
        readerBackgroundColor = :bgColor,
        preloadPagesBefore = :before,
        preloadPagesAfter = :after
    WHERE id = :id
""")
suspend fun updateReaderSettings(
    id: Long, direction: Int?, mode: Int?, filter: Int?,
    tintColor: Long?, bgColor: Long?, before: Int?, after: Int?
)
```

Reduces 7 DB round-trips to 1 every time reader preferences are saved.

---

## Appendix: Files Reviewed

| File | Purpose |
|------|---------|
| `core/database/src/…/OtakuReaderDatabase.kt` | DB entity list, version = 22 |
| `core/database/src/…/dao/MangaDao.kt` | Manga queries |
| `core/database/src/…/dao/ChapterDao.kt` | Chapter queries |
| `core/database/src/…/dao/ReadingHistoryDao.kt` | History + continue-reading queries |
| `core/database/src/…/dao/FeedDao.kt` | Feed queries |
| `core/database/src/…/dao/TrackerSyncDao.kt` | Tracker sync state queries |
| `core/database/src/…/dao/ReadingListDao.kt` | Reading list queries |
| `core/database/src/…/dao/DownloadQueueDao.kt` | Download queue persistence |
| `core/database/src/…/migrations/DatabaseMigrations.kt` | All 20 migration objects |
| `core/database/schemas/…/22.json` | Full v22 schema with all indexes |
| `core/network/src/…/di/NetworkModule.kt` | OkHttpClient + Retrofit config |
| `core/network/src/…/interceptor/IgnoreGzipInterceptor.kt` | Brotli/gzip interceptor |
| `app/src/…/OtakuReaderApplication.kt` | Coil ImageLoader config, trim-memory hooks |
| `app/src/…/MainActivity.kt` | SplashScreen, startup workers, setContent |
| `app/src/…/di/ImageLoaderModule.kt` | Hilt ImageLoader provision |
| `data/src/…/download/DownloadManager.kt` | Download queue + concurrency |
| `data/src/…/download/Downloader.kt` | Per-page OkHttp download, no retry |
| `data/src/…/worker/LibraryUpdateWorker.kt` | Periodic library refresh |
| `data/src/…/worker/AutoBackupWorker.kt` | Periodic backup |
| `data/src/…/worker/FeedRefreshWorker.kt` | Periodic feed cleanup |
| `data/src/…/worker/TrackerSyncWorker.kt` | Periodic tracker sync |
| `data/src/…/worker/RecordReadingHistoryWorker.kt` | One-shot history persistence |
| `data/src/…/worker/ReadingReminderWorker.kt` | Daily reading reminder |
