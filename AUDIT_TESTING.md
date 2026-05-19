# AUDIT_TESTING.md — Otaku-Reader Test Coverage Audit

**Audit date:** 2026-05-18  
**Repo SHA:** 28a13cdd6e9550856e87f4aa4bbdd9fc3b06baa0

---

## 1. Executive Summary

**Coverage Grade: B+** — Strong core, visible UI blind-spots

The test suite is substantially better than the 91-file headline suggests. The database migration tests (`DatabaseMigrationTest.kt`) cover 20 migrations v2→v22 with per-step column/table assertions, directly falsifying the prior audit claim that "migration tests: none exist." Tracker tests (AniList, Kitsu, MAL, MangaUpdates, Shikimori) are thorough and correct. Architecture tests enforce clean-layer separation at source-file level.

**Primary weaknesses:**
1. `core:ui` is completely untested — 0% coverage, no test source set found
2. `ReaderScreen.kt`, all four reader-mode composables, `SmartDownloadTrigger`, panel/prefetch/screenshot subsystems — no Compose UI tests
3. `TachiyomiBackupImporter` is untested — highest single silent-failure surface (malformed `.tachibk` drops library silently)
4. `TrackerOAuthViewModel` has no test — OAuth token exchange/refresh for all five trackers uncovered
5. `SettingsViewModel` has no test — settings persistence delegates completely uncovered
6. Coverage gate applies only to `:domain` and `:data` — `feature:reader`, `feature:tracking`, `feature:settings`, `core:ui` all outside the gate

---

## 2. Coverage Heatmap

| Module | Test Files | Estimated Coverage | Key Gaps |
|---|---|---|---|
| `:domain` (use cases) | 16 use-case tests + ArchitectureTest + TitleNormalizerTest | ~70–75% | Tracker-domain use cases; `SyncTrackingUseCase` |
| `:domain` (models) | `MangaTest.kt`, `ChapterTest.kt` | ~40% | Model validation edge cases |
| `:data` (repository) | 7 repo tests (Category, Chapter, Feed, Manga, PageBookmark, ReadingList, Source, Statistics) | ~65% | `TachiyomiBackupImporter`; `BackupRestorer` merge path |
| `:data` (tracking) | 5 tracker tests (AniList, Kitsu, MAL, MangaUpdates, Shikimori) | ~80% | OAuth token-refresh path |
| `:data` (download) | 3 files (CbzCreator, DownloadManager, DownloadProvider) | ~70% | Download retry/resume on network failure |
| `:data` (worker) | 4 files (GoalCompletionNotifier, LibraryUpdateWorker, UpdateNotifier, UpdateNotifierApiGuard) | ~65% | Worker cancellation mid-run |
| `:data` (backup) | `BackupRoundTripTest.kt` | ~50% | `BackupRestorer` merge logic; `TachiyomiBackupImporter` = **0%** |
| `:data` (OPDS) | `OpdsParserTest.kt` | ~60% | OPDS pagination; authentication challenges |
| `:data` (mapper) | `EntityMappersTest.kt` | ~55% | Network-DTO → domain mappers |
| `:core:database` | `DatabaseMigrationTest.kt` + 5 DAO tests | ~45% (instrumented only) | `TrackEntryDao`, `DownloadQueueDao`, `OpdsServerDao` untested |
| `:feature:reader` | `ReaderViewModelTest.kt` + panel/prefetch subdirs | ~25% estimated | `ReaderScreen.kt`, all reader-mode composables |
| `:feature:tracking` | 0 dedicated feature tests | **0%** | `TrackerOAuthViewModel`, `TrackingScreen` |
| `:feature:settings` | 0 test files | **0%** | `SettingsViewModel`, all settings screens |
| `:core:ui` | 0 test files | **0%** | All shared UI components |

---

## 3. Test Quality Assessment

**What's good:**

