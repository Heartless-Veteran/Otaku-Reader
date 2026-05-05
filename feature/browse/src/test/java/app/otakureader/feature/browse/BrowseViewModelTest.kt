package app.otakureader.feature.browse

import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.domain.model.FeedSavedSearch
import app.otakureader.domain.repository.FeedRepository
import app.otakureader.domain.usecase.library.AddMangaToLibraryUseCase
import app.otakureader.domain.usecase.source.GetLatestUpdatesUseCase
import app.otakureader.domain.usecase.source.GetPopularMangaUseCase
import app.otakureader.domain.usecase.source.GetSourceFiltersUseCase
import app.otakureader.domain.usecase.source.GetSourcesUseCase
import app.otakureader.domain.usecase.source.SearchMangaUseCase
import app.otakureader.sourceapi.FilterList
import app.otakureader.sourceapi.MangaPage
import app.otakureader.sourceapi.MangaSource
import app.otakureader.sourceapi.SourceManga
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
class BrowseViewModelTest {

    private val getSourcesUseCase: GetSourcesUseCase = mockk()
    private val getPopularMangaUseCase: GetPopularMangaUseCase = mockk()
    private val getLatestUpdatesUseCase: GetLatestUpdatesUseCase = mockk()
    private val searchMangaUseCase: SearchMangaUseCase = mockk()
    private val getSourceFiltersUseCase: GetSourceFiltersUseCase = mockk()
    private val addMangaToLibraryUseCase: AddMangaToLibraryUseCase = mockk()
    private val feedRepository: FeedRepository = mockk()
    private val generalPreferences: GeneralPreferences = mockk()

    private lateinit var viewModel: BrowseViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Default stubs
        every { getSourcesUseCase() } returns flowOf(emptyList())
        every { generalPreferences.showNsfwContent } returns flowOf(false)
        every { feedRepository.getSavedSearches() } returns flowOf(emptyList())

