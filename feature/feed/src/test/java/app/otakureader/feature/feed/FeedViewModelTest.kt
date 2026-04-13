package app.otakureader.feature.feed

import app.otakureader.domain.model.FeedItem
import app.otakureader.domain.model.FeedSource
import app.otakureader.domain.repository.FeedRepository
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

    /**
     * FeedViewModel.state uses SharingStarted.WhileSubscribed(5_000), meaning the upstream
     * combine only runs while there is an active subscriber. Call [activateState] once per test
     * to subscribe, then cancel the returned job after assertions.
     */
    private fun TestScope.activateState(viewModel: FeedViewModel): Job {
        return launch { viewModel.state.collect() }
    }

    private val sampleFeedItems = listOf(
        FeedItem(
            id = 1L,
            mangaId = 10L,
            mangaTitle = "Naruto",
            mangaThumbnailUrl = null,
            chapterId = 100L,
            chapterName = "Chapter 1",
            chapterNumber = 1f,
            sourceId = 1L,
            sourceName = "MangaDex",
            timestamp = Instant.ofEpochMilli(1_000_000L),
            isRead = false
        ),
        FeedItem(
            id = 2L,
            mangaId = 20L,
            mangaTitle = "Bleach",
            mangaThumbnailUrl = null,
            chapterId = 200L,
            chapterName = "Chapter 50",
            chapterNumber = 50f,
            sourceId = 2L,
            sourceName = "MangaPlus",
            timestamp = Instant.ofEpochMilli(2_000_000L),
            isRead = true
        )
    )

    private val sampleFeedSources = listOf(
        FeedSource(sourceId = 1L, sourceName = "MangaDex", isEnabled = true),
        FeedSource(sourceId = 2L, sourceName = "MangaPlus", isEnabled = false)
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

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun init_emitsLoadingStateInitially() {
        val viewModel = createViewModel()
        // The initial value is FeedState(isLoading = true)
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun init_loadsFeedItemsAndSources() = runTest {
        every { feedRepository.getFeedItems(any()) } returns flowOf(sampleFeedItems)
        every { feedRepository.getFeedSources() } returns flowOf(sampleFeedSources)

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.feedItems.size)
        assertEquals(2, viewModel.state.value.feedSources.size)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
        collectJob.cancel()
    }

    @Test
    fun init_withEmptyRepo_emitsEmptyState() = runTest {
        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.feedItems.isEmpty())
        assertTrue(viewModel.state.value.feedSources.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
        collectJob.cancel()
    }

    @Test
    fun init_withFlowError_emitsErrorState() = runTest {
        every { feedRepository.getFeedItems(any()) } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("DB error")
        }
        every { feedRepository.getFeedSources() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertEquals("DB error", viewModel.state.value.error)
        collectJob.cancel()
    }

    @Test
    fun init_stateReactiveTo_repoUpdates() = runTest {
        val itemsFlow = MutableStateFlow(emptyList<FeedItem>())
        every { feedRepository.getFeedItems(any()) } returns itemsFlow
        every { feedRepository.getFeedSources() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.feedItems.isEmpty())

        itemsFlow.value = sampleFeedItems
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.feedItems.size)
        collectJob.cancel()
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun onEvent_Refresh_callsRepositoryRefresh() = runTest {
        coEvery { feedRepository.refreshFeed() } returns Unit

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(FeedEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { feedRepository.refreshFeed() }
        collectJob.cancel()
    }

    @Test
    fun onEvent_Refresh_clearsError() = runTest {
        // First call throws, second call succeeds
        var callCount = 0
        coEvery { feedRepository.refreshFeed() } answers {
            callCount++
            if (callCount == 1) throw RuntimeException("Network error") else Unit
        }

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)

        viewModel.onEvent(FeedEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.onEvent(FeedEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.state.value.error)
        collectJob.cancel()
    }

    @Test
    fun onEvent_Refresh_onException_setsError() = runTest {
        coEvery { feedRepository.refreshFeed() } throws RuntimeException("Network error")

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(FeedEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Network error", viewModel.state.value.error)
        collectJob.cancel()
    }

    @Test
    fun onEvent_Refresh_setsLoadingTrueAndThenFalse() = runTest {
        coEvery { feedRepository.refreshFeed() } returns Unit

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle() // settle initial state
        assertFalse(viewModel.state.value.isLoading)

        // After completing refresh, isLoading should be false
        viewModel.onEvent(FeedEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        collectJob.cancel()
    }

    // ── OnFeedItemClick ───────────────────────────────────────────────────────

    @Test
    fun onEvent_OnFeedItemClick_emitsNavigateEffect() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(FeedEvent.OnFeedItemClick(mangaId = 10L, chapterId = 100L))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is FeedEffect.NavigateToReader)
            val navEffect = effect as FeedEffect.NavigateToReader
            assertEquals(10L, navEffect.mangaId)
            assertEquals(100L, navEffect.chapterId)
        }
    }

    // ── OnMarkAsRead ──────────────────────────────────────────────────────────

    @Test
    fun onEvent_OnMarkAsRead_callsRepository() = runTest {
        coEvery { feedRepository.markFeedItemAsRead(any()) } returns Unit

        val viewModel = createViewModel()
        viewModel.onEvent(FeedEvent.OnMarkAsRead(feedItemId = 1L))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { feedRepository.markFeedItemAsRead(1L) }
    }

    @Test
    fun onEvent_OnMarkAsRead_onException_emitsSnackbarEffect() = runTest {
        coEvery { feedRepository.markFeedItemAsRead(any()) } throws RuntimeException("Failed to mark")

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(FeedEvent.OnMarkAsRead(feedItemId = 1L))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is FeedEffect.ShowSnackbar)
        }
    }

    // ── OnToggleSource ────────────────────────────────────────────────────────

    @Test
    fun onEvent_OnToggleSource_callsRepository() = runTest {
        coEvery { feedRepository.toggleFeedSource(any(), any()) } returns Unit

        val viewModel = createViewModel()
        viewModel.onEvent(FeedEvent.OnToggleSource(sourceId = 1L, enabled = false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { feedRepository.toggleFeedSource(1L, false) }
    }

    @Test
    fun onEvent_OnToggleSource_onException_emitsSnackbarEffect() = runTest {
        coEvery { feedRepository.toggleFeedSource(any(), any()) } throws RuntimeException("Update failed")

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(FeedEvent.OnToggleSource(sourceId = 1L, enabled = true))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is FeedEffect.ShowSnackbar)
            assertEquals("Update failed", (effect as FeedEffect.ShowSnackbar).message)
        }
    }

    // ── ClearHistory ──────────────────────────────────────────────────────────

    @Test
    fun onEvent_ClearHistory_callsRepository() = runTest {
        coEvery { feedRepository.clearFeedHistory() } returns Unit

        val viewModel = createViewModel()
        viewModel.onEvent(FeedEvent.ClearHistory)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { feedRepository.clearFeedHistory() }
    }

    @Test
    fun onEvent_ClearHistory_onException_emitsSnackbarEffect() = runTest {
        coEvery { feedRepository.clearFeedHistory() } throws RuntimeException("Clear failed")

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(FeedEvent.ClearHistory)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is FeedEffect.ShowSnackbar)
            assertEquals("Clear failed", (effect as FeedEffect.ShowSnackbar).message)
        }
    }

    // ── Feed items limit ──────────────────────────────────────────────────────

    @Test
    fun init_requestsFeedItemsWithLimit100() = runTest {
        every { feedRepository.getFeedItems(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.verify { feedRepository.getFeedItems(100) }
        collectJob.cancel()
    }
}
