package app.otakureader.data.backup.mapper

import app.otakureader.core.database.entity.CategoryEntity
import app.otakureader.core.database.entity.ChapterEntity
import app.otakureader.core.database.entity.MangaEntity
import app.otakureader.core.database.entity.ReadingHistoryEntity
import app.otakureader.data.backup.model.BackupCategory
import app.otakureader.data.backup.model.BackupChapter
import app.otakureader.data.backup.model.BackupManga
import app.otakureader.data.backup.model.BackupPreferences
import app.otakureader.data.backup.model.BackupReadingHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupMappersTest {

    // ── MangaEntity → BackupManga ─────────────────────────────────────────────

    @Test
    fun mangaEntity_toBackupManga_mapsAllFields() {
        val entity = MangaEntity(
            id = 1L,
            sourceId = 100L,
            url = "/manga/naruto",
            title = "Naruto",
            thumbnailUrl = "https://example.com/thumb.jpg",
            author = "Masashi Kishimoto",
            artist = "Masashi Kishimoto",
            description = "A ninja story",
            genre = "Action|||Adventure|||Comedy",
            status = 2,
            favorite = true,
            lastUpdate = 1_000_000L,
            initialized = true,
            viewerFlags = 3,
            chapterFlags = 5,
            coverLastModified = 500_000L,
            dateAdded = 200_000L,
            notes = "Great manga",
            readerBackgroundColor = 0xFF000000L
        )

        val backup = entity.toBackupManga()

        assertEquals(100L, backup.sourceId)
        assertEquals("/manga/naruto", backup.url)
        assertEquals("Naruto", backup.title)
        assertEquals("https://example.com/thumb.jpg", backup.thumbnailUrl)
        assertEquals("Masashi Kishimoto", backup.author)
        assertEquals("Masashi Kishimoto", backup.artist)
        assertEquals("A ninja story", backup.description)
        assertEquals(listOf("Action", "Adventure", "Comedy"), backup.genre)
        assertEquals(2, backup.status)
        assertTrue(backup.favorite)
        assertEquals(1_000_000L, backup.lastUpdate)
        assertTrue(backup.initialized)
        assertEquals(3, backup.viewerFlags)
        assertEquals(5, backup.chapterFlags)
        assertEquals(500_000L, backup.coverLastModified)
        assertEquals(200_000L, backup.dateAdded)
        assertEquals("Great manga", backup.notes)
        assertEquals(0xFF000000L, backup.readerBackgroundColor)
    }

    @Test
    fun mangaEntity_toBackupManga_withNullOptionalFields_mapsCorrectly() {
        val entity = MangaEntity(
            sourceId = 1L,
            url = "/m/1",
            title = "Minimal Manga",
            genre = null
        )

        val backup = entity.toBackupManga()

        assertNull(backup.thumbnailUrl)
        assertNull(backup.author)
        assertNull(backup.artist)
        assertNull(backup.description)
        assertTrue(backup.genre.isEmpty())
        assertNull(backup.notes)
        assertNull(backup.readerBackgroundColor)
    }

    @Test
    fun mangaEntity_toBackupManga_withEmptyGenre_mapsToEmptyList() {
        val entity = MangaEntity(
            sourceId = 1L,
            url = "/m/1",
            title = "Test",
            genre = ""
        )

        val backup = entity.toBackupManga()

        assertTrue(backup.genre.isEmpty())
    }

    @Test
    fun mangaEntity_toBackupManga_withSingleGenre_mapsToSingletonList() {
        val entity = MangaEntity(
            sourceId = 1L,
            url = "/m/1",
            title = "Test",
            genre = "Action"
        )

        val backup = entity.toBackupManga()

        assertEquals(listOf("Action"), backup.genre)
    }

    @Test
    fun mangaEntity_toBackupManga_withChaptersAndCategoryIds_includesThemInResult() {
        val entity = MangaEntity(sourceId = 1L, url = "/m/1", title = "Test")
        val chapters = listOf(BackupChapter(url = "/c/1", name = "Chapter 1"))
        val categoryIds = listOf(10L, 20L)

        val backup = entity.toBackupManga(chapters = chapters, categoryIds = categoryIds)

        assertEquals(1, backup.chapters.size)
        assertEquals("/c/1", backup.chapters.first().url)
        assertEquals(listOf(10L, 20L), backup.categoryIds)
    }

    // ── BackupManga → MangaEntity ─────────────────────────────────────────────

    @Test
    fun backupManga_toMangaEntity_mapsAllFields() {
        val backup = BackupManga(
            sourceId = 200L,
            url = "/manga/bleach",
            title = "Bleach",
            thumbnailUrl = "https://example.com/bleach.jpg",
            author = "Tite Kubo",
            artist = "Tite Kubo",
            description = "Soul reaper story",
            genre = listOf("Action", "Supernatural"),
            status = 1,
            favorite = false,
            lastUpdate = 2_000_000L,
            initialized = true,
            viewerFlags = 0,
            chapterFlags = 0,
            coverLastModified = 300_000L,
            dateAdded = 100_000L,
            notes = null,
            readerBackgroundColor = null
        )

        val entity = backup.toMangaEntity()

        // id should be reset to 0 (auto-generate)
        assertEquals(0L, entity.id)
        assertEquals(200L, entity.sourceId)
        assertEquals("/manga/bleach", entity.url)
        assertEquals("Bleach", entity.title)
        assertEquals("https://example.com/bleach.jpg", entity.thumbnailUrl)
        assertEquals("Tite Kubo", entity.author)
        assertEquals("Action|||Supernatural", entity.genre)
        assertEquals(1, entity.status)
        assertTrue(entity.initialized)
    }

    @Test
    fun backupManga_toMangaEntity_alwaysResetsIdToZero() {
        val backup = BackupManga(sourceId = 1L, url = "/m/1", title = "Test")

        val entity = backup.toMangaEntity()

        assertEquals(0L, entity.id)
    }

    @Test
    fun backupManga_toMangaEntity_genreListJoinedWithSeparator() {
        val backup = BackupManga(
            sourceId = 1L,
            url = "/m/1",
            title = "Test",
            genre = listOf("Comedy", "Romance", "Drama")
        )

        val entity = backup.toMangaEntity()

        assertEquals("Comedy|||Romance|||Drama", entity.genre)
    }

    @Test
    fun backupManga_toMangaEntity_emptyGenreListProducesEmptyString() {
        val backup = BackupManga(sourceId = 1L, url = "/m/1", title = "Test", genre = emptyList())

        val entity = backup.toMangaEntity()

        assertEquals("", entity.genre)
    }

    // ── Roundtrip: MangaEntity → BackupManga → MangaEntity ───────────────────

    @Test
    fun mangaEntity_roundtripThroughBackup_preservesFields() {
        val original = MangaEntity(
            id = 42L,
            sourceId = 5L,
            url = "/m/roundtrip",
            title = "Roundtrip Manga",
            thumbnailUrl = "https://example.com/rt.jpg",
            author = "Author",
            artist = "Artist",
            description = "Round trip test",
            genre = "Fantasy|||Sci-Fi",
            status = 3,
            favorite = true,
            lastUpdate = 99L,
            initialized = true,
            viewerFlags = 7,
            chapterFlags = 2,
            coverLastModified = 50L,
            dateAdded = 10L,
            notes = "Test note",
            readerBackgroundColor = 0xFFFFFFFFL
        )

        val restored = original.toBackupManga().toMangaEntity()

        // id is reset to 0 on restore
        assertEquals(0L, restored.id)
        assertEquals(original.sourceId, restored.sourceId)
        assertEquals(original.url, restored.url)
        assertEquals(original.title, restored.title)
        assertEquals(original.thumbnailUrl, restored.thumbnailUrl)
        assertEquals(original.author, restored.author)
        assertEquals(original.artist, restored.artist)
        assertEquals(original.description, restored.description)
        assertEquals(original.genre, restored.genre)
        assertEquals(original.status, restored.status)
        assertEquals(original.favorite, restored.favorite)
        assertEquals(original.lastUpdate, restored.lastUpdate)
        assertEquals(original.initialized, restored.initialized)
        assertEquals(original.viewerFlags, restored.viewerFlags)
        assertEquals(original.chapterFlags, restored.chapterFlags)
        assertEquals(original.coverLastModified, restored.coverLastModified)
        assertEquals(original.dateAdded, restored.dateAdded)
        assertEquals(original.notes, restored.notes)
        assertEquals(original.readerBackgroundColor, restored.readerBackgroundColor)
    }

    // ── ChapterEntity → BackupChapter ─────────────────────────────────────────

    @Test
    fun chapterEntity_toBackupChapter_mapsAllFields() {
        val entity = ChapterEntity(
            id = 10L,
            mangaId = 1L,
            url = "/c/10",
            name = "Chapter 10",
            scanlator = "ScanGroup",
            read = true,
            bookmark = false,
            lastPageRead = 42,
            chapterNumber = 10.5f,
            sourceOrder = 5,
            dateFetch = 1_000L,
            dateUpload = 2_000L,
            lastModified = 3_000L
        )

        val backup = entity.toBackupChapter()

        assertEquals("/c/10", backup.url)
        assertEquals("Chapter 10", backup.name)
        assertEquals("ScanGroup", backup.scanlator)
        assertTrue(backup.read)
        assertEquals(42, backup.lastPageRead)
        assertEquals(10.5f, backup.chapterNumber)
        assertEquals(5, backup.sourceOrder)
        assertEquals(1_000L, backup.dateFetch)
        assertEquals(2_000L, backup.dateUpload)
        assertEquals(3_000L, backup.lastModified)
        assertNull(backup.readingHistory)
    }

    @Test
    fun chapterEntity_toBackupChapter_withReadingHistory_includesHistory() {
        val entity = ChapterEntity(
            mangaId = 1L, url = "/c/1", name = "Chapter 1"
        )
        val history = BackupReadingHistory(readAt = 9_000L, readDurationMs = 300_000L)

        val backup = entity.toBackupChapter(readingHistory = history)

        assertEquals(9_000L, backup.readingHistory?.readAt)
        assertEquals(300_000L, backup.readingHistory?.readDurationMs)
    }

    // ── BackupChapter → ChapterEntity ─────────────────────────────────────────

    @Test
    fun backupChapter_toChapterEntity_resetsIdAndSetsMangaId() {
        val backup = BackupChapter(
            url = "/c/1",
            name = "Chapter 1",
            scanlator = "Group",
            read = true,
            bookmark = true,
            lastPageRead = 15,
            chapterNumber = 1.0f,
            sourceOrder = 1,
            dateFetch = 100L,
            dateUpload = 200L,
            lastModified = 300L
        )

        val entity = backup.toChapterEntity(mangaId = 55L)

        assertEquals(0L, entity.id)
        assertEquals(55L, entity.mangaId)
        assertEquals("/c/1", entity.url)
        assertEquals("Chapter 1", entity.name)
        assertTrue(entity.read)
        assertTrue(entity.bookmark)
        assertEquals(15, entity.lastPageRead)
        assertEquals(1.0f, entity.chapterNumber)
    }

    // ── ReadingHistoryEntity ──────────────────────────────────────────────────

    @Test
    fun readingHistoryEntity_toBackupReadingHistory_mapsFields() {
        val entity = ReadingHistoryEntity(
            id = 5L,
            chapterId = 100L,
            readAt = 1_500_000L,
            readDurationMs = 600_000L
        )

        val backup = entity.toBackupReadingHistory()

        assertEquals(1_500_000L, backup.readAt)
        assertEquals(600_000L, backup.readDurationMs)
    }

    @Test
    fun backupReadingHistory_toReadingHistoryEntity_resetsIdAndSetsChapterId() {
        val backup = BackupReadingHistory(readAt = 2_000_000L, readDurationMs = 300_000L)

        val entity = backup.toReadingHistoryEntity(chapterId = 77L)

        assertEquals(0L, entity.id)
        assertEquals(77L, entity.chapterId)
        assertEquals(2_000_000L, entity.readAt)
        assertEquals(300_000L, entity.readDurationMs)
    }

    // ── CategoryEntity ────────────────────────────────────────────────────────

    @Test
    fun categoryEntity_toBackupCategory_mapsAllFields() {
        val entity = CategoryEntity(
            id = 3L,
            name = "Favorites",
            order = 1,
            flags = CategoryEntity.FLAG_HIDDEN
        )

        val backup = entity.toBackupCategory()

        assertEquals(3L, backup.id)
        assertEquals("Favorites", backup.name)
        assertEquals(1, backup.order)
        assertEquals(CategoryEntity.FLAG_HIDDEN, backup.flags)
    }

    @Test
    fun backupCategory_toCategoryEntity_preservesId() {
        val backup = BackupCategory(id = 10L, name = "Action", order = 2, flags = 0)

        val entity = backup.toCategoryEntity()

        assertEquals(10L, entity.id)
        assertEquals("Action", entity.name)
        assertEquals(2, entity.order)
        assertEquals(0, entity.flags)
    }

    @Test
    fun categoryEntity_roundtripThroughBackup_preservesAllFields() {
        val original = CategoryEntity(id = 7L, name = "Completed", order = 3, flags = 2)

        val restored = original.toBackupCategory().toCategoryEntity()

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.order, restored.order)
        assertEquals(original.flags, restored.flags)
    }

    // ── createBackupPreferences ───────────────────────────────────────────────

    @Test
    fun createBackupPreferences_mapsAllArguments() {
        val prefs = createBackupPreferences(
            themeMode = 2,
            useDynamicColor = false,
            locale = "ja",
            readerMode = 1,
            keepScreenOn = false,
            volumeKeysEnabled = true,
            volumeKeysInverted = true,
            libraryGridSize = 4,
            showBadges = false,
            updateCheckInterval = 6,
            notificationsEnabled = false
        )

        assertEquals(2, prefs.themeMode)
        assertEquals(false, prefs.useDynamicColor)
        assertEquals("ja", prefs.locale)
        assertEquals(1, prefs.readerMode)
        assertEquals(false, prefs.keepScreenOn)
        assertEquals(true, prefs.volumeKeysEnabled)
        assertEquals(true, prefs.volumeKeysInverted)
        assertEquals(4, prefs.libraryGridSize)
        assertEquals(false, prefs.showBadges)
        assertEquals(6, prefs.updateCheckInterval)
        assertEquals(false, prefs.notificationsEnabled)
    }

    @Test
    fun createBackupPreferences_withDefaults_producesExpectedValues() {
        val prefs = createBackupPreferences(
            themeMode = 0,
            useDynamicColor = true,
            locale = "",
            readerMode = 0,
            keepScreenOn = true,
            volumeKeysEnabled = false,
            volumeKeysInverted = false,
            libraryGridSize = 3,
            showBadges = true,
            updateCheckInterval = 12,
            notificationsEnabled = true
        )

        assertEquals(0, prefs.themeMode)
        assertTrue(prefs.useDynamicColor)
        assertEquals("", prefs.locale)
        assertTrue(prefs.keepScreenOn)
        assertEquals(12, prefs.updateCheckInterval)
        assertTrue(prefs.notificationsEnabled)
    }
}
