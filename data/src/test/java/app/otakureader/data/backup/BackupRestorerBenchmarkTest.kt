package app.otakureader.data.backup

import app.otakureader.core.database.dao.*
import app.otakureader.core.database.entity.*
import app.otakureader.core.preferences.*
import app.otakureader.data.backup.model.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.system.measureTimeMillis

class BackupRestorerBenchmarkTest {

    @Test
    fun benchmarkRestoreChapters() = runBlocking {
        val chapterDao = mockk<ChapterDao>(relaxed = true)
        val readingHistoryDao = mockk<ReadingHistoryDao>(relaxed = true)
        val mangaDao = mockk<MangaDao>(relaxed = true)
        val categoryDao = mockk<CategoryDao>(relaxed = true)
        val mangaCategoryDao = mockk<MangaCategoryDao>(relaxed = true)

        val restorer = BackupRestorer(
            mangaDao = mangaDao,
            chapterDao = chapterDao,
            categoryDao = categoryDao,
            mangaCategoryDao = mangaCategoryDao,
            readingHistoryDao = readingHistoryDao,
            generalPreferences = mockk(relaxed = true),
            libraryPreferences = mockk(relaxed = true),
            readerPreferences = mockk(relaxed = true)
        )

        // Create 1000 chapters
        val chapters = (1..1000).map { i ->
            BackupChapter(
                url = "url_$i",
                name = "Chapter $i",
                readingHistory = BackupReadingHistory(readAt = 123L)
            )
        }

        val backupManga = BackupManga(
            sourceId = 1L,
            url = "manga_url",
            title = "Manga",
            chapters = chapters
        )

        val backupData = BackupData(
            manga = listOf(backupManga)
        )

        val jsonString = kotlinx.serialization.json.Json.encodeToString(BackupData.serializer(), backupData)

        // Mock DB calls
        coEvery { mangaDao.getMangaBySourceAndUrl(any(), any()) } returns null
        coEvery { mangaDao.insert(any()) } returns 1L
        every { chapterDao.getChaptersByMangaId(any()) } returns flowOf(emptyList())
        coEvery { chapterDao.insertAllWithIds(any()) } returns (1..1000L).toList()
        coEvery { chapterDao.insert(any()) } returns 1L

        val time = measureTimeMillis {
            restorer.restoreBackup(jsonString)
        }

        println("Restore took $time ms")
    }
}
