package app.otakureader.data.repository

import app.otakureader.core.database.dao.PageBookmarkDao
import app.otakureader.core.database.entity.PageBookmarkEntity
import app.otakureader.domain.model.PageBookmark
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PageBookmarkRepositoryImplTest {

    private lateinit var pageBookmarkDao: PageBookmarkDao
    private lateinit var repository: PageBookmarkRepositoryImpl

    private fun makeEntity(
        id: Long = 1L,
        mangaId: Long = 10L,
        chapterId: Long = 100L,
        pageIndex: Int = 0,
        note: String? = null,
        createdAt: Long = 1000L
    ) = PageBookmarkEntity(
        id = id,
        mangaId = mangaId,
        chapterId = chapterId,
        pageIndex = pageIndex,
        note = note,
        createdAt = createdAt
    )

    private fun makeBookmark(
        id: Long = 1L,
        mangaId: Long = 10L,
        chapterId: Long = 100L,
        pageIndex: Int = 0,
        note: String? = null,
        createdAt: Long = 1000L
    ) = PageBookmark(
        id = id,
        mangaId = mangaId,
        chapterId = chapterId,
        pageIndex = pageIndex,
        note = note,
        createdAt = createdAt
    )

    @Before
    fun setUp() {
        pageBookmarkDao = mockk()
        repository = PageBookmarkRepositoryImpl(pageBookmarkDao)
    }

    // ---- getBookmarksForManga ----

    @Test
    fun `getBookmarksForManga returns mapped domain models`() = runTest {
        val entities = listOf(
            makeEntity(id = 1L, pageIndex = 0),
            makeEntity(id = 2L, pageIndex = 5)
        )
        every { pageBookmarkDao.getBookmarksForManga(10L) } returns flowOf(entities)

        repository.getBookmarksForManga(10L).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals(0, result[0].pageIndex)
            assertEquals(5, result[1].pageIndex)
            awaitComplete()
        }
    }

    @Test
    fun `getBookmarksForManga with no bookmarks returns empty list`() = runTest {
        every { pageBookmarkDao.getBookmarksForManga(99L) } returns flowOf(emptyList())

        repository.getBookmarksForManga(99L).test {
            assertEquals(emptyList<PageBookmark>(), awaitItem())
            awaitComplete()
        }
    }

    // ---- getBookmarksForChapter ----

    @Test
    fun `getBookmarksForChapter returns mapped domain models`() = runTest {
        val entities = listOf(makeEntity(chapterId = 100L, pageIndex = 3))
        every { pageBookmarkDao.getBookmarksForChapter(100L) } returns flowOf(entities)

        repository.getBookmarksForChapter(100L).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(3, result[0].pageIndex)
            awaitComplete()
        }
    }

    // ---- getBookmark ----

    @Test
    fun `getBookmark returns domain model when entity exists`() = runTest {
        val entity = makeEntity(chapterId = 100L, pageIndex = 2, note = "Great art")
        coEvery { pageBookmarkDao.getBookmark(100L, 2) } returns entity

        val result = repository.getBookmark(100L, 2)

        assertEquals(2, result?.pageIndex)
        assertEquals("Great art", result?.note)
    }

    @Test
    fun `getBookmark returns null when no bookmark exists`() = runTest {
        coEvery { pageBookmarkDao.getBookmark(100L, 999) } returns null

        val result = repository.getBookmark(100L, 999)

        assertNull(result)
    }

    // ---- isPageBookmarked ----

    @Test
    fun `isPageBookmarked returns true when page is bookmarked`() = runTest {
        every { pageBookmarkDao.isPageBookmarked(100L, 5) } returns flowOf(true)

        repository.isPageBookmarked(100L, 5).test {
            assertTrue(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isPageBookmarked returns false when page is not bookmarked`() = runTest {
        every { pageBookmarkDao.isPageBookmarked(100L, 5) } returns flowOf(false)

        repository.isPageBookmarked(100L, 5).test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    // ---- addBookmark ----

    @Test
    fun `addBookmark inserts entity and returns generated id`() = runTest {
        coEvery { pageBookmarkDao.insertBookmark(any()) } returns 55L

        val bookmark = makeBookmark(id = 0L, note = "Bookmark note")
        val id = repository.addBookmark(bookmark)

        assertEquals(55L, id)
        coVerify {
            pageBookmarkDao.insertBookmark(match {
                it.note == "Bookmark note" && it.mangaId == 10L
            })
        }
    }

    // ---- removeBookmark ----

    @Test
    fun `removeBookmark calls dao with correct args`() = runTest {
        coEvery { pageBookmarkDao.deleteBookmark(100L, 3) } returns Unit

        repository.removeBookmark(100L, 3)

        coVerify { pageBookmarkDao.deleteBookmark(100L, 3) }
    }

    // ---- removeAllBookmarksForManga ----

    @Test
    fun `removeAllBookmarksForManga calls dao with manga id`() = runTest {
        coEvery { pageBookmarkDao.deleteAllBookmarksForManga(10L) } returns Unit

        repository.removeAllBookmarksForManga(10L)

        coVerify { pageBookmarkDao.deleteAllBookmarksForManga(10L) }
    }

    // ---- getBookmarkCountForManga ----

    @Test
    fun `getBookmarkCountForManga returns count flow from dao`() = runTest {
        every { pageBookmarkDao.getBookmarkCountForManga(10L) } returns flowOf(7)

        repository.getBookmarkCountForManga(10L).test {
            assertEquals(7, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getBookmarkCountForManga returns zero when no bookmarks`() = runTest {
        every { pageBookmarkDao.getBookmarkCountForManga(10L) } returns flowOf(0)

        repository.getBookmarkCountForManga(10L).test {
            assertEquals(0, awaitItem())
            awaitComplete()
        }
    }

    // ---- entity to domain mapping ----

    @Test
    fun `bookmark entity is correctly mapped to domain model`() = runTest {
        val entity = makeEntity(
            id = 5L, mangaId = 20L, chapterId = 200L,
            pageIndex = 10, note = "Favorite scene", createdAt = 9999L
        )
        every { pageBookmarkDao.getBookmarksForManga(20L) } returns flowOf(listOf(entity))

        repository.getBookmarksForManga(20L).test {
            val bookmark = awaitItem().first()
            assertEquals(5L, bookmark.id)
            assertEquals(20L, bookmark.mangaId)
            assertEquals(200L, bookmark.chapterId)
            assertEquals(10, bookmark.pageIndex)
            assertEquals("Favorite scene", bookmark.note)
            assertEquals(9999L, bookmark.createdAt)
            awaitComplete()
        }
    }
}
