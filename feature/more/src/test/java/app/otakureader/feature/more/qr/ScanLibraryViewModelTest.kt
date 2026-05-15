package app.otakureader.feature.more.qr

import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.ShareableLibrary
import app.otakureader.domain.model.ShareableManga
import app.otakureader.domain.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanLibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mangaRepository: MangaRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mangaRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ScanLibraryViewModel(mangaRepository)

    private fun shareableManga(title: String, sourceId: String = "100", url: String = "/m/$title") =
        ShareableManga(title = title, sourceId = sourceId, url = url)

    private fun libraryWith(vararg items: ShareableManga) = ShareableLibrary(manga = items.toList())

    private fun manga(id: Long, sourceId: Long = 100L, url: String = "/m/test") =
        Manga(id = id, sourceId = sourceId, url = url, title = "Manga $id")

    @Test
    fun `initial state is Idle`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.importState.value is ScanLibraryViewModel.ImportState.Idle)
    }

    @Test
    fun `importing empty library transitions to Done with zero counts`() = runTest {
        val viewModel = createViewModel()

        viewModel.importLibrary(libraryWith())
        advanceUntilIdle()

        val state = viewModel.importState.value as ScanLibraryViewModel.ImportState.Done
        assertEquals(0, state.imported)
        assertEquals(0, state.skipped)
    }

    @Test
    fun `all manga found transitions to Done with full imported count`() = runTest {
        val item1 = shareableManga("Naruto", "100", "/m/naruto")
        val item2 = shareableManga("Bleach", "200", "/m/bleach")
        val manga1 = manga(1L, 100L, "/m/naruto")
        val manga2 = manga(2L, 200L, "/m/bleach")
        coEvery { mangaRepository.getMangaBySourceAndUrl(100L, "/m/naruto") } returns manga1
        coEvery { mangaRepository.getMangaBySourceAndUrl(200L, "/m/bleach") } returns manga2

        val viewModel = createViewModel()
        viewModel.importLibrary(libraryWith(item1, item2))
        advanceUntilIdle()

        val state = viewModel.importState.value as ScanLibraryViewModel.ImportState.Done
        assertEquals(2, state.imported)
        assertEquals(0, state.skipped)
        coVerify(exactly = 1) { mangaRepository.addToFavorites(1L) }
        coVerify(exactly = 1) { mangaRepository.addToFavorites(2L) }
    }

    @Test
    fun `manga not in db is counted as skipped`() = runTest {
        val item = shareableManga("Unknown", "100", "/m/unknown")
        coEvery { mangaRepository.getMangaBySourceAndUrl(100L, "/m/unknown") } returns null

        val viewModel = createViewModel()
        viewModel.importLibrary(libraryWith(item))
        advanceUntilIdle()

        val state = viewModel.importState.value as ScanLibraryViewModel.ImportState.Done
        assertEquals(0, state.imported)
        assertEquals(1, state.skipped)
    }

    @Test
    fun `invalid sourceId is counted as skipped without calling repository`() = runTest {
        val item = shareableManga("Bad Source", sourceId = "not-a-number")

        val viewModel = createViewModel()
        viewModel.importLibrary(libraryWith(item))
        advanceUntilIdle()

        val state = viewModel.importState.value as ScanLibraryViewModel.ImportState.Done
        assertEquals(0, state.imported)
        assertEquals(1, state.skipped)
        coVerify(exactly = 0) { mangaRepository.getMangaBySourceAndUrl(any(), any()) }
    }

    @Test
    fun `mixed results correctly split imported and skipped counts`() = runTest {
        val found = shareableManga("Found", "100", "/m/found")
        val missing = shareableManga("Missing", "200", "/m/missing")
        val badId = shareableManga("Bad", sourceId = "abc")
        coEvery { mangaRepository.getMangaBySourceAndUrl(100L, "/m/found") } returns manga(1L)
        coEvery { mangaRepository.getMangaBySourceAndUrl(200L, "/m/missing") } returns null

        val viewModel = createViewModel()
        viewModel.importLibrary(libraryWith(found, missing, badId))
        advanceUntilIdle()

        val state = viewModel.importState.value as ScanLibraryViewModel.ImportState.Done
        assertEquals(1, state.imported)
        assertEquals(2, state.skipped)
    }

    @Test
    fun `repository exception transitions to Error state`() = runTest {
        val item = shareableManga("Test", "100", "/m/test")
        coEvery { mangaRepository.getMangaBySourceAndUrl(any(), any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()
        viewModel.importLibrary(libraryWith(item))
        advanceUntilIdle()

        val state = viewModel.importState.value as ScanLibraryViewModel.ImportState.Error
        assertEquals("DB error", state.message)
    }

    @Test
    fun `calling importLibrary while importing is a no-op`() = runTest {
        val item = shareableManga("Test", "100", "/m/test")
        coEvery { mangaRepository.getMangaBySourceAndUrl(any(), any()) } returns manga(1L)

        val viewModel = createViewModel()
        viewModel.importLibrary(libraryWith(item))

        // Second call before idle — should be ignored
        viewModel.importLibrary(libraryWith(item))
        advanceUntilIdle()

        // Only one pass happened
        coVerify(exactly = 1) { mangaRepository.getMangaBySourceAndUrl(any(), any()) }
    }
}
