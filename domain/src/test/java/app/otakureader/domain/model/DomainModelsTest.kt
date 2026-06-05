package app.otakureader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainModelsTest {

    // ── ContentRating ──────────────────────────────────────────────────────────

    @Test
    fun `ContentRating fromOrdinal returns correct entries`() {
        assertEquals(ContentRating.SAFE, ContentRating.fromOrdinal(0))
        assertEquals(ContentRating.SUGGESTIVE, ContentRating.fromOrdinal(1))
        assertEquals(ContentRating.EROTICA, ContentRating.fromOrdinal(2))
        assertEquals(ContentRating.PORNOGRAPHIC, ContentRating.fromOrdinal(3))
    }

    @Test
    fun `ContentRating fromOrdinal out of bounds defaults to SAFE`() {
        assertEquals(ContentRating.SAFE, ContentRating.fromOrdinal(999))
        assertEquals(ContentRating.SAFE, ContentRating.fromOrdinal(-1))
    }

    // ── MangaStatus ────────────────────────────────────────────────────────────

    @Test
    fun `MangaStatus fromOrdinal returns correct entries`() {
        assertEquals(MangaStatus.UNKNOWN, MangaStatus.fromOrdinal(0))
        assertEquals(MangaStatus.ONGOING, MangaStatus.fromOrdinal(1))
        assertEquals(MangaStatus.COMPLETED, MangaStatus.fromOrdinal(2))
        assertEquals(MangaStatus.LICENSED, MangaStatus.fromOrdinal(3))
        assertEquals(MangaStatus.PUBLISHING_FINISHED, MangaStatus.fromOrdinal(4))
        assertEquals(MangaStatus.CANCELLED, MangaStatus.fromOrdinal(5))
        assertEquals(MangaStatus.ON_HIATUS, MangaStatus.fromOrdinal(6))
    }

    @Test
    fun `MangaStatus fromOrdinal out of bounds defaults to UNKNOWN`() {
        assertEquals(MangaStatus.UNKNOWN, MangaStatus.fromOrdinal(99))
    }

    // ── TrackStatus ────────────────────────────────────────────────────────────

    @Test
    fun `TrackStatus fromOrdinal returns correct entries`() {
        assertEquals(TrackStatus.READING, TrackStatus.fromOrdinal(0))
        assertEquals(TrackStatus.COMPLETED, TrackStatus.fromOrdinal(1))
        assertEquals(TrackStatus.ON_HOLD, TrackStatus.fromOrdinal(2))
        assertEquals(TrackStatus.DROPPED, TrackStatus.fromOrdinal(3))
        assertEquals(TrackStatus.PLAN_TO_READ, TrackStatus.fromOrdinal(4))
        assertEquals(TrackStatus.RE_READING, TrackStatus.fromOrdinal(5))
    }

    @Test
    fun `TrackStatus fromOrdinal out of bounds defaults to PLAN_TO_READ`() {
        assertEquals(TrackStatus.PLAN_TO_READ, TrackStatus.fromOrdinal(-1))
        assertEquals(TrackStatus.PLAN_TO_READ, TrackStatus.fromOrdinal(100))
    }

    // ── TrackEntry ─────────────────────────────────────────────────────────────

    @Test
    fun `TrackEntry default values are correct`() {
        val entry = TrackEntry(remoteId = 42L, mangaId = 1L, trackerId = TrackerType.ANILIST)
        assertEquals(42L, entry.remoteId)
        assertEquals(1L, entry.mangaId)
        assertEquals(TrackerType.ANILIST, entry.trackerId)
        assertEquals("", entry.title)
        assertEquals(TrackStatus.PLAN_TO_READ, entry.status)
        assertEquals(0f, entry.lastChapterRead)
        assertEquals(0f, entry.score)
    }

    @Test
    fun `TrackEntry copy preserves unchanged fields`() {
        val original = TrackEntry(remoteId = 1L, mangaId = 2L, trackerId = TrackerType.MY_ANIME_LIST, score = 8.5f)
        val updated = original.copy(lastChapterRead = 10f, status = TrackStatus.READING)
        assertEquals(8.5f, updated.score)
        assertEquals(1L, updated.remoteId)
        assertEquals(10f, updated.lastChapterRead)
        assertEquals(TrackStatus.READING, updated.status)
    }

    // ── ChapterWithHistory ─────────────────────────────────────────────────────

    @Test
    fun `ChapterWithHistory default values are correct`() {
        val chapter = Chapter(id = 1L, mangaId = 2L, url = "/c/1", name = "Ch 1")
        val withHistory = ChapterWithHistory(chapter = chapter)
        assertEquals(chapter, withHistory.chapter)
        assertEquals(0L, withHistory.readAt)
        assertEquals(0L, withHistory.readDurationMs)
        assertNull(withHistory.mangaTitle)
        assertNull(withHistory.mangaThumbnailUrl)
    }

    @Test
    fun `ChapterWithHistory with manga metadata`() {
        val chapter = Chapter(id = 5L, mangaId = 3L, url = "/c/5", name = "Ch 5", read = true)
        val withHistory = ChapterWithHistory(
            chapter = chapter,
            readAt = 1000L,
            readDurationMs = 300L,
            mangaTitle = "One Piece",
            mangaThumbnailUrl = "https://example.com/cover.jpg"
        )
        assertEquals("One Piece", withHistory.mangaTitle)
        assertEquals(300L, withHistory.readDurationMs)
        assertEquals(1000L, withHistory.readAt)
    }

    // ── MigrationCandidate ─────────────────────────────────────────────────────

    @Test
    fun `MigrationCandidate default values are correct`() {
        val candidate = MigrationCandidate(sourceId = 10L, url = "/m/1", title = "Naruto")
        assertEquals(10L, candidate.sourceId)
        assertEquals("Naruto", candidate.title)
        assertEquals(MangaStatus.UNKNOWN, candidate.status)
        assertEquals(0f, candidate.similarityScore)
        assertEquals(0, candidate.chapterCount)
        assertNull(candidate.thumbnailUrl)
    }

    @Test
    fun `MigrationCandidate with similarity score`() {
        val candidate = MigrationCandidate(
            sourceId = 1L,
            url = "/m/2",
            title = "Bleach",
            chapterCount = 686,
            similarityScore = 0.95f,
            status = MangaStatus.COMPLETED
        )
        assertEquals(686, candidate.chapterCount)
        assertEquals(0.95f, candidate.similarityScore)
        assertEquals(MangaStatus.COMPLETED, candidate.status)
    }

    // ── MigrationMode / MigrationStatus ───────────────────────────────────────

    @Test
    fun `MigrationMode entries exist`() {
        assertEquals(2, MigrationMode.entries.size)
        assertEquals(MigrationMode.COPY, MigrationMode.valueOf("COPY"))
        assertEquals(MigrationMode.MOVE, MigrationMode.valueOf("MOVE"))
    }

    @Test
    fun `MigrationStatus all entries accessible`() {
        assertEquals(7, MigrationStatus.entries.size)
        assertEquals(MigrationStatus.PENDING, MigrationStatus.valueOf("PENDING"))
        assertEquals(MigrationStatus.COMPLETED, MigrationStatus.valueOf("COMPLETED"))
        assertEquals(MigrationStatus.FAILED, MigrationStatus.valueOf("FAILED"))
    }

    // ── TrackerType constants ─────────────────────────────────────────────────

    @Test
    fun `TrackerType constants have expected values`() {
        assertEquals(1, TrackerType.MY_ANIME_LIST)
        assertEquals(2, TrackerType.ANILIST)
        assertEquals(3, TrackerType.KITSU)
        assertEquals(4, TrackerType.MANGA_UPDATES)
        assertEquals(5, TrackerType.SHIKIMORI)
    }

    // ── ContinueReadingItem ───────────────────────────────────────────────────

    @Test
    fun `ContinueReadingItem fields are correctly stored`() {
        val item = ContinueReadingItem(
            mangaId = 1L,
            chapterId = 10L,
            mangaTitle = "Naruto",
            thumbnailUrl = "https://example.com/cover.jpg",
            chapterName = "Chapter 1",
            chapterNumber = 1.0f,
            lastPageRead = 5,
            readAt = 1000L
        )
        assertEquals(1L, item.mangaId)
        assertEquals(10L, item.chapterId)
        assertEquals("Naruto", item.mangaTitle)
        assertEquals(5, item.lastPageRead)
        assertEquals(1000L, item.readAt)
    }

    @Test
    fun `ContinueReadingItem nullable thumbnailUrl is allowed`() {
        val item = ContinueReadingItem(
            mangaId = 2L, chapterId = 20L, mangaTitle = "Bleach",
            thumbnailUrl = null, chapterName = "Ch 2", chapterNumber = 2.0f,
            lastPageRead = 0, readAt = 2000L
        )
        assertNull(item.thumbnailUrl)
    }
}
