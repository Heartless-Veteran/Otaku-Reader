package app.otakureader.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE manga ADD COLUMN initialized INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE manga ADD COLUMN viewer INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE manga ADD COLUMN chapter_flags INTEGER DEFAULT NULL")
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE manga ADD COLUMN categories TEXT DEFAULT NULL")
    }
}

internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)")
    }
}

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS manga_categories (_id INTEGER PRIMARY KEY AUTOINCREMENT, manga_id INTEGER NOT NULL, category_id INTEGER NOT NULL, FOREIGN KEY (category_id) REFERENCES categories (_id) ON DELETE CASCADE)")
    }
}

internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE manga ADD COLUMN date_added INTEGER DEFAULT 0")
    }
}

internal val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE manga ADD COLUMN update_strategy INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE manga ADD COLUMN calculate_interval INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chapters ADD COLUMN version INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE chapters ADD COLUMN is_syncing INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE manga ADD COLUMN last_modified_at INTEGER DEFAULT 0")
        db.execSQL("ALTER TABLE chapters ADD COLUMN last_modified_at INTEGER DEFAULT 0")
    }
}

internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE manga ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS tracks (_id INTEGER PRIMARY KEY AUTOINCREMENT, manga_id INTEGER NOT NULL, sync_id INTEGER NOT NULL, remote_id INTEGER NOT NULL, library_id INTEGER, title TEXT, last_chapter_read INTEGER, total_chapters INTEGER, score REAL, status INTEGER, tracking_url TEXT, start_date INTEGER, finish_date INTEGER)")
    }
}

internal val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS manga_sync (_id INTEGER PRIMARY KEY AUTOINCREMENT, manga_id INTEGER NOT NULL, sync_id INTEGER NOT NULL, remote_id INTEGER NOT NULL, library_id INTEGER, title TEXT, last_chapter_read INTEGER, total_chapters INTEGER, score REAL, status INTEGER, tracking_url TEXT, start_date INTEGER, finish_date INTEGER)")
        db.execSQL("INSERT INTO manga_sync SELECT * FROM tracks")
        db.execSQL("DROP TABLE tracks")
    }
}

internal val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `categorization_results`")
        db.execSQL("DROP TABLE IF EXISTS `smart_search_cache`")
        db.execSQL("DROP TABLE IF EXISTS `recommendations`")
        db.execSQL("DROP TABLE IF EXISTS `reading_patterns`")
        db.execSQL("DROP TABLE IF EXISTS `recommendation_refreshes`")
    }
}

internal val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reading_streaks` (
                `date` TEXT PRIMARY KEY NOT NULL,
                `chapter_count` INTEGER NOT NULL DEFAULT 0,
                `read_duration_ms` INTEGER NOT NULL DEFAULT 0,
                `last_read_at` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }
}

internal val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `manga` ADD COLUMN `userCompleted` INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `manga` ADD COLUMN `userDropped` INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `page_bookmarks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `manga_id` INTEGER NOT NULL,
                `chapter_id` INTEGER NOT NULL,
                `page_index` INTEGER NOT NULL,
                `note` TEXT,
                `created_at` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`chapter_id`) REFERENCES `chapters`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`manga_id`) REFERENCES `manga`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_page_bookmarks_chapter_id` ON `page_bookmarks` (`chapter_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_page_bookmarks_manga_id` ON `page_bookmarks` (`manga_id`)")
    }
}

internal val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chapters ADD COLUMN userNotes TEXT")
    }
}

internal val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reading_lists` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `color` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reading_list_items` (
                `listId` INTEGER NOT NULL,
                `mangaId` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `addedAt` INTEGER NOT NULL,
                `note` TEXT,
                PRIMARY KEY(`listId`, `mangaId`),
                FOREIGN KEY(`listId`) REFERENCES `reading_lists`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`mangaId`) REFERENCES `manga`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_list_items_listId` ON `reading_list_items`(`listId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_list_items_mangaId` ON `reading_list_items`(`mangaId`)")
    }
}

/** All migrations in order, for use in [Room.databaseBuilder] and migration tests. */
internal val ALL_MIGRATIONS = arrayOf(
    MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
    MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
    MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
    MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18,
    MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21
)
