package app.otakureader.feature.history

import app.otakureader.domain.model.Chapter
import app.otakureader.domain.model.ChapterWithHistory
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.usecase.GetHistoryUseCase
import app.cash.turbine.test
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

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getHistoryUseCase: GetHistoryUseCase
    private lateinit var chapterRepository: ChapterRepository

    private fun chapter(id: Long, name: String = "Chapter $id") =
        Chapter(id = id, mangaId = 1L, url = "/c/$id", name = name, read = true)

    private fun historyEntry(chapterId: Long, name: String = "Chapter $chapterId") =
        ChapterWithHistory(chapter = chapter(chapterId, name), readAt = chapterId * 1000L)

    private val sampleHistory = listOf(
        historyEntry(1L, "Chapter 1"),
        historyEntry(2L, "Chapter 2"),
        historyEntry(3L, "Dragon Slayer Arc")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getHistoryUseCase = mockk()
        chapterRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(getHistoryUseCase, chapterRepository)
    }

    @Test
    fun init_loadsHistoryOnCreation() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.state.value.history.size)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun init_withEmptyHistory_emitsEmptyList() = runTest {
        every { getHistoryUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<ChapterWithHistory>(), viewModel.state.value.history)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun onEvent_OnSearchQueryChange_filtersHistory() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.OnSearchQueryChange("Dragon"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.history.size)
        assertEquals("Dragon Slayer Arc", viewModel.state.value.history[0].chapter.name)
    }

    @Test
    fun onEvent_OnSearchQueryChange_withBlankQuery_showsAll() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.OnSearchQueryChange("Dragon"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.history.size)

        viewModel.onEvent(HistoryEvent.OnSearchQueryChange(""))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, viewModel.state.value.history.size)
    }

    @Test
    fun onEvent_OnSearchQueryChange_updatesSearchQueryState() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.OnSearchQueryChange("chapter"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("chapter", viewModel.state.value.searchQuery)
    }

    @Test
    fun onEvent_OnChapterLongClick_addsToSelection() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.OnChapterLongClick(chapterId = 1L))

        assertTrue(viewModel.state.value.selectedItems.contains(1L))
    }

    @Test
    fun onEvent_OnChapterLongClick_togglesSelection() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // First long click - adds
        viewModel.onEvent(HistoryEvent.OnChapterLongClick(chapterId = 2L))
        assertTrue(viewModel.state.value.selectedItems.contains(2L))

        // Second long click - removes
        viewModel.onEvent(HistoryEvent.OnChapterLongClick(chapterId = 2L))
        assertFalse(viewModel.state.value.selectedItems.contains(2L))
    }

    @Test
    fun onEvent_ClearSelection_removesAllSelectedItems() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.OnChapterLongClick(1L))
        viewModel.onEvent(HistoryEvent.OnChapterLongClick(2L))
        assertEquals(2, viewModel.state.value.selectedItems.size)

        viewModel.onEvent(HistoryEvent.ClearSelection)
        assertTrue(viewModel.state.value.selectedItems.isEmpty())
    }

    @Test
    fun onEvent_SelectAll_selectsAllHistoryItems() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.SelectAll)

        assertEquals(3, viewModel.state.value.selectedItems.size)
        assertTrue(viewModel.state.value.selectedItems.containsAll(listOf(1L, 2L, 3L)))
    }

    @Test
    fun onEvent_OnChapterClick_withNoSelection_emitsNavigateEffect() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(HistoryEvent.OnChapterClick(mangaId = 1L, chapterId = 1L))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HistoryEffect.NavigateToReader)
            assertEquals(1L, (effect as HistoryEffect.NavigateToReader).mangaId)
            assertEquals(1L, effect.chapterId)
        }
    }

    @Test
    fun onEvent_OnChapterClick_withSelectionActive_togglesSelection() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Activate selection mode
        viewModel.onEvent(HistoryEvent.OnChapterLongClick(2L))
        assertTrue(viewModel.state.value.selectedItems.isNotEmpty())

        // Click in selection mode should toggle selection, not navigate
        viewModel.onEvent(HistoryEvent.OnChapterClick(mangaId = 1L, chapterId = 1L))
        assertTrue(viewModel.state.value.selectedItems.contains(1L))
    }

    @Test
    fun onEvent_ClearHistory_invokesRepositoryAndShowsSnackbar() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)
        coEvery { chapterRepository.clearAllHistory() } returns Unit

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(HistoryEvent.ClearHistory)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HistoryEffect.ShowSnackbar)
            assertEquals(R.string.history_cleared, (effect as HistoryEffect.ShowSnackbar).messageRes)
        }

        coVerify(exactly = 1) { chapterRepository.clearAllHistory() }
    }

    @Test
    fun onEvent_ClearHistory_withError_showsErrorSnackbar() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)
        coEvery { chapterRepository.clearAllHistory() } throws RuntimeException("DB error")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(HistoryEvent.ClearHistory)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HistoryEffect.ShowSnackbar)
            assertEquals(R.string.history_clear_failed, (effect as HistoryEffect.ShowSnackbar).messageRes)
        }
    }

    @Test
    fun onEvent_RemoveFromHistory_hidesItemAndEmitsUndoEffect() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, viewModel.state.value.history.size)

        viewModel.effect.test {
            viewModel.onEvent(HistoryEvent.RemoveFromHistory(chapterId = 1L))
            // runCurrent() processes tasks at t=0 (combine re-emit + effect send)
            // without advancing through delay(UNDO_TIMEOUT_MS) at t=4000.
            testDispatcher.scheduler.runCurrent()

            // Chapter is hidden from the list while the undo timer is running
            assertEquals(2, viewModel.state.value.history.size)

            val effect = awaitItem()
            assertTrue(effect is HistoryEffect.ShowUndoSnackbar)
            assertEquals(1L, (effect as HistoryEffect.ShowUndoSnackbar).chapterId)

            cancelAndIgnoreRemainingEvents()
        }
        // Repo must NOT be called yet — deletion auto-commits after UNDO_TIMEOUT_MS
        coVerify(exactly = 0) { chapterRepository.removeFromHistory(any()) }
    }

    @Test
    fun onEvent_RemoveFromHistory_autoCommitsAfterTimeout() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)
        coEvery { chapterRepository.removeFromHistory(any()) } returns Unit

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(HistoryEvent.RemoveFromHistory(chapterId = 1L))
            testDispatcher.scheduler.runCurrent()  // send effect, suspend at delay
            awaitItem() // consume ShowUndoSnackbar

            // Advance the virtual clock past the auto-commit delay
            testDispatcher.scheduler.advanceTimeBy(HistoryViewModel.UNDO_TIMEOUT_MS + 1)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { chapterRepository.removeFromHistory(1L) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onEvent_UndoRemoveFromHistory_clearsPendingWithoutDeletion() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(HistoryEvent.RemoveFromHistory(chapterId = 2L))
            testDispatcher.scheduler.runCurrent()  // send effect, suspend at delay
            awaitItem() // consume ShowUndoSnackbar

            // Undo cancels the pending job; the delay coroutine is now cancelled.
            viewModel.onEvent(HistoryEvent.UndoRemoveFromHistory(chapterId = 2L))

            // Advance past where auto-commit would have fired — nothing happens.
            testDispatcher.scheduler.advanceTimeBy(HistoryViewModel.UNDO_TIMEOUT_MS + 1)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { chapterRepository.removeFromHistory(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onEvent_MarkSelectedAsRead_callsRepositoryAndClearsSelection() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)
        coEvery { chapterRepository.updateChapterProgress(any<Collection<Long>>(), any(), any()) } returns Unit

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.OnChapterLongClick(1L))
        viewModel.onEvent(HistoryEvent.OnChapterLongClick(2L))
        assertEquals(2, viewModel.state.value.selectedItems.size)

        viewModel.effect.test {
            viewModel.onEvent(HistoryEvent.MarkSelectedAsRead)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HistoryEffect.ShowSnackbar)
            assertTrue((effect as HistoryEffect.ShowSnackbar).formatArgs.contains(2))
        }

        assertTrue(viewModel.state.value.selectedItems.isEmpty())
        coVerify(exactly = 1) { chapterRepository.updateChapterProgress(match<Collection<Long>> { it.containsAll(listOf(1L, 2L)) }, true, 0) }
    }

    @Test
    fun onEvent_RemoveSelectedFromHistory_removesAllSelectedAndClearsSelection() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)
        coEvery { chapterRepository.removeFromHistory(any()) } returns Unit

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.OnChapterLongClick(1L))
        viewModel.onEvent(HistoryEvent.OnChapterLongClick(2L))
        assertEquals(2, viewModel.state.value.selectedItems.size)

        viewModel.effect.test {
            viewModel.onEvent(HistoryEvent.RemoveSelectedFromHistory)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HistoryEffect.ShowSnackbar)
            assertTrue((effect as HistoryEffect.ShowSnackbar).formatArgs.contains(2))
        }

        assertTrue(viewModel.state.value.selectedItems.isEmpty())
        coVerify(exactly = 1) { chapterRepository.removeFromHistory(1L) }
        coVerify(exactly = 1) { chapterRepository.removeFromHistory(2L) }
    }

    @Test
    fun onEvent_RemoveSelectedFromHistory_withNoSelection_doesNothing() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // No items selected - should not call repository
        viewModel.onEvent(HistoryEvent.RemoveSelectedFromHistory)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { chapterRepository.removeFromHistory(any()) }
    }

    @Test
    fun onEvent_SetDateFilter_updatesDateFilterState() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.SetDateFilter(start = 1_000L, end = 3_000L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1_000L, viewModel.state.value.dateFilterStart)
        assertEquals(3_000L, viewModel.state.value.dateFilterEnd)
    }

    @Test
    fun onEvent_ClearDateFilter_removesDateFilterState() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.SetDateFilter(start = 1_000L, end = 3_000L))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(HistoryEvent.ClearDateFilter)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.dateFilterStart)
        assertNull(viewModel.state.value.dateFilterEnd)
    }

    @Test
    fun onEvent_RefreshHistory_setsPullRefreshingThenClearsIt() = runTest {
        every { getHistoryUseCase() } returns flowOf(sampleHistory)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.RefreshHistory)
        assertTrue(viewModel.state.value.isPullRefreshing)

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isPullRefreshing)
    }
}