- **MockK usage:** All tests use `mockk()` + `coEvery`/`every` properly. `coVerify(exactly = 1)` used to assert side-effect counts. Argument capture via `answers { capturedQuery = firstArg() }` correctly used in tracker tests.
- **Turbine:** `awaitItem()` / `awaitComplete()` usage in `GetCategoriesUseCaseTest` is correct. No bare `collect {}` anti-patterns.
- **Coroutines Test:** `runTest` throughout; no raw `runBlocking` in test bodies.
- **Architecture test:** `ArchitectureTest.kt` scans source files with `File.walkTopDown()` to enforce banned Android imports in domain, verify entity-model separation, and enforce convention plugin usage. High-value static guardrail.
- **Migration tests:** `DatabaseMigrationTest` uses `MigrationTestHelper` correctly with step-by-step `PRAGMA table_info` assertions. Full chain test `fullMigrationChain_v9ToV22` validates end-state.
- **Tracker tests:** `AniListTrackerTest` alone has 38 test cases covering login/logout, re-auth, GraphQL search/find/update, all 6 status bidirectional mappings, and error propagation.

**What's weak:**

- **Migration tests are instrumented-only — silently skipped in CI.** `DatabaseMigrationTest` uses `@RunWith(AndroidJUnit4::class)` and `MigrationTestHelper` which requires an Android runtime. The CI `unit-tests` job runs `testDebugUnitTest` (JVM-only). Migration tests never actually run in CI.
- Some use-case tests (e.g., `DeleteCategoryUseCaseTest` at ~1.1 KB) are extremely thin — likely a single `coVerify` with no flow or error-path assertions.
- No property-based or parameterized tests anywhere in the visible tree.

---

## 4. Critical Untested Paths

### P0 — Will break silently in production

**P0-1: TachiyomiBackupImporter**
`data/src/main/java/app/otakureader/data/backup/tachiyomi/TachiyomiBackupImporter.kt` — zero test coverage. This is the migration path for every user coming from Tachiyomi/Mihon. A format change, missing field, or malformed `.tachibk` file silently drops the user's entire library.

**P0-2: TrackerOAuthViewModel**
`feature/tracking/src/main/java/app/otakureader/feature/tracking/TrackerOAuthViewModel.kt` — no test. OAuth callback URL parsing, token storage, re-authentication entirely uncovered. A broken redirect URI is invisible to CI.

**P0-3: DatabaseMigrationTest silently skipped in CI**
The migration tests are `@RunWith(AndroidJUnit4::class)` instrumented code. The CI `unit-tests` job runs `testDebugUnitTest` (JVM-only). 20 migration scripts ship to production with no CI gate.

**P0-4: BackupRestorer merge logic**
`BackupRestorer.kt` handles restoring into an existing library (merging categories, deduplicating chapters by URL, preserving reading progress). No test covers the merge path — only serialization model is tested in `BackupRoundTripTest`.

### P1 — Likely to break under edge conditions

- `SmartDownloadTrigger` — auto-download logic triggered from reading is completely untested
- `SettingsViewModel` — settings persistence via DataStore delegates has no test
- `LibraryUpdateWorker` cancellation mid-run (existing test covers happy-path only)
- OPDS authentication challenges (401 → credential prompt flow)

---

## 5. Generated Test Stubs

### Stub 1: Room Database Migration Test (Robolectric — runs in CI)

