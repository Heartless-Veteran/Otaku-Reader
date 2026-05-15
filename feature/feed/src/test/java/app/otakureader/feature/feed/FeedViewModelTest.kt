package app.otakureader.feature.feed

import app.cash.turbine.test
import app.otakureader.domain.model.FeedItem
import app.otakureader.domain.model.FeedSource
import app.otakureader.domain.repository.FeedRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var feedRepository: FeedRepository

    private val sampleFeedItems = listOf(
        FeedItem(
            id = 1L, mangaId = 10L, mangaTitle = "Naruto", mangaThumbnailUrl = null,
            chapterId = 100L, chapterName = "Chapter 1", chapterNumber = 1f,
            sourceId = 1L, sourceName = "MangaDex", timestamp = Instant.EPOCH
        ),
        FeedItem(
            id = 2L, mangaId = 20L, mangaTitle = "Bleach", mangaThumbnailUrl = null,
            chapterId = 200L, chapterName = "Chapter 1", chapterNumber = 1f,
            sourceId = 1L, sourceName = "MangaDex", timestamp = Instant.EPOCH
        )
    )
    private val sampleSources = listOf(
        FeedSource(sourceId = 1L, sourceName = "MangaDex", isEnabled = true)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        feedRepository = mockk {
            every { getFeedItems(any()) } returns flowOf(emptyList())
            every { getFeedSources() } returns flowOf(emptyList())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = FeedViewModel(feedRepository)

    @Test
    fun init_stateStartsWithLoadingTrue() {
        val viewModel = createViewModel()
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun state_withFeedItems_populatesItems() = runTest {
        every { feedRepository.getFeedItems(any()) } returns flowOf(sampleFeedItems)
        every { feedRepository.getFeedSources() } returns flowOf(sampleSources)

        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // initialValue
            val loaded = awaitItem()
            assertEquals(2, loaded.feedItems.size)
            assertEquals(1, loaded.feedSources.size)
            assertFalse(loaded.isLoading)
            assertNull(loaded.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_withEmptyRepository_emitsEmptyState() = runTest {
        every { feedRepository.getFeedItems(any()) } returns flowOf(emptyList())
        every { feedRepository.getFeedSources() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // initialValue
            val empty = awaitItem()
            assertTrue(empty.feedItems.isEmpty())
            assertTrue(empty.feedSources.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onEvent_Refresh_callsRefreshFeed() = runTest {
        coEvery { feedRepository.refreshFeed() } returns Unit
        val viewModel = createViewModel()

        viewModel.onEvent(FeedEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { feedRepository.refreshFeed() }
    }

    @Test
    fun onEvent_Refresh_onError_setsErrorInState() = runTest {
        coEvery { feedRepository.refreshFeed() } throws RuntimeException("Network error")

        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // initialValue
            awaitItem() // loading = false after combine settles

            viewModel.onEvent(FeedEvent.Refresh)
            testDispatcher.scheduler.advanceUntilIdle()

            // Expect loading=true, then error state
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val error = awaitItem()
            assertFalse(error.isLoading)
            assertNotNull(error.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onEvent_OnFeedItemClick_emitsNavigateToReaderEffect() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(FeedEvent.OnFeedItemClick(mangaId = 10L, chapterId = 100L))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is FeedEffect.NavigateToReader)
            assertEquals(10L, (effect as FeedEffect.NavigateToReader).mangaId)
            assertEquals(100L, effect.chapterId)
        }
    }

    @Test
    fun onEvent_OnMarkAsRead_callsMarkFeedItemAsRead() = runTest {
        coEvery { feedRepository.markFeedItemAsRead(any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.onEvent(FeedEvent.OnMarkAsRead(feedItemId = 1L))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { feedRepository.markFeedItemAsRead(1L) }
    }

    @Test
    fun onEvent_OnMarkAsRead_onError_emitsShowSnackbarEffect() = runTest {
        coEvery { feedRepository.markFeedItemAsRead(any()) } throws RuntimeException("DB error")
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(FeedEvent.OnMarkAsRead(feedItemId = 1L))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is FeedEffect.ShowSnackbar)
        }
    }

    @Test
    fun onEvent_OnToggleSource_callsToggleFeedSource() = runTest {
        coEvery { feedRepository.toggleFeedSource(any(), any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.onEvent(FeedEvent.OnToggleSource(sourceId = 1L, enabled = false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { feedRepository.toggleFeedSource(1L, false) }
    }

    @Test
    fun onEvent_OnToggleSource_onError_emitsShowSnackbarEffect() = runTest {
        coEvery { feedRepository.toggleFeedSource(any(), any()) } throws RuntimeException("error")
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(FeedEvent.OnToggleSource(sourceId = 1L, enabled = false))
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(awaitItem() is FeedEffect.ShowSnackbar)
        }
    }

    @Test
    fun onEvent_ClearHistory_callsClearFeedHistory() = runTest {
        coEvery { feedRepository.clearFeedHistory() } returns Unit
        val viewModel = createViewModel()

        viewModel.onEvent(FeedEvent.ClearHistory)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { feedRepository.clearFeedHistory() }
    }

    @Test
    fun onEvent_ClearHistory_onError_emitsShowSnackbarEffect() = runTest {
        coEvery { feedRepository.clearFeedHistory() } throws RuntimeException("fail")
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(FeedEvent.ClearHistory)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(awaitItem() is FeedEffect.ShowSnackbar)
        }
    }
}
