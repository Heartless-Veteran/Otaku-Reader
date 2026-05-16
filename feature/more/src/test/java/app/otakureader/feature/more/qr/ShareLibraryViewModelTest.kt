package app.otakureader.feature.more.qr

import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.repository.MangaRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class ShareLibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mangaRepository: MangaRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mangaRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ShareLibraryViewModel(mangaRepository)

    @Test
    fun `initial state is empty list`() = runTest {
        every { mangaRepository.getLibraryManga() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.library.value.isEmpty())
    }

    @Test
    fun `library emits mapped ShareableManga list`() = runTest {
        val manga = Manga(
            id = 1L,
            sourceId = 100L,
            url = "/manga/naruto",
            title = "Naruto",
            thumbnailUrl = "https://example.com/thumb.jpg",
            status = MangaStatus.COMPLETED
        )
        every { mangaRepository.getLibraryManga() } returns flowOf(listOf(manga))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val library = viewModel.library.value
        assertEquals(1, library.size)
        assertEquals("Naruto", library[0].title)
        assertEquals("100", library[0].sourceId)
        assertEquals("/manga/naruto", library[0].url)
        assertEquals("https://example.com/thumb.jpg", library[0].thumbnailUrl)
        assertEquals(MangaStatus.COMPLETED, library[0].status)
    }

    @Test
    fun `library maps multiple manga preserving order`() = runTest {
        val manga1 = Manga(id = 1L, sourceId = 100L, url = "/m/1", title = "Alpha")
        val manga2 = Manga(id = 2L, sourceId = 200L, url = "/m/2", title = "Beta")
        val manga3 = Manga(id = 3L, sourceId = 300L, url = "/m/3", title = "Gamma")
        every { mangaRepository.getLibraryManga() } returns flowOf(listOf(manga1, manga2, manga3))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val library = viewModel.library.value
        assertEquals(3, library.size)
        assertEquals("Alpha", library[0].title)
        assertEquals("Beta", library[1].title)
        assertEquals("Gamma", library[2].title)
    }

    @Test
    fun `sourceId is mapped as string from long`() = runTest {
        val manga = Manga(id = 1L, sourceId = 1759567593, url = "/m/1", title = "Test")
        every { mangaRepository.getLibraryManga() } returns flowOf(listOf(manga))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("1759567593", viewModel.library.value[0].sourceId)
    }
}
