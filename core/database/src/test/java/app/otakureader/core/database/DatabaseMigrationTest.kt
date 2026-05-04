package app.otakureader.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.otakureader.core.database.migrations.ALL_MIGRATIONS
import app.otakureader.core.database.migrations.MIGRATION_13_14
import app.otakureader.core.database.migrations.MIGRATION_14_15
import app.otakureader.core.database.migrations.MIGRATION_15_16
import app.otakureader.core.database.migrations.MIGRATION_16_17
import app.otakureader.core.database.migrations.MIGRATION_17_18
import app.otakureader.core.database.migrations.MIGRATION_18_19
import app.otakureader.core.database.migrations.MIGRATION_19_20
import app.otakureader.core.database.migrations.MIGRATION_20_21
import app.otakureader.core.database.migrations.MIGRATION_9_10
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OtakuReaderDatabase::class.java,
    )

    // ── Chain integrity ──────────────────────────────────────────────────────

    @Test
    fun allMigrations_formsContiguousChain() {
        val sorted = ALL_MIGRATIONS.sortedBy { it.startVersion }
        assertEquals("Migration chain must start at version 2", 2, sorted.first().startVersion)
        assertEquals("Migration chain must end at version 21", 21, sorted.last().endVersion)

        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]
            assertEquals(
                "Gap between migration ${current.startVersion}→${current.endVersion} " +
                    "and ${next.startVersion}→${next.endVersion}",
                current.endVersion,
                next.startVersion,
            )
        }
    }

    @Test
    fun allMigrations_eachVersionIncrementsByOne() {
        for (migration in ALL_MIGRATIONS) {
            assertEquals(
                "Migration ${migration.startVersion}→${migration.endVersion} should increment by 1",
                migration.startVersion + 1,
                migration.endVersion,
            )
        }
    }

    @Test
    fun allMigrations_count() {
        assertEquals("Expected 19 migrations (v2→v21)", 19, ALL_MIGRATIONS.size)
    }

    // ── Migration 9 → 10 ────────────────────────────────────────────────────
    // Adds: feed_items, feed_sources, feed_saved_searches, tracker_sync_state, sync_configuration

    @Test
    fun migration9To10_createsExpectedTables() {
        helper.createDatabase(TEST_DB, 9).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)
        val tables = db.tableNames()
        assertTrue("feed_items must exist after 9→10", "feed_items" in tables)
        assertTrue("feed_sources must exist after 9→10", "feed_sources" in tables)
        assertTrue("feed_saved_searches must exist after 9→10", "feed_saved_searches" in tables)
        assertTrue("tracker_sync_state must exist after 9→10", "tracker_sync_state" in tables)
        assertTrue("sync_configuration must exist after 9→10", "sync_configuration" in tables)
        db.close()
    }

    @Test
    fun migration9To10_feedSavedSearches_hasSourceIdIndex() {
        helper.createDatabase(TEST_DB, 9).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)
        val indexes = db.indexNames("feed_saved_searches")
        assertTrue(
            "index_feed_saved_searches_sourceId must exist after 9→10",
            "index_feed_saved_searches_sourceId" in indexes,
        )
        db.close()
    }

    // ── Migration 9 → 10: new tables added in this fix ──────────────────────

    @Test
    fun migration9To10_createsReadingHistory() {
        helper.createDatabase(TEST_DB, 9).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)
        assertTrue("reading_history must exist after 9→10", "reading_history" in db.tableNames())
        assertTrue(
            "index_reading_history_chapter_id must exist after 9→10",
            "index_reading_history_chapter_id" in db.indexNames("reading_history"),
        )
        db.close()
    }

    @Test
    fun migration9To10_createsOpdsServers() {
        helper.createDatabase(TEST_DB, 9).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)
        assertTrue("opds_servers must exist after 9→10", "opds_servers" in db.tableNames())
        db.close()
    }

    // ── Migration 13 → 14 ───────────────────────────────────────────────────
    // Adds: manga.contentRating (INTEGER NOT NULL DEFAULT 0)

    @Test
    fun migration13To14_addsContentRatingColumn() {
        helper.createDatabase(TEST_DB, 13).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 14, true, MIGRATION_13_14)
        assertTrue(
            "manga.contentRating must exist after migration 13→14",
            "contentRating" in db.columnNames("manga"),
        )
        db.close()
    }

    @Test
    fun migration13To14_contentRatingDefaultsToZero() {
        val db13 = helper.createDatabase(TEST_DB, 13)
        db13.execSQL(
            "INSERT INTO manga (sourceId, url, title, status, favorite, lastUpdate, initialized, " +
                "viewerFlags, chapterFlags, coverLastModified, dateAdded, autoDownload, notifyNewChapters) " +
                "VALUES (1, 'url', 'Test Manga', 0, 0, 0, 0, 0, 0, 0, 0, 0, 1)",
        )
        db13.close()

        val db14 = helper.runMigrationsAndValidate(TEST_DB, 14, true, MIGRATION_13_14)
        val cursor = db14.query("SELECT contentRating FROM manga WHERE title = 'Test Manga'")
        assertTrue("Row must survive migration", cursor.moveToFirst())
        assertEquals("contentRating default must be 0", 0, cursor.getInt(0))
        cursor.close()
        db14.close()
    }

    // ── Migrations 15 → 21 ──────────────────────────────────────────────────
    // These tests start from v14 (the last exported schema before v21) and apply
    // migrations directly via .migrate() to avoid requiring intermediate schema
    // export files (15.json–20.json). Each test verifies the incremental change
    // introduced by its specific migration.

    @Test
    fun migration15To16_createsReadingStreaks() {
        val db = helper.createDatabase(TEST_DB, 14)
        MIGRATION_14_15.migrate(db)
        assertFalse("reading_streaks must NOT exist at v15", "reading_streaks" in db.tableNames())
        MIGRATION_15_16.migrate(db)
        assertTrue("reading_streaks must exist after 15→16", "reading_streaks" in db.tableNames())
        val cols = db.columnNames("reading_streaks")
        assertTrue("date column must exist", "date" in cols)
        assertTrue("chapter_count column must exist", "chapter_count" in cols)
        assertTrue("read_duration_ms column must exist", "read_duration_ms" in cols)
        assertTrue("last_read_at column must exist", "last_read_at" in cols)
        db.close()
    }

    @Test
    fun migration16To17_addsUserCompleted() {
        val db = helper.createDatabase(TEST_DB, 14)
        MIGRATION_14_15.migrate(db)
        MIGRATION_15_16.migrate(db)
        assertFalse("userCompleted must NOT exist at v16", "userCompleted" in db.columnNames("manga"))
        MIGRATION_16_17.migrate(db)
        assertTrue("manga.userCompleted must exist after 16→17", "userCompleted" in db.columnNames("manga"))
        db.close()
    }

    @Test
    fun migration17To18_addsUserDropped() {
        val db = helper.createDatabase(TEST_DB, 14)
        MIGRATION_14_15.migrate(db)
        MIGRATION_15_16.migrate(db)
        MIGRATION_16_17.migrate(db)
        assertFalse("userDropped must NOT exist at v17", "userDropped" in db.columnNames("manga"))
        MIGRATION_17_18.migrate(db)
        assertTrue("manga.userDropped must exist after 17→18", "userDropped" in db.columnNames("manga"))
        db.close()
    }

    @Test
    fun migration18To19_createsPageBookmarks() {
        val db = helper.createDatabase(TEST_DB, 14)
        MIGRATION_14_15.migrate(db)
        MIGRATION_15_16.migrate(db)
        MIGRATION_16_17.migrate(db)
        MIGRATION_17_18.migrate(db)
        assertFalse("page_bookmarks must NOT exist at v18", "page_bookmarks" in db.tableNames())
        MIGRATION_18_19.migrate(db)
        assertTrue("page_bookmarks must exist after 18→19", "page_bookmarks" in db.tableNames())
        val indexes = db.indexNames("page_bookmarks")
        assertTrue("index_page_bookmarks_chapter_id must exist", "index_page_bookmarks_chapter_id" in indexes)
        assertTrue("index_page_bookmarks_manga_id must exist", "index_page_bookmarks_manga_id" in indexes)
        assertTrue(
            "index_page_bookmarks_manga_id_created_at must exist",
            "index_page_bookmarks_manga_id_created_at" in indexes,
        )
        db.close()
    }

    @Test
    fun migration19To20_addsUserNotes() {
        val db = helper.createDatabase(TEST_DB, 14)
        MIGRATION_14_15.migrate(db)
        MIGRATION_15_16.migrate(db)
        MIGRATION_16_17.migrate(db)
        MIGRATION_17_18.migrate(db)
        MIGRATION_18_19.migrate(db)
        assertFalse("userNotes must NOT exist at v19", "userNotes" in db.columnNames("chapters"))
        MIGRATION_19_20.migrate(db)
        assertTrue("chapters.userNotes must exist after 19→20", "userNotes" in db.columnNames("chapters"))
        db.close()
    }

    @Test
    fun migration20To21_createsReadingLists() {
        val db = helper.createDatabase(TEST_DB, 14)
        MIGRATION_14_15.migrate(db)
        MIGRATION_15_16.migrate(db)
        MIGRATION_16_17.migrate(db)
        MIGRATION_17_18.migrate(db)
        MIGRATION_18_19.migrate(db)
        MIGRATION_19_20.migrate(db)
        assertFalse("reading_lists must NOT exist at v20", "reading_lists" in db.tableNames())
        MIGRATION_20_21.migrate(db)
        val tables = db.tableNames()
        assertTrue("reading_lists must exist after 20→21", "reading_lists" in tables)
        assertTrue("reading_list_items must exist after 20→21", "reading_list_items" in tables)
        val indexes = db.indexNames("reading_list_items")
        assertTrue("index_reading_list_items_listId must exist", "index_reading_list_items_listId" in indexes)
        assertTrue("index_reading_list_items_mangaId must exist", "index_reading_list_items_mangaId" in indexes)
        assertTrue(
            "index_reading_list_items_listId_addedAt must exist",
            "index_reading_list_items_listId_addedAt" in indexes,
        )
        db.close()
    }

    // ── Full chain v2 → v21 ─────────────────────────────────────────────────

    @Test
    fun fullMigrationChain_v2ToV21() {
        helper.createDatabase(TEST_DB, 2).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 21, true, *ALL_MIGRATIONS)
        val tables = db.tableNames()
        assertTrue("manga must exist after full chain", "manga" in tables)
        assertTrue("chapters must exist after full chain", "chapters" in tables)
        assertTrue("reading_history must exist after full chain", "reading_history" in tables)
        assertTrue("reading_streaks must exist after full chain", "reading_streaks" in tables)
        assertTrue("reading_lists must exist after full chain", "reading_lists" in tables)
        assertTrue("page_bookmarks must exist after full chain", "page_bookmarks" in tables)
        assertTrue("tracker_sync_state must exist after full chain", "tracker_sync_state" in tables)
        assertTrue("contentRating must exist in manga after full chain", "contentRating" in db.columnNames("manga"))
        assertTrue("userNotes must exist in chapters after full chain", "userNotes" in db.columnNames("chapters"))
        db.close()
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun SupportSQLiteDatabase.tableNames(): Set<String> {
    val names = mutableSetOf<String>()
    query(
        "SELECT name FROM sqlite_master WHERE type='table' " +
            "AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' AND name != 'room_master_table'",
    ).use { cursor ->
        while (cursor.moveToNext()) names.add(cursor.getString(0))
    }
    return names
}

private fun SupportSQLiteDatabase.columnNames(table: String): Set<String> {
    val names = mutableSetOf<String>()
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIdx = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) names.add(cursor.getString(nameIdx))
    }
    return names
}

private fun SupportSQLiteDatabase.indexNames(table: String): Set<String> {
    val names = mutableSetOf<String>()
    query("PRAGMA index_list(`$table`)").use { cursor ->
        val nameIdx = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) names.add(cursor.getString(nameIdx))
    }
    return names
}