        viewModel = BrowseViewModel(
            getSourcesUseCase = getSourcesUseCase,
            getPopularMangaUseCase = getPopularMangaUseCase,
            getLatestUpdatesUseCase = getLatestUpdatesUseCase,
            searchMangaUseCase = searchMangaUseCase,
            getSourceFiltersUseCase = getSourceFiltersUseCase,
            addMangaToLibraryUseCase = addMangaToLibraryUseCase,
            feedRepository = feedRepository,
            generalPreferences = generalPreferences
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty sources and no loading`() = runTest {
        val state = viewModel.state.value
        assertEquals(emptyList<String>(), state.sources)
        assertEquals(false, state.isLoading)
        assertEquals(false, state.isSearching)
    }

    @Test
    fun `select source loads popular manga`() = runTest {
        val source = MangaSource(id = "1", name = "Test Source", lang = "en", isNsfw = false)
        val mangaPage = MangaPage(
            mangas = listOf(SourceManga(title = "Manga 1", url = "url1", thumbnailUrl = null)),
            hasNextPage = true
        )

        every { getSourcesUseCase() } returns flowOf(listOf(source))
        coEvery { getPopularMangaUseCase("1", 1) } returns Result.success(mangaPage)
        every { getSourceFiltersUseCase("1") } returns FilterList()

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("1", state.currentSourceId)
        assertEquals(1, state.popularManga.size)
        assertEquals("Manga 1", state.popularManga[0].title)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `search query change updates state`() = runTest {
        viewModel.onEvent(BrowseEvent.OnSearchQueryChange("One Piece"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("One Piece", viewModel.state.value.searchQuery)
    }

    @Test
    fun `search performs search with query`() = runTest {
        val source = MangaSource(id = "1", name = "Test Source", lang = "en", isNsfw = false)
        val mangaPage = MangaPage(
            mangas = listOf(
                SourceManga(title = "One Piece", url = "url1", thumbnailUrl = null),
                SourceManga(title = "One Punch Man", url = "url2", thumbnailUrl = null)
            ),
            hasNextPage = false
        )

        every { getSourcesUseCase() } returns flowOf(listOf(source))
        coEvery { searchMangaUseCase("1", "One", 1, any()) } returns Result.success(mangaPage)
        every { getSourceFiltersUseCase("1") } returns FilterList()

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.OnSearchQueryChange("One"))
        viewModel.onEvent(BrowseEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.searchResults.size)
        assertEquals(false, state.isSearching)
    }

    @Test
    fun `toggle filter sheet changes visibility`() = runTest {
        assertEquals(false, viewModel.state.value.showFilterSheet)

        viewModel.onEvent(BrowseEvent.ToggleFilterSheet)
        assertEquals(true, viewModel.state.value.showFilterSheet)

        viewModel.onEvent(BrowseEvent.ToggleFilterSheet)
        assertEquals(false, viewModel.state.value.showFilterSheet)
    }

    @Test
    fun `clear selection resets state`() = runTest {
        val source = MangaSource(id = "1", name = "Test Source", lang = "en", isNsfw = false)
        val manga = SourceManga(title = "Manga 1", url = "url1", thumbnailUrl = null)

        every { getSourcesUseCase() } returns flowOf(listOf(source))
        coEvery { getPopularMangaUseCase("1", 1) } returns Result.success(MangaPage(listOf(manga), false))
        every { getSourceFiltersUseCase("1") } returns FilterList()

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.OnMangaLongClick(manga))
        assertTrue(viewModel.state.value.isBulkSelectionMode)
        assertEquals(1, viewModel.state.value.selectedManga.size)

        viewModel.onEvent(BrowseEvent.ClearSelection)
        assertFalse(viewModel.state.value.isBulkSelectionMode)
        assertEquals(0, viewModel.state.value.selectedManga.size)
    }

    @Test
    fun `add selected to library sends success effect`() = runTest {
        val source = MangaSource(id = "1", name = "Test Source", lang = "en", isNsfw = false)
        val manga = SourceManga(title = "Manga 1", url = "url1", thumbnailUrl = null)

        every { getSourcesUseCase() } returns flowOf(listOf(source))
        coEvery { getPopularMangaUseCase("1", 1) } returns Result.success(MangaPage(listOf(manga), false))
        coEvery { addMangaToLibraryUseCase(listOf(manga), "1") } returns Result.success(1)
        every { getSourceFiltersUseCase("1") } returns FilterList()

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.OnMangaLongClick(manga))
        viewModel.onEvent(BrowseEvent.AddSelectedToLibrary)
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is BrowseEffect.ShowSnackbar)
    }

    @Test
    fun `load next page appends results`() = runTest {
        val source = MangaSource(id = "1", name = "Test Source", lang = "en", isNsfw = false)
        val page1 = MangaPage(
            mangas = listOf(SourceManga(title = "Manga 1", url = "url1", thumbnailUrl = null)),
            hasNextPage = true
        )
        val page2 = MangaPage(
            mangas = listOf(SourceManga(title = "Manga 2", url = "url2", thumbnailUrl = null)),
            hasNextPage = false
        )

        every { getSourcesUseCase() } returns flowOf(listOf(source))
        coEvery { getPopularMangaUseCase("1", 1) } returns Result.success(page1)
        coEvery { getPopularMangaUseCase("1", 2) } returns Result.success(page2)
        every { getSourceFiltersUseCase("1") } returns FilterList()

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.popularManga.size)

        viewModel.onEvent(BrowseEvent.LoadNextPage)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.popularManga.size)
        assertEquals("Manga 1", viewModel.state.value.popularManga[0].title)
        assertEquals("Manga 2", viewModel.state.value.popularManga[1].title)
    }

    @Test
    fun `refresh sources sends snackbar effect`() = runTest {
        viewModel.onEvent(BrowseEvent.RefreshSources)
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is BrowseEffect.ShowSnackbar)
    }

    @Test
    fun `search results remain visible after search completes`() = runTest {
        val source = MangaSource(id = "1", name = "Test Source", lang = "en", isNsfw = false)
        val mangaPage = MangaPage(
            mangas = listOf(SourceManga(title = "Naruto", url = "url1", thumbnailUrl = null)),
            hasNextPage = false
        )

        every { getSourcesUseCase() } returns flowOf(listOf(source))
        coEvery { searchMangaUseCase("1", "Naruto", 1, any()) } returns Result.success(mangaPage)
        every { getSourceFiltersUseCase("1") } returns FilterList()

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.OnSearchQueryChange("Naruto"))
        viewModel.onEvent(BrowseEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("isSearching must be false after search completes", state.isSearching)
        assertTrue("hasSearchResults must be true after successful search", state.hasSearchResults)
        assertEquals(1, state.searchResults.size)
    }

    @Test
    fun `selecting source resets search results`() = runTest {
        val source1 = MangaSource(id = "1", name = "Source 1", lang = "en", isNsfw = false)
        val source2 = MangaSource(id = "2", name = "Source 2", lang = "en", isNsfw = false)
        val mangaPage = MangaPage(
            mangas = listOf(SourceManga(title = "Manga 1", url = "url1", thumbnailUrl = null)),
            hasNextPage = false
        )

        every { getSourcesUseCase() } returns flowOf(listOf(source1, source2))
        coEvery { searchMangaUseCase("1", "query", 1, any()) } returns Result.success(mangaPage)
        coEvery { getPopularMangaUseCase("2", 1) } returns Result.success(MangaPage(emptyList(), false))
        every { getSourceFiltersUseCase(any()) } returns FilterList()

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(BrowseEvent.OnSearchQueryChange("query"))
        viewModel.onEvent(BrowseEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("hasSearchResults must be true after search", viewModel.state.value.hasSearchResults)

        viewModel.onEvent(BrowseEvent.SelectSource("2"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("hasSearchResults must be false after source change", viewModel.state.value.hasSearchResults)
        assertEquals(0, viewModel.state.value.searchResults.size)
    }

    @Test
    fun `clearing query resets hasSearchResults`() = runTest {
        val source = MangaSource(id = "1", name = "Test Source", lang = "en", isNsfw = false)
        val mangaPage = MangaPage(
            mangas = listOf(SourceManga(title = "Manga", url = "url1", thumbnailUrl = null)),
            hasNextPage = false
        )

        every { getSourcesUseCase() } returns flowOf(listOf(source))
        coEvery { searchMangaUseCase("1", "query", 1, any()) } returns Result.success(mangaPage)
        every { getSourceFiltersUseCase("1") } returns FilterList()

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(BrowseEvent.OnSearchQueryChange("query"))
        viewModel.onEvent(BrowseEvent.Search)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("hasSearchResults must be true after search", viewModel.state.value.hasSearchResults)

        viewModel.onEvent(BrowseEvent.OnSearchQueryChange(""))
        assertEquals("hasSearchResults must be false when query cleared", false, viewModel.state.value.hasSearchResults)
    }

    @Test
    fun `saved searches filtered to selected source`() = runTest {
        val search1 = FeedSavedSearch(id = 1L, sourceId = 1L, sourceName = "1", query = "one piece", filters = emptyMap())
        val search2 = FeedSavedSearch(id = 2L, sourceId = 2L, sourceName = "2", query = "naruto", filters = emptyMap())
        val source = MangaSource(id = "1", name = "Source 1", lang = "en", isNsfw = false)

        every { feedRepository.getSavedSearches() } returns flowOf(listOf(search1, search2))
        every { getSourcesUseCase() } returns flowOf(listOf(source))
        every { getSourceFiltersUseCase("1") } returns FilterList()
        coEvery { getPopularMangaUseCase("1", 1) } returns Result.success(MangaPage(emptyList(), false))

        viewModel = BrowseViewModel(
            getSourcesUseCase = getSourcesUseCase,
            getPopularMangaUseCase = getPopularMangaUseCase,
            getLatestUpdatesUseCase = getLatestUpdatesUseCase,
            searchMangaUseCase = searchMangaUseCase,
            getSourceFiltersUseCase = getSourceFiltersUseCase,
            addMangaToLibraryUseCase = addMangaToLibraryUseCase,
            feedRepository = feedRepository,
            generalPreferences = generalPreferences
        )

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        val savedSearches = viewModel.state.value.savedSearches
        assertEquals(1, savedSearches.size)
        assertEquals("one piece", savedSearches[0].query)
    }

    @Test
    fun `saved searches update when source changes`() = runTest {
        val search1 = FeedSavedSearch(id = 1L, sourceId = 1L, sourceName = "1", query = "one piece", filters = emptyMap())
        val search2 = FeedSavedSearch(id = 2L, sourceId = 2L, sourceName = "2", query = "naruto", filters = emptyMap())
        val source1 = MangaSource(id = "1", name = "Source 1", lang = "en", isNsfw = false)
        val source2 = MangaSource(id = "2", name = "Source 2", lang = "en", isNsfw = false)

        val savedSearchFlow = MutableStateFlow(listOf(search1, search2))
        every { feedRepository.getSavedSearches() } returns savedSearchFlow
        every { getSourcesUseCase() } returns flowOf(listOf(source1, source2))
        every { getSourceFiltersUseCase(any()) } returns FilterList()
        coEvery { getPopularMangaUseCase(any(), 1) } returns Result.success(MangaPage(emptyList(), false))

        viewModel = BrowseViewModel(
            getSourcesUseCase = getSourcesUseCase,
            getPopularMangaUseCase = getPopularMangaUseCase,
            getLatestUpdatesUseCase = getLatestUpdatesUseCase,
            searchMangaUseCase = searchMangaUseCase,
            getSourceFiltersUseCase = getSourceFiltersUseCase,
            addMangaToLibraryUseCase = addMangaToLibraryUseCase,
            feedRepository = feedRepository,
            generalPreferences = generalPreferences
        )

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.savedSearches.size)
        assertEquals("one piece", viewModel.state.value.savedSearches[0].query)

        viewModel.onEvent(BrowseEvent.SelectSource("2"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.savedSearches.size)
        assertEquals("naruto", viewModel.state.value.savedSearches[0].query)
    }

    @Test
    fun `apply filters hides sheet and searches`() = runTest {
        val source = MangaSource(id = "1", name = "Test Source", lang = "en", isNsfw = false)
        val mangaPage = MangaPage(
            mangas = listOf(SourceManga(title = "Manga 1", url = "url1", thumbnailUrl = null)),
            hasNextPage = false
        )

        every { getSourcesUseCase() } returns flowOf(listOf(source))
        every { getSourceFiltersUseCase("1") } returns FilterList()
        coEvery { searchMangaUseCase("1", "", 1, any()) } returns Result.success(mangaPage)

        viewModel.onEvent(BrowseEvent.SelectSource("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BrowseEvent.ToggleFilterSheet)
        assertTrue(viewModel.state.value.showFilterSheet)

        viewModel.onEvent(BrowseEvent.ApplyFilters)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.showFilterSheet)
        assertEquals(1, viewModel.state.value.searchResults.size)
    }
}
