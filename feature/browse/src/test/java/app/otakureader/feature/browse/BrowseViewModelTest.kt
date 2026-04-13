package app.otakureader.feature.browse

import app.otakureader.core.preferences.AiPreferences
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.domain.model.SourceScore
import app.otakureader.domain.usecase.ai.ScoreSourcesForMangaUseCase
import app.otakureader.domain.usecase.ai.SourceInfo
import app.otakureader.domain.usecase.source.GetLatestUpdatesUseCase
import app.otakureader.domain.usecase.source.GetPopularMangaUseCase
import app.otakureader.domain.usecase.source.GetSourceFiltersUseCase
import app.otakureader.domain.usecase.source.GetSourcesUseCase
import app.otakureader.domain.usecase.source.SearchMangaUseCase
import app.otakureader.sourceapi.FilterList
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getSourcesUseCase: GetSourcesUseCase
    private lateinit var getPopularMangaUseCase: GetPopularMangaUseCase
    private lateinit var getLatestUpdatesUseCase: GetLatestUpdatesUseCase
    private lateinit var searchMangaUseCase: SearchMangaUseCase
    private lateinit var getSourceFiltersUseCase: GetSourceFiltersUseCase
    private lateinit var generalPreferences: GeneralPreferences
    private lateinit var aiPreferences: AiPreferences
    private lateinit var scoreSourcesForManga: ScoreSourcesForMangaUseCase

    private val source1 = mockk<MangaSource>(relaxed = true) {
        every { id } returns "source1"
        every { name } returns "Source One"
        every { lang } returns "en"
        every { isNsfw } returns false
    }

    private val nsfwSource = mockk<MangaSource>(relaxed = true) {
        every { id } returns "nsfw_source"
        every { name } returns "NSFW Source"
        every { lang } returns "en"
        every { isNsfw } returns true
    }

    private val sampleManga1 = SourceManga(url = "/m/1", title = "Naruto")
    private val sampleManga2 = SourceManga(url = "/m/2", title = "Bleach")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getSourcesUseCase = mockk()
        getPopularMangaUseCase = mockk()
        getLatestUpdatesUseCase = mockk()
        searchMangaUseCase = mockk()
        getSourceFiltersUseCase = mockk()
        generalPreferences = mockk {
            every { showNsfwContent } returns flowOf(false)
        }
        aiPreferences = mockk {
            every { aiEnabled } returns flowOf(false)
            every { aiSourceIntelligence } returns flowOf(false)
        }
        scoreSourcesForManga = mockk()
        coEvery { getSourceFiltersUseCase.invoke(any()) } returns FilterList()
        coEvery { scoreSourcesForManga.invoke(any(), any(), any()) } returns Result.success(emptyList<SourceScore>())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = BrowseViewModel(
        getSourcesUseCase = getSourcesUseCase,
        getPopularMangaUseCase = getPopularMangaUseCase,
        getLatestUpdatesUseCase = getLatestUpdatesUseCase,
        searchMangaUseCase = searchMangaUseCase,
        getSourceFiltersUseCase = getSourceFiltersUseCase,
        generalPreferences = generalPreferences,
        aiPreferences = aiPreferences,
        scoreSourcesForManga = scoreSourcesForManga
    )

    /**
     * BrowseViewModel.state uses SharingStarted.WhileSubscribed(5_000), meaning the upstream
     * only runs while there is an active subscriber. Subscribe before advancing the dispatcher.
     */
    private fun TestScope.activateState(viewModel: BrowseViewModel): Job {
        return launch { viewModel.state.collect() }
    }

    // ── Source loading ────────────────────────────────────────────────────────

    @Test
    fun init_loadsSources_andStoresTheirIds() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("source1"), viewModel.state.value.sources)
        collectJob.cancel()
    }

    @Test
    fun init_withNsfwHidden_filtersNsfwSources() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1, nsfwSource))
        every { generalPreferences.showNsfwContent } returns flowOf(false)

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("source1"), viewModel.state.value.sources)
        assertFalse(viewModel.state.value.sources.contains("nsfw_source"))
        collectJob.cancel()
    }

    @Test
    fun init_withNsfwEnabled_includesNsfwSources() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1, nsfwSource))
        every { generalPreferences.showNsfwContent } returns flowOf(true)

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.sources.size)
        assertTrue(viewModel.state.value.sources.contains("nsfw_source"))
        collectJob.cancel()
    }

    // ── SelectSource ──────────────────────────────────────────────────────────

    @Test
    fun onEvent_SelectSource_updatesCurrentSourceId() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = listOf(sampleManga1), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("source1", viewModel.state.value.currentSourceId)
        collectJob.cancel()
    }

    @Test
    fun onEvent_SelectSource_loadsPopularManga() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase("source1", 1) } returns Result.success(
            MangaPage(mangas = listOf(sampleManga1, sampleManga2), hasNextPage = true)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.popularManga.size)
        assertTrue(viewModel.state.value.hasNextPage)
        collectJob.cancel()
    }

    @Test
    fun onEvent_SelectSource_onFailure_setsError() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.failure(
            RuntimeException("Network error")
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Network error", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
        collectJob.cancel()
    }

    @Test
    fun onEvent_SelectSource_resetsFilters() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = emptyList(), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.activeFilters.filters.isEmpty())
        assertTrue(viewModel.state.value.availableFilters.filters.isEmpty())
        collectJob.cancel()
    }

    // ── OnSearchQueryChange ───────────────────────────────────────────────────

    @Test
    fun onEvent_OnSearchQueryChange_updatesSearchQuery() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.OnSearchQueryChange("One Piece"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("One Piece", viewModel.state.value.searchQuery)
        collectJob.cancel()
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    fun onEvent_Search_withNoSourceSelected_doesNothing() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.OnSearchQueryChange("Naruto"))
        viewModel.onEvent(BrowseEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.searchResults.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun onEvent_Search_withSourceSelected_loadsResults() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = emptyList(), hasNextPage = false)
        )
        coEvery { searchMangaUseCase("source1", "Naruto", 1, any()) } returns Result.success(
            MangaPage(mangas = listOf(sampleManga1), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.OnSearchQueryChange("Naruto"))
        viewModel.onEvent(BrowseEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.searchResults.size)
        assertEquals("Naruto", viewModel.state.value.searchResults.first().title)
        collectJob.cancel()
    }

    @Test
    fun onEvent_Search_onFailure_setsError() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = emptyList(), hasNextPage = false)
        )
        coEvery { searchMangaUseCase(any(), any(), any(), any()) } returns Result.failure(
            RuntimeException("Search failed")
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.OnSearchQueryChange("test"))
        viewModel.onEvent(BrowseEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Search failed", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSearching)
        collectJob.cancel()
    }

    // ── LoadLatest ────────────────────────────────────────────────────────────

    @Test
    fun onEvent_LoadLatest_withNoSourceSelected_doesNothing() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.LoadLatest)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.popularManga.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun onEvent_LoadLatest_loadsLatestUpdates() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = emptyList(), hasNextPage = false)
        )
        coEvery { getLatestUpdatesUseCase("source1", 1) } returns Result.success(
            MangaPage(mangas = listOf(sampleManga1, sampleManga2), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.LoadLatest)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.popularManga.size)
        assertEquals(1, viewModel.state.value.currentPage)
        collectJob.cancel()
    }

    @Test
    fun onEvent_LoadLatest_onFailure_setsError() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = emptyList(), hasNextPage = false)
        )
        coEvery { getLatestUpdatesUseCase(any(), any()) } returns Result.failure(
            RuntimeException("Latest failed")
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.LoadLatest)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Latest failed", viewModel.state.value.error)
        collectJob.cancel()
    }

    // ── LoadNextPage ──────────────────────────────────────────────────────────

    @Test
    fun onEvent_LoadNextPage_whenNoNextPage_doesNothing() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = listOf(sampleManga1), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.LoadNextPage)
        testDispatcher.scheduler.advanceUntilIdle()

        // Only page 1 was loaded
        assertEquals(1, viewModel.state.value.currentPage)
        collectJob.cancel()
    }

    @Test
    fun onEvent_LoadNextPage_withNextPage_loadsMangaAppended() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase("source1", 1) } returns Result.success(
            MangaPage(mangas = listOf(sampleManga1), hasNextPage = true)
        )
        coEvery { getPopularMangaUseCase("source1", 2) } returns Result.success(
            MangaPage(mangas = listOf(sampleManga2), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.LoadNextPage)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.popularManga.size)
        assertEquals(2, viewModel.state.value.currentPage)
        assertFalse(viewModel.state.value.hasNextPage)
        collectJob.cancel()
    }

    // ── ToggleFilterSheet ─────────────────────────────────────────────────────

    @Test
    fun onEvent_ToggleFilterSheet_togglingShowFilterSheet() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.showFilterSheet)

        viewModel.onEvent(BrowseEvent.ToggleFilterSheet)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.showFilterSheet)

        viewModel.onEvent(BrowseEvent.ToggleFilterSheet)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.showFilterSheet)
        collectJob.cancel()
    }

    // ── OnMangaClick ──────────────────────────────────────────────────────────

    @Test
    fun onEvent_OnMangaClick_withSourceSelected_emitsNavigateEffect() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = listOf(sampleManga1), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(BrowseEvent.OnMangaClick(sampleManga1))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is BrowseEffect.NavigateToMangaDetail)
            val navEffect = effect as BrowseEffect.NavigateToMangaDetail
            assertEquals("source1", navEffect.sourceId)
            assertEquals("/m/1", navEffect.mangaUrl)
        }
        collectJob.cancel()
    }

    @Test
    fun onEvent_OnMangaClick_withoutSourceSelected_doesNotEmitEffect() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(BrowseEvent.OnMangaClick(sampleManga1))
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
        collectJob.cancel()
    }

    // ── RefreshSources ────────────────────────────────────────────────────────

    @Test
    fun onEvent_RefreshSources_emitsSnackbarEffect() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(BrowseEvent.RefreshSources)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is BrowseEffect.ShowSnackbar)
        }
    }

    // ── Source Intelligence ───────────────────────────────────────────────────

    @Test
    fun sourceIntelligenceEnabled_falseWhenBothTogglesOff() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())
        every { aiPreferences.aiEnabled } returns flowOf(false)
        every { aiPreferences.aiSourceIntelligence } returns flowOf(false)

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.sourceIntelligenceEnabled)
        collectJob.cancel()
    }

    @Test
    fun sourceIntelligenceEnabled_trueWhenBothTogglesOn() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())
        every { aiPreferences.aiEnabled } returns flowOf(true)
        every { aiPreferences.aiSourceIntelligence } returns flowOf(true)

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.sourceIntelligenceEnabled)
        collectJob.cancel()
    }

    @Test
    fun sourceIntelligenceEnabled_falseWhenAiDisabledButSourceIntelOn() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())
        every { aiPreferences.aiEnabled } returns flowOf(false)
        every { aiPreferences.aiSourceIntelligence } returns flowOf(true)

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.sourceIntelligenceEnabled)
        collectJob.cancel()
    }

    @Test
    fun onEvent_RequestSourceScores_updatesScoresInState() = runTest {
        val scores = listOf(
            SourceScore(
                sourceId = "source1",
                mangaId = 1L,
                contentQualityScore = 0.9f,
                updateFrequencyScore = 0.8f,
                reliabilityScore = 0.95f,
                overallScore = 0.88f,
                recommendation = "Best scanlation quality"
            )
        )
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { scoreSourcesForManga.invoke(any(), any(), any()) } returns Result.success(scores)

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.RequestSourceScores(mangaId = 1L, mangaTitle = "Naruto"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.sourceScores.size)
        assertEquals("source1", viewModel.state.value.sourceScores.first().sourceId)
        assertFalse(viewModel.state.value.isAnalyzingSource)
        collectJob.cancel()
    }

    @Test
    fun onEvent_RequestSourceScores_onFailure_clearsLoadingState() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { scoreSourcesForManga.invoke(any(), any(), any()) } returns Result.success(emptyList<SourceScore>())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.RequestSourceScores(mangaId = 1L, mangaTitle = "Test"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isAnalyzingSource)
        assertTrue(viewModel.state.value.sourceScores.isEmpty())
        collectJob.cancel()
    }

    // ── ResetFilters / ApplyFilters ───────────────────────────────────────────

    @Test
    fun onEvent_ResetFilters_copiesAvailableToActive() = runTest {
        every { getSourcesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.ResetFilters)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(viewModel.state.value.availableFilters.filters, viewModel.state.value.activeFilters.filters)
        collectJob.cancel()
    }

    @Test
    fun onEvent_ApplyFilters_closesFilterSheetAndSearches() = runTest {
        every { getSourcesUseCase() } returns flowOf(listOf(source1))
        coEvery { getPopularMangaUseCase(any(), any()) } returns Result.success(
            MangaPage(mangas = emptyList(), hasNextPage = false)
        )
        coEvery { searchMangaUseCase(any(), any(), any(), any()) } returns Result.success(
            MangaPage(mangas = emptyList(), hasNextPage = false)
        )

        val viewModel = createViewModel()
        val collectJob = activateState(viewModel)
        viewModel.onEvent(BrowseEvent.SelectSource("source1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.ToggleFilterSheet)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.showFilterSheet)

        viewModel.onEvent(BrowseEvent.ApplyFilters)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.showFilterSheet)
        collectJob.cancel()
    }
}