```kotlin
package app.otakureader.core.database

import androidx.room.testing.MigrationTestHelper
import app.otakureader.core.database.migrations.ALL_MIGRATIONS
import app.otakureader.core.database.migrations.MIGRATION_21_22
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TEST_DB = "migration-test-jvm"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseMigrationRobolectricTest {

    @get:Rule
    val helper = MigrationTestHelper(
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        OtakuReaderDatabase::class.java,
    )

    @Test
    fun `migration chain v2 to v22 is contiguous`() {
        val sorted = ALL_MIGRATIONS.sortedBy { it.startVersion }
        assertEquals(2, sorted.first().startVersion)
        assertEquals(22, sorted.last().endVersion)
        for (i in 0 until sorted.size - 1) {
            assertEquals(
                "Gap at ${sorted[i].endVersion}→${sorted[i+1].startVersion}",
                sorted[i].endVersion, sorted[i + 1].startVersion
            )
        }
    }

    @Test
    fun `migration 21 to 22 creates download_queue with required columns`() {
        helper.createDatabase(TEST_DB, 21).use { db ->
            MIGRATION_21_22.migrate(db)
            val cursor = db.query("PRAGMA table_info(download_queue)")
            val cols = buildSet {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndex("name")))
            }
            cursor.close()
            listOf("chapter_id", "manga_id", "manga_title", "chapter_title",
                   "source_name", "page_urls_json", "priority", "status", "added_at")
                .forEach { col -> assertTrue("download_queue.$col must exist", col in cols) }
        }
    }

    @Test
    fun `full chain from v9 produces correct final schema`() {
        helper.createDatabase(TEST_DB, 9).close()
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 22, true,
            *ALL_MIGRATIONS.filter { it.startVersion in 9..21 }.toTypedArray()
        )
        val tables = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table' " +
                 "AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' " +
                 "AND name != 'room_master_table'").use { c ->
            while (c.moveToNext()) tables.add(c.getString(0))
        }
        listOf("manga", "chapters", "reading_history", "reading_streaks",
               "reading_lists", "download_queue", "page_bookmarks",
               "tracker_sync_state", "opds_servers").forEach {
            assertTrue("$it must exist in final schema", it in tables)
        }
        db.close()
    }
}
```

### Stub 2: TrackerOAuthViewModel — OAuth Token Exchange

