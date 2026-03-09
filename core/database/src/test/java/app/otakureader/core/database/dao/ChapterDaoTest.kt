package app.otakureader.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.otakureader.core.database.OtakuReaderDatabase
import app.otakureader.core.database.entity.ChapterEntity
import app.otakureader.core.database.entity.MangaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChapterDaoTest {

    private lateinit var database: OtakuReaderDatabase
    private lateinit var chapterDao: ChapterDao
    private lateinit var mangaDao: MangaDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OtakuReaderDatabase::class.java
        ).allowMainThreadQueries().build()
        chapterDao = database.chapterDao()
        mangaDao = database.mangaDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun benchmarkUpdateMultipleChapters() = runBlocking {
        val mangaId = 1L
        mangaDao.insert(MangaEntity(id = mangaId, title = "Test Manga", sourceId = 1L, url = "url", favorite = true))

        val chapterCount = 1000
        val chapters = (1..chapterCount).map { i ->
            ChapterEntity(
                id = i.toLong(),
                mangaId = mangaId,
                url = "url_$i",
                name = "Chapter $i",
                read = false,
                chapterNumber = i.toFloat()
            )
        }
        chapterDao.insertAll(chapters)

        val chapterIds = chapters.map { it.id }

        val startTime = System.currentTimeMillis()

        // Simulate the N+1 query problem
        chapterIds.forEach { id ->
            chapterDao.updateChapterProgress(id, true, 0)
        }

        val endTime = System.currentTimeMillis()
        println("BENCHMARK: N+1 update took ${endTime - startTime} ms")

        // Verify all were updated
        val unreadCount = chapterDao.getUnreadCountByMangaId(mangaId).first()
        assertEquals(0, unreadCount)
    }

    @Test
    fun benchmarkUpdateMultipleChaptersOptimized() = runBlocking {
        val mangaId = 2L
        mangaDao.insert(MangaEntity(id = mangaId, title = "Test Manga 2", sourceId = 1L, url = "url2", favorite = true))

        val chapterCount = 1000
        val chapters = (1..chapterCount).map { i ->
            ChapterEntity(
                id = i.toLong() + 2000,
                mangaId = mangaId,
                url = "url_$i",
                name = "Chapter $i",
                read = false,
                chapterNumber = i.toFloat()
            )
        }
        chapterDao.insertAll(chapters)

        val chapterIds = chapters.map { it.id }

        val startTime = System.currentTimeMillis()

        // Use optimized query
        chapterDao.updateChapterProgress(chapterIds, true, 0)

        val endTime = System.currentTimeMillis()
        println("BENCHMARK: Optimized update took ${endTime - startTime} ms")

        // Verify all were updated
        val unreadCount = chapterDao.getUnreadCountByMangaId(mangaId).first()
        assertEquals(0, unreadCount)
    }

}
