package app.otakureader.data.repository

import app.otakureader.core.database.dao.FeedDao
import app.otakureader.core.database.entity.FeedItemEntity
import app.otakureader.core.database.entity.FeedSavedSearchEntity
import app.otakureader.core.database.entity.FeedSourceEntity
import app.otakureader.domain.model.FeedSavedSearch
import app.otakureader.domain.model.FeedSource
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class FeedRepositoryImplTest {

    private lateinit var feedDao: FeedDao
    private lateinit var repository: FeedRepositoryImpl

    private fun makeFeedSource(
        id: Long = 1L,
        sourceId: Long = 100L,
        sourceName: String = "MangaDex",
        isEnabled: Boolean = true,
        itemCount: Int = 20,
        order: Int = 0
    ) = FeedSourceEntity(id = id, sourceId = sourceId, sourceName = sourceName,
        isEnabled = isEnabled, itemCount = itemCount, order = order)

    private fun makeFeedItem(
        id: Long = 1L,
        mangaId: Long = 1L,
        mangaTitle: String = "Naruto",
        chapterId: Long = 10L,
        chapterName: String = "Chapter 1",
        chapterNumber: Float = 1f,
        sourceId: Long = 100L,
        sourceName: String = "MangaDex"
    ) = FeedItemEntity(
        id = id,
        mangaId = mangaId,
        mangaTitle = mangaTitle,
        mangaThumbnailUrl = null,
        chapterId = chapterId,
        chapterName = chapterName,
        chapterNumber = chapterNumber,
        sourceId = sourceId,
        sourceName = sourceName,
        timestamp = Instant.ofEpochMilli(1000L)
    )

    @Before
    fun setUp() {
        feedDao = mockk()
        repository = FeedRepositoryImpl(feedDao)
    }

    // ---- getFeedSources ----

    @Test
    fun `getFeedSources returns mapped domain models`() = runTest {
        val entities = listOf(
            makeFeedSource(sourceId = 1L, sourceName = "MangaDex"),
            makeFeedSource(id = 2L, sourceId = 2L, sourceName = "Komga")
        )
        every { feedDao.getFeedSources() } returns flowOf(entities)

        repository.getFeedSources().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("MangaDex", result[0].sourceName)
            assertEquals("Komga", result[1].sourceName)
            awaitComplete()
        }
    }

    @Test
    fun `getFeedSources with no sources returns empty list`() = runTest {
        every { feedDao.getFeedSources() } returns flowOf(emptyList())

        repository.getFeedSources().test {
            assertEquals(emptyList<FeedSource>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getFeedSources maps isEnabled and order correctly`() = runTest {
        val entity = makeFeedSource(isEnabled = false, order = 5)
        every { feedDao.getFeedSources() } returns flowOf(listOf(entity))

        repository.getFeedSources().test {
            val result = awaitItem()
            assertEquals(false, result[0].isEnabled)
            assertEquals(5, result[0].order)
            awaitComplete()
        }
    }

    // ---- addFeedSource ----

    @Test
    fun `addFeedSource inserts entity with correct values`() = runTest {
        coEvery { feedDao.insertFeedSource(any()) } returns 1L

        repository.addFeedSource(sourceId = 100L, sourceName = "MangaDex")

        coVerify {
            feedDao.insertFeedSource(match {
                it.sourceId == 100L && it.sourceName == "MangaDex"
            })
        }
    }

    // ---- removeFeedSource ----

    @Test
    fun `removeFeedSource calls dao with correct sourceId`() = runTest {
        coEvery { feedDao.deleteFeedSourceById(100L) } returns Unit

        repository.removeFeedSource(100L)

        coVerify(exactly = 1) { feedDao.deleteFeedSourceById(100L) }
    }

    // ---- toggleFeedSource ----

    @Test
    fun `toggleFeedSource calls dao with enabled true`() = runTest {
        coEvery { feedDao.setFeedSourceEnabled(1L, true) } returns Unit

        repository.toggleFeedSource(1L, true)

        coVerify { feedDao.setFeedSourceEnabled(1L, true) }
    }

    @Test
    fun `toggleFeedSource calls dao with enabled false`() = runTest {
        coEvery { feedDao.setFeedSourceEnabled(2L, false) } returns Unit

        repository.toggleFeedSource(2L, false)

        coVerify { feedDao.setFeedSourceEnabled(2L, false) }
    }

    // ---- updateFeedSourceOrder ----

    @Test
    fun `updateFeedSourceOrder calls dao with correct args`() = runTest {
        coEvery { feedDao.updateFeedSourceOrder(1L, 3) } returns Unit

        repository.updateFeedSourceOrder(1L, 3)

        coVerify { feedDao.updateFeedSourceOrder(1L, 3) }
    }

    // ---- updateFeedSourceItemCount ----

    @Test
    fun `updateFeedSourceItemCount calls dao with correct args`() = runTest {
        coEvery { feedDao.updateFeedSourceItemCount(1L, 50) } returns Unit

        repository.updateFeedSourceItemCount(1L, 50)

        coVerify { feedDao.updateFeedSourceItemCount(1L, 50) }
    }

    // ---- getFeedItems ----

    @Test
    fun `getFeedItems returns mapped domain models`() = runTest {
        val items = listOf(makeFeedItem(), makeFeedItem(id = 2L, chapterId = 11L))
        every { feedDao.getFeedItems(100) } returns flowOf(items)

        repository.getFeedItems(100).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Naruto", result[0].mangaTitle)
            awaitComplete()
        }
    }

    // ---- getFeedItemsForSource ----

    @Test
    fun `getFeedItemsForSource delegates to dao with sourceId`() = runTest {
        val items = listOf(makeFeedItem(sourceId = 100L))
        every { feedDao.getFeedItemsForSource(100L, 20) } returns flowOf(items)

        repository.getFeedItemsForSource(100L, 20).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            awaitComplete()
        }
    }

    // ---- markFeedItemAsRead ----

    @Test
    fun `markFeedItemAsRead calls dao with correct id`() = runTest {
        coEvery { feedDao.markFeedItemAsRead(5L) } returns Unit

        repository.markFeedItemAsRead(5L)

        coVerify { feedDao.markFeedItemAsRead(5L) }
    }

    // ---- clearFeedHistory ----

    @Test
    fun `clearFeedHistory calls dao clearAllFeedItems`() = runTest {
        coEvery { feedDao.clearAllFeedItems() } returns Unit

        repository.clearFeedHistory()

        coVerify(exactly = 1) { feedDao.clearAllFeedItems() }
    }

    // ---- getSavedSearches ----

    @Test
    fun `getSavedSearches returns mapped domain models`() = runTest {
        val entities = listOf(
            FeedSavedSearchEntity(id = 1L, sourceId = 100L, sourceName = "MangaDex",
                query = "naruto", filtersJson = null, order = 0)
        )
        every { feedDao.getSavedSearches() } returns flowOf(entities)

        repository.getSavedSearches().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("naruto", result[0].query)
            assertEquals(emptyMap<String, String>(), result[0].filters)
            awaitComplete()
        }
    }

    @Test
    fun `getSavedSearches decodes filters from json string`() = runTest {
        val filtersJson = "type\u001Faction\u001Estatus\u001Fongoing"
        val entity = FeedSavedSearchEntity(
            id = 1L, sourceId = 100L, sourceName = "MangaDex",
            query = "naruto", filtersJson = filtersJson, order = 0
        )
        every { feedDao.getSavedSearches() } returns flowOf(listOf(entity))

        repository.getSavedSearches().test {
            val result = awaitItem()
            assertTrue(result[0].filters.isNotEmpty())
            awaitComplete()
        }
    }

    // ---- addSavedSearch ----

    @Test
    fun `addSavedSearch inserts entity with correct values`() = runTest {
        coEvery { feedDao.insertSavedSearch(any()) } returns 1L

        repository.addSavedSearch(
            sourceId = 100L,
            sourceName = "MangaDex",
            query = "bleach",
            filters = emptyMap()
        )

        coVerify {
            feedDao.insertSavedSearch(match {
                it.sourceId == 100L && it.query == "bleach" && it.filtersJson == null
            })
        }
    }

    @Test
    fun `addSavedSearch encodes filters into string`() = runTest {
        coEvery { feedDao.insertSavedSearch(any()) } returns 1L

        repository.addSavedSearch(
            sourceId = 100L,
            sourceName = "MangaDex",
            query = "test",
            filters = mapOf("type" to "manga")
        )

        coVerify {
            feedDao.insertSavedSearch(match { it.filtersJson != null })
        }
    }

    // ---- removeSavedSearch ----

    @Test
    fun `removeSavedSearch calls dao with correct id`() = runTest {
        coEvery { feedDao.deleteSavedSearchById(3L) } returns Unit

        repository.removeSavedSearch(3L)

        coVerify { feedDao.deleteSavedSearchById(3L) }
    }

    // ---- updateSavedSearchOrder ----

    @Test
    fun `updateSavedSearchOrder calls dao with correct args`() = runTest {
        coEvery { feedDao.updateSavedSearchOrder(2L, 4) } returns Unit

        repository.updateSavedSearchOrder(2L, 4)

        coVerify { feedDao.updateSavedSearchOrder(2L, 4) }
    }

    // ---- refreshFeed (no-op) ----

    @Test
    fun `refreshFeed is a no-op and does not throw`() = runTest {
        // refreshFeed is a no-op — actual refresh is done by FeedRefreshWorker
        repository.refreshFeed()
    }
}