```kotlin
package app.otakureader.feature.tracking

import app.cash.turbine.test
import app.otakureader.domain.model.TrackerType
import app.otakureader.domain.repository.TrackerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerOAuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var trackerRepository: TrackerRepository
    private lateinit var viewModel: TrackerOAuthViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        trackerRepository = mockk(relaxed = true)
        viewModel = TrackerOAuthViewModel(trackerRepository)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state has no tracker selected and is not loading`() = runTest {
        viewModel.state.test {
            val initial = awaitItem()
            assertFalse(initial.isLoading)
            assertNull(initial.selectedTracker)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handleOAuthCallback with valid code triggers token exchange`() = runTest {
        val url = "otakureader://oauth/anilist?code=valid_auth_code"
        coEvery { trackerRepository.exchangeOAuthCode(TrackerType.ANILIST, "valid_auth_code") } returns Result.success(Unit)

        viewModel.handleOAuthCallback(url, TrackerType.ANILIST)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { trackerRepository.exchangeOAuthCode(TrackerType.ANILIST, "valid_auth_code") }
    }

    @Test
    fun `handleOAuthCallback with missing code emits error state`() = runTest {
        viewModel.state.test {
            awaitItem()
            viewModel.handleOAuthCallback("otakureader://oauth/anilist", TrackerType.ANILIST)
            dispatcher.scheduler.advanceUntilIdle()
            val errorState = awaitItem()
            assertNotNull(errorState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `network failure clears loading and emits error`() = runTest {
        coEvery {
            trackerRepository.exchangeOAuthCode(TrackerType.MAL, "some_code")
        } returns Result.failure(RuntimeException("Network error"))

        viewModel.state.test {
            awaitItem()
            viewModel.handleOAuthCallback("otakureader://oauth/mal?code=some_code", TrackerType.MAL)
            dispatcher.scheduler.advanceUntilIdle()
            val errorState = awaitItem()
            assertFalse("Loading must clear on failure", errorState.isLoading)
            assertNotNull("Error must be set on failure", errorState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Stub 3: TachiyomiBackupImporter — Import Fidelity

```kotlin
package app.otakureader.data.backup.tachiyomi

import app.otakureader.data.backup.model.BackupManga
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TachiyomiBackupImporterTest {

    private lateinit var importer: TachiyomiBackupImporter

    @Before fun setUp() { importer = TachiyomiBackupImporter() }

    private val minimalJson = """
        {"version":2,"backupManga":[{"source":1,"url":"/manga/berserk","title":"Berserk",
        "author":"Kentaro Miura","status":2,"genre":["Action","Drama"],"favorite":true,
        "chapters":[{"url":"/ch/1","name":"Chapter 1","read":true,"lastPageRead":24,"chapterNumber":1.0}]}],
        "backupCategories":[{"name":"Favorites","order":0}]}
    """.trimIndent()

    @Test
    fun `import valid JSON maps all manga fields`() {
        val result = importer.import(minimalJson)
        assertNotNull(result)
        assertEquals(1, result.manga.size)
        val manga: BackupManga = result.manga[0]
        assertEquals("Berserk", manga.title)
        assertEquals("Kentaro Miura", manga.author)
        assertEquals(1L, manga.sourceId)
        assertTrue(manga.favorite)
    }

    @Test
    fun `import maps chapter reading state`() {
        val chapter = importer.import(minimalJson).manga[0].chapters[0]
        assertTrue(chapter.read)
        assertEquals(24, chapter.lastPageRead)
        assertEquals(1.0f, chapter.chapterNumber)
    }

    @Test
    fun `import maps categories`() {
        val result = importer.import(minimalJson)
        assertEquals(1, result.categories.size)
        assertEquals("Favorites", result.categories[0].name)
    }

    @Test
    fun `import with missing optional fields does not throw`() {
        val minimal = """{"version":2,"backupManga":[{"source":1,"url":"/m/1","title":"T","favorite":false}]}"""
        val result = importer.import(minimal)
        assertEquals(1, result.manga.size)
        assertTrue(result.manga[0].chapters.isEmpty())
    }

    @Test(expected = Exception::class)
    fun `completely malformed JSON throws`() { importer.import("not json at all }{") }

    @Test
    fun `empty backup produces empty lists`() {
        val result = importer.import("""{"version":2,"backupManga":[],"backupCategories":[]}""")
        assertTrue(result.manga.isEmpty())
        assertTrue(result.categories.isEmpty())
    }

    @Test
    fun `import 500 manga returns correct count`() {
        val entries = (1..500).joinToString(",") { """{"source":1,"url":"/m/$it","title":"M$it","favorite":false}""" }
        val result = importer.import("""{"version":2,"backupManga":[$entries]}""")
        assertEquals(500, result.manga.size)
        // Performance regression testing belongs in a dedicated benchmark module (baselineprofile/),
        // not in unit tests — wall-clock assertions are flaky on CI runners.
    }
}
```

---

## 6. CI Gate Assessment

**Current gate:** `./gradlew :domain:koverVerify :data:koverVerifyDebug` — 60% line coverage on `:domain` and `:data` only.

**Is 60% sufficient? No — for three reasons:**

1. **The gate excludes the riskiest code.** Database migrations, OAuth flows, `TachiyomiBackupImporter`, all reader ViewModel logic, and all settings persistence live outside the gated modules. A 100% pass on the coverage gate can coexist with completely broken migrations, broken OAuth, and a broken backup importer.

2. **The migration tests that exist don't run in CI.** `DatabaseMigrationTest` is `@RunWith(AndroidJUnit4::class)` instrumented code. `testDebugUnitTest` is JVM-only. These tests never execute in CI.

3. **`continue-on-error: true` on the unit tests step** — the pattern requires a separate failure-detection step. If that step itself has a bug, test failures would not block merges.

**Recommended improvements:**

| Priority | Action | Effort |
|---|---|---|
| P0 | Add Robolectric to `core:database`; move migration tests to JVM-runnable; add `:core:database` to Kover gate at 40% | 1 day |
| P0 | Add `TachiyomiBackupImporter` tests (use stub above) | 2 hours |
| P1 | Add `:feature:reader` to coverage gate at 30% (ViewModel only) | 1 day |
| P1 | Add `TrackerOAuthViewModel` tests (use stub above) | 2 hours |
| P2 | Raise `:domain` and `:data` gate from 60% to 70% | Natural tracking |
| P2 | Add `core:ui` screenshot tests using Roborazzi | 3 days |

*Audit conducted at commit `28a13cdd6e`. Ruflo agent: `qa-engineer` (ruflo-testgen plugin).*
