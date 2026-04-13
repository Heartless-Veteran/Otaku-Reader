package app.otakureader.feature.browse

import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.domain.usecase.source.GetSourcesUseCase
import app.otakureader.domain.usecase.source.GlobalSearchUseCase
import app.otakureader.sourceapi.MangaPage
import app.otakureader.sourceapi.MangaSource
import app.otakureader.sourceapi.SourceManga
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getSourcesUseCase: GetSourcesUseCase
    private lateinit var globalSearchUseCase: GlobalSearchUseCase
    private lateinit var generalPreferences: GeneralPreferences

    private val source1 = mockk<MangaSource>(relaxed = true) {
        every { id } returns "source1"
        every { name } returns "Source One"
        every { lang } returns "en"
        every { isNsfw } returns false
    }
    private val source2 = mockk<MangaSource>(relaxed = true) {
        every { id } returns "source2"
        every { name } returns "Source Two"
        every { lang } returns "en"
        every { isNsfw } returns false
    }
    private val nsfwSource = mockk<MangaSource>(relaxed = true) {
        every { id } returns "nsfw_source"
        every { name } returns "NSFW Source"
        every { lang } returns "en"
        every { isNsfw } returns true
    }

    private val manga1 = SourceManga(url = "/m/1", title = "Naruto")
    private val manga2 = SourceManga(url = "/m/2", title = "Naruto Shippuden")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getSourcesUseCase = mockk()
        globalSearchUseCase = mockk()
        generalPreferences = mockk {
            every { showNsfwContent } returns flowOf(false)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = GlobalSearchViewModel(
        getSourcesUseCase = getSourcesUseCase,
        globalSearchUseCase = globalSearchUseCase,
        generalPreferences = generalPreferences
    )

    /**
     * GlobalSearchViewModel.state uses SharingStarted.WhileSubscribed(5_000). Subscribe before
     * advancing the dispatcher so the upstream state updates are reflected in state.value.
     */
    private fun TestScope.activateState(viewModel: GlobalSearchViewModel): Job {
        return launch { viewModel.state.collect() }
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun init_startsWithEmptyState() {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        // Initial value before subscribing
        assertEquals("", viewModel.state.value.query)
        assertFalse(viewModel.state.value.isSearching)
        assertTrue(viewModel.state.value.sourceResults.isEmpty())
    }

    // ── initQuery ─────────────────────────────────────────────────────────────

    @Test
    fun initQuery_withNonBlankQuery_setsQueryAndSearches() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { globalSearchUseCase("source1", "Naruto") } returns Result.success(
            MangaPage(mangas = listOf(manga1), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.initQuery("Naruto")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Naruto", viewModel.state.value.query)
        assertEquals(1, viewModel.state.value.sourceResults.size)
        assertEquals(1, viewModel.state.value.sourceResults.first().results.size)
        collectJob.cancel()
    }

    @Test
    fun initQuery_withBlankQuery_doesNothing() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.initQuery("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.state.value.query)
        assertTrue(viewModel.state.value.sourceResults.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun initQuery_calledTwice_onlySearchesOnce() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { globalSearchUseCase("source1", "Naruto") } returns Result.success(
            MangaPage(mangas = listOf(manga1), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.initQuery("Naruto")
        viewModel.initQuery("Bleach") // Second call should be ignored because query is already set
        testDispatcher.scheduler.advanceUntilIdle()

        // Query stays as "Naruto" — second initQuery is ignored
        assertEquals("Naruto", viewModel.state.value.query)
        collectJob.cancel()
    }

    // ── OnQueryChange ─────────────────────────────────────────────────────────

    @Test
    fun onEvent_OnQueryChange_updatesQuery() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(GlobalSearchEvent.OnQueryChange("One Piece"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("One Piece", viewModel.state.value.query)
        collectJob.cancel()
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    fun onEvent_Search_withBlankQuery_doesNotSearch() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(GlobalSearchEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.sourceResults.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun onEvent_Search_withQuery_initializesLoadingStatesPerSource() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1, source2))
        // Make searches take long so we can check loading state
        coEvery { globalSearchUseCase(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1_000L)
            Result.success(MangaPage(mangas = emptyList(), hasNextPage = false))
        }

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(GlobalSearchEvent.OnQueryChange("Naruto"))
        viewModel.onEvent(GlobalSearchEvent.Search)
        testDispatcher.scheduler.advanceTimeBy(50) // not yet done

        assertTrue(viewModel.state.value.isSearching)
        assertEquals(2, viewModel.state.value.sourceResults.size)
        assertTrue(viewModel.state.value.sourceResults.all { it.isLoading })
        collectJob.cancel()
    }

    @Test
    fun onEvent_Search_successfulResults_populatesSourceResults() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { globalSearchUseCase("source1", "Naruto") } returns Result.success(
            MangaPage(mangas = listOf(manga1, manga2), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(GlobalSearchEvent.OnQueryChange("Naruto"))
        viewModel.onEvent(GlobalSearchEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.state.value.sourceResults.first()
        assertEquals("source1", result.sourceId)
        assertEquals(2, result.results.size)
        assertFalse(result.isLoading)
        collectJob.cancel()
    }

    @Test
    fun onEvent_Search_failedSource_capturesErrorPerSource() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1, source2))
        coEvery { globalSearchUseCase("source1", "Naruto") } returns Result.failure(
            RuntimeException("Source1 error")
        )
        coEvery { globalSearchUseCase("source2", "Naruto") } returns Result.success(
            MangaPage(mangas = listOf(manga1), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(GlobalSearchEvent.OnQueryChange("Naruto"))
        viewModel.onEvent(GlobalSearchEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        val source1Result = viewModel.state.value.sourceResults.first { it.sourceId == "source1" }
        val source2Result = viewModel.state.value.sourceResults.first { it.sourceId == "source2" }

        assertEquals("Source1 error", source1Result.error)
        assertFalse(source1Result.isLoading)
        assertEquals(1, source2Result.results.size)
        assertFalse(source2Result.isLoading)
        collectJob.cancel()
    }

    @Test
    fun onEvent_Search_withNoSources_emitsEmptyResultsAndIsNotSearching() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(GlobalSearchEvent.OnQueryChange("Naruto"))
        viewModel.onEvent(GlobalSearchEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isSearching)
        assertTrue(viewModel.state.value.sourceResults.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun onEvent_Search_withNsfwHidden_filtersNsfwSources() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1, nsfwSource))
        every { generalPreferences.showNsfwContent } returns flowOf(false)
        coEvery { globalSearchUseCase("source1", "Naruto") } returns Result.success(
            MangaPage(mangas = listOf(manga1), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(GlobalSearchEvent.OnQueryChange("Naruto"))
        viewModel.onEvent(GlobalSearchEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.sourceResults.size)
        assertEquals("source1", viewModel.state.value.sourceResults.first().sourceId)
        collectJob.cancel()
    }

    @Test
    fun onEvent_Search_cancelsPreviousSearch_onNewSearch() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { globalSearchUseCase("source1", "old") } coAnswers {
            kotlinx.coroutines.delay(2_000L)
            Result.success(MangaPage(mangas = listOf(manga1), hasNextPage = false))
        }
        coEvery { globalSearchUseCase("source1", "new") } returns Result.success(
            MangaPage(mangas = listOf(manga2), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(GlobalSearchEvent.OnQueryChange("old"))
        viewModel.onEvent(GlobalSearchEvent.Search)
        testDispatcher.scheduler.advanceTimeBy(100) // in-flight

        // Start new search which should cancel old one
        viewModel.onEvent(GlobalSearchEvent.OnQueryChange("new"))
        viewModel.onEvent(GlobalSearchEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        // The "new" results should be present, not "old"
        val result = viewModel.state.value.sourceResults.first { it.sourceId == "source1" }
        assertTrue(result.results.any { it.title == "Naruto Shippuden" })
        collectJob.cancel()
    }

    // ── OnMangaClick ──────────────────────────────────────────────────────────

    @Test
    fun onEvent_OnMangaClick_emitsNavigateEffect() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(GlobalSearchEvent.OnMangaClick(sourceId = "source1", manga = manga1))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is GlobalSearchEffect.NavigateToMangaDetail)
            val navEffect = effect as GlobalSearchEffect.NavigateToMangaDetail
            assertEquals("source1", navEffect.sourceId)
            assertEquals("/m/1", navEffect.mangaUrl)
        }
    }

    // ── isSearching flag ──────────────────────────────────────────────────────

    @Test
    fun state_isSearchingFalse_afterAllSourcesRespond() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1, source2))
        coEvery { globalSearchUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = emptyList(), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(GlobalSearchEvent.OnQueryChange("Naruto"))
        viewModel.onEvent(GlobalSearchEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isSearching)
        collectJob.cancel()
    }
}
