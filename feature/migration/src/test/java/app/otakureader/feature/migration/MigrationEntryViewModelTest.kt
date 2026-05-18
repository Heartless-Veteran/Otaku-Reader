package app.otakureader.feature.migration

import app.cash.turbine.test
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.usecase.GetLibraryMangaUseCase
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
class MigrationEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getLibraryManga: GetLibraryMangaUseCase

    private val sampleMangas = listOf(
        Manga(id = 1L, sourceId = 10L, url = "/m/1", title = "Naruto", favorite = true),
        Manga(id = 2L, sourceId = 10L, url = "/m/2", title = "Bleach", favorite = true),
        Manga(id = 3L, sourceId = 10L, url = "/m/3", title = "One Piece", favorite = true),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getLibraryManga = mockk {
            every { invoke() } returns flowOf(emptyList())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MigrationEntryViewModel(getLibraryManga)

    @Test
    fun init_loadsLibraryManga() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.state.value.mangaList.size)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun init_withEmptyLibrary_emitsEmptyList() = runTest {
        every { getLibraryManga() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.mangaList.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun onEvent_OnMangaToggle_addsIdToSelection() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(MigrationEntryEvent.OnMangaToggle(mangaId = 1L))

        assertTrue(viewModel.state.value.selectedIds.contains(1L))
    }

    @Test
    fun onEvent_OnMangaToggle_togglesSelection_whenAlreadySelected() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(MigrationEntryEvent.OnMangaToggle(mangaId = 1L))
        assertTrue(viewModel.state.value.selectedIds.contains(1L))

        viewModel.onEvent(MigrationEntryEvent.OnMangaToggle(mangaId = 1L))
        assertFalse(viewModel.state.value.selectedIds.contains(1L))
    }

    @Test
    fun onEvent_SelectAll_selectsAllManga() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(MigrationEntryEvent.SelectAll)

        assertEquals(3, viewModel.state.value.selectedIds.size)
        assertTrue(viewModel.state.value.selectedIds.containsAll(listOf(1L, 2L, 3L)))
    }

    @Test
    fun onEvent_ClearSelection_removesAllSelected() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(MigrationEntryEvent.OnMangaToggle(mangaId = 1L))
        viewModel.onEvent(MigrationEntryEvent.OnMangaToggle(mangaId = 2L))
        assertEquals(2, viewModel.state.value.selectedIds.size)

        viewModel.onEvent(MigrationEntryEvent.ClearSelection)
        assertTrue(viewModel.state.value.selectedIds.isEmpty())
    }

    @Test
    fun onEvent_OnSearchQueryChange_updatesSearchQuery() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(MigrationEntryEvent.OnSearchQueryChange("Naruto"))
        assertEquals("Naruto", viewModel.state.value.searchQuery)
    }

    @Test
    fun filteredList_withQuery_returnsMatchingManga() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(MigrationEntryEvent.OnSearchQueryChange("Bleach"))

        val filtered = viewModel.filteredList()
        assertEquals(1, filtered.size)
        assertEquals("Bleach", filtered[0].title)
    }

    @Test
    fun filteredList_withBlankQuery_returnsAllManga() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(MigrationEntryEvent.OnSearchQueryChange(""))
        assertEquals(3, viewModel.filteredList().size)
    }

    @Test
    fun onEvent_OnStartMigration_withSelection_emitsNavigateToMigrationEffect() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(MigrationEntryEvent.OnMangaToggle(mangaId = 1L))
        viewModel.onEvent(MigrationEntryEvent.OnMangaToggle(mangaId = 2L))

        viewModel.effect.test {
            viewModel.onEvent(MigrationEntryEvent.OnStartMigration)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is MigrationEntryEffect.NavigateToMigration)
            val navigate = effect as MigrationEntryEffect.NavigateToMigration
            assertEquals(2, navigate.selectedMangaIds.size)
            assertTrue(navigate.selectedMangaIds.containsAll(listOf(1L, 2L)))
        }
    }

    @Test
    fun onEvent_OnStartMigration_withNoSelection_doesNotEmitEffect() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(MigrationEntryEvent.OnStartMigration)
            testDispatcher.scheduler.advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun onEvent_NavigateBack_emitsNavigateBackEffect() = runTest {
        every { getLibraryManga() } returns flowOf(sampleMangas)

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onEvent(MigrationEntryEvent.NavigateBack)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(awaitItem() is MigrationEntryEffect.NavigateBack)
        }
    }
}
