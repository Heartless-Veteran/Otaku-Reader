package app.otakureader.domain.usecase.source

import app.otakureader.domain.repository.SourceRepository
import app.otakureader.sourceapi.FilterList
import app.otakureader.sourceapi.MangaPage
import app.otakureader.sourceapi.MangaSource
import app.otakureader.sourceapi.SourceManga
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SourceUseCasesTest {

    private lateinit var sourceRepository: SourceRepository

    private val mockSource = mockk<MangaSource>(relaxed = true) {
        every { id } returns "en.mangadex"
        every { name } returns "MangaDex"
        every { lang } returns "en"
    }

    private val testMangaPage = MangaPage(
        mangas = listOf(
            SourceManga(url = "/m/1", title = "Naruto"),
            SourceManga(url = "/m/2", title = "Bleach")
        ),
        hasNextPage = false
    )

    @Before
    fun setUp() {
        sourceRepository = mockk()
    }

    // --- GetSourcesUseCase ---

    @Test
    fun `GetSources returns all sources from repository`() = runTest {
        val sources = listOf(mockSource)
        every { sourceRepository.getSources() } returns flowOf(sources)

        val useCase = GetSourcesUseCase(sourceRepository)
        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("en.mangadex", result[0].id)
            awaitComplete()
        }

        verify(exactly = 1) { sourceRepository.getSources() }
    }

    @Test
    fun `GetSources returns empty list when no sources`() = runTest {
        every { sourceRepository.getSources() } returns flowOf(emptyList())

        val useCase = GetSourcesUseCase(sourceRepository)
        useCase().test {
            assertEquals(emptyList<MangaSource>(), awaitItem())
            awaitComplete()
        }
    }

    // --- GetPopularMangaUseCase ---

    @Test
    fun `GetPopularManga delegates to repository`() = runTest {
        coEvery { sourceRepository.getPopularManga("en.mangadex", 1) } returns Result.success(testMangaPage)

        val useCase = GetPopularMangaUseCase(sourceRepository)
        val result = useCase("en.mangadex", 1)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.mangas?.size)
        coVerify(exactly = 1) { sourceRepository.getPopularManga("en.mangadex", 1) }
    }

    @Test
    fun `GetPopularManga propagates failure`() = runTest {
        val error = RuntimeException("Network error")
        coEvery { sourceRepository.getPopularManga(any(), any()) } returns Result.failure(error)

        val useCase = GetPopularMangaUseCase(sourceRepository)
        val result = useCase("en.mangadex", 1)

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `GetPopularManga uses default page of 1`() = runTest {
        coEvery { sourceRepository.getPopularManga("en.mangadex", 1) } returns Result.success(testMangaPage)

        val useCase = GetPopularMangaUseCase(sourceRepository)
        useCase("en.mangadex")

        coVerify { sourceRepository.getPopularManga("en.mangadex", 1) }
    }

    // --- GetLatestUpdatesUseCase ---

    @Test
    fun `GetLatestUpdates delegates to repository`() = runTest {
        coEvery { sourceRepository.getLatestUpdates("en.mangadex", 1) } returns Result.success(testMangaPage)

        val useCase = GetLatestUpdatesUseCase(sourceRepository)
        val result = useCase("en.mangadex", 1)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { sourceRepository.getLatestUpdates("en.mangadex", 1) }
    }

    @Test
    fun `GetLatestUpdates propagates failure`() = runTest {
        val error = RuntimeException("Timeout")
        coEvery { sourceRepository.getLatestUpdates(any(), any()) } returns Result.failure(error)

        val useCase = GetLatestUpdatesUseCase(sourceRepository)
        val result = useCase("en.mangadex", 1)

        assertTrue(result.isFailure)
        assertEquals("Timeout", result.exceptionOrNull()?.message)
    }

    // --- SearchMangaUseCase ---

    @Test
    fun `SearchManga delegates to repository with filters`() = runTest {
        val filters = FilterList()
        coEvery { sourceRepository.searchManga("en.mangadex", "naruto", 1, filters) } returns
            Result.success(testMangaPage)

        val useCase = SearchMangaUseCase(sourceRepository)
        val result = useCase("en.mangadex", "naruto", 1, filters)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { sourceRepository.searchManga("en.mangadex", "naruto", 1, filters) }
    }

    @Test
    fun `SearchManga propagates failure`() = runTest {
        val error = RuntimeException("Search failed")
        coEvery { sourceRepository.searchManga(any(), any(), any(), any()) } returns Result.failure(error)

        val useCase = SearchMangaUseCase(sourceRepository)
        val result = useCase("en.mangadex", "naruto")

        assertTrue(result.isFailure)
        assertEquals("Search failed", result.exceptionOrNull()?.message)
    }

    // --- GetMangaDetailsUseCase ---

    @Test
    fun `GetMangaDetails delegates to repository`() = runTest {
        val sourceManga = SourceManga(url = "/m/1", title = "Naruto")
        val detailedManga = sourceManga.copy(author = "Kishimoto", initialized = true)
        coEvery { sourceRepository.getMangaDetails("en.mangadex", sourceManga) } returns
            Result.success(detailedManga)

        val useCase = GetMangaDetailsUseCase(sourceRepository)
        val result = useCase("en.mangadex", sourceManga)

        assertTrue(result.isSuccess)
        assertEquals("Kishimoto", result.getOrNull()?.author)
        coVerify(exactly = 1) { sourceRepository.getMangaDetails("en.mangadex", sourceManga) }
    }

    @Test
    fun `GetMangaDetails propagates failure`() = runTest {
        val sourceManga = SourceManga(url = "/m/1", title = "Naruto")
        val error = RuntimeException("Details failed")
        coEvery { sourceRepository.getMangaDetails(any(), any()) } returns Result.failure(error)

        val useCase = GetMangaDetailsUseCase(sourceRepository)
        val result = useCase("en.mangadex", sourceManga)

        assertTrue(result.isFailure)
    }

    // --- GetSourceFiltersUseCase ---

    @Test
    fun `GetSourceFilters delegates to repository`() = runTest {
        val filters = FilterList()
        coEvery { sourceRepository.getSourceFilters("en.mangadex") } returns filters

        val useCase = GetSourceFiltersUseCase(sourceRepository)
        val result = useCase("en.mangadex")

        assertEquals(filters, result)
        coVerify(exactly = 1) { sourceRepository.getSourceFilters("en.mangadex") }
    }

    // --- GlobalSearchUseCase ---

    @Test
    fun `GlobalSearch delegates to repository with query and page 1`() = runTest {
        coEvery { sourceRepository.searchManga("en.mangadex", "one piece", 1) } returns
            Result.success(testMangaPage)

        val useCase = GlobalSearchUseCase(sourceRepository)
        val result = useCase("en.mangadex", "one piece")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { sourceRepository.searchManga("en.mangadex", "one piece", 1) }
    }

    @Test
    fun `GlobalSearch propagates failure`() = runTest {
        val error = RuntimeException("Global search failed")
        coEvery { sourceRepository.searchManga(any(), any(), any()) } returns Result.failure(error)

        val useCase = GlobalSearchUseCase(sourceRepository)
        val result = useCase("en.mangadex", "naruto")

        assertTrue(result.isFailure)
        assertEquals("Global search failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `GlobalSearch always searches page 1`() = runTest {
        coEvery { sourceRepository.searchManga("en.mangadex", "dragon ball", 1) } returns
            Result.success(testMangaPage)

        val useCase = GlobalSearchUseCase(sourceRepository)
        useCase("en.mangadex", "dragon ball")

        coVerify { sourceRepository.searchManga("en.mangadex", "dragon ball", 1) }
    }
}
