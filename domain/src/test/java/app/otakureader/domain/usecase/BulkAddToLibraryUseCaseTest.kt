package app.otakureader.domain.usecase

import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BulkAddToLibraryUseCase].
 *
 * Covers:
 * - M-8: Input validation (empty list, non-positive IDs)
 * - M-9: Parallel execution path
 * - Happy path: all manga found and added
 * - Partial failure: some manga not found
 * - Repository exception handling
 * - Category assignment
 */
class BulkAddToLibraryUseCaseTest {

    private lateinit var mangaRepository: MangaRepository
    private lateinit var useCase: BulkAddToLibraryUseCase

    private val sampleManga = Manga(
        id = 1L,
        sourceId = 1L,
        url = "/m/1",
        title = "Naruto",
        favorite = false
    )

    @Before
    fun setUp() {
        mangaRepository = mockk(relaxed = true)
        useCase = BulkAddToLibraryUseCase(mangaRepository)
    }

    // ── Input validation (M-8) ────────────────────────────────────────────────

    @Test
    fun `invoke with empty list returns zero counts without touching repository`() = runTest {
        val result = useCase(emptyList())

        assertEquals(0, result.successCount)
        assertEquals(0, result.failCount)
        assertEquals(0, result.totalCount)
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { mangaRepository.getMangaById(any()) }
    }

    @Test
    fun `invoke with non-positive IDs returns fail count without touching repository`() = runTest {
        val result = useCase(listOf(0L, -1L, -100L))

        assertEquals(0, result.successCount)
        assertEquals(3, result.failCount)
        assertEquals(3, result.totalCount)
        assertFalse(result.isSuccess)
        coVerify(exactly = 0) { mangaRepository.getMangaById(any()) }
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `invoke with valid IDs adds all manga and returns full success`() = runTest {
        coEvery { mangaRepository.getMangaById(1L) } returns sampleManga
        coEvery { mangaRepository.getMangaById(2L) } returns sampleManga.copy(id = 2L, title = "Bleach")
        coEvery { mangaRepository.addToFavorites(any()) } returns Unit

        val result = useCase(listOf(1L, 2L))

        assertEquals(2, result.successCount)
        assertEquals(0, result.failCount)
        assertEquals(2, result.totalCount)
        assertTrue(result.isSuccess)
        assertFalse(result.isPartialSuccess)
    }

    // ── Partial failure ───────────────────────────────────────────────────────

    @Test
    fun `invoke when some manga not found returns partial success`() = runTest {
        coEvery { mangaRepository.getMangaById(1L) } returns sampleManga
        coEvery { mangaRepository.getMangaById(99L) } returns null
        coEvery { mangaRepository.addToFavorites(any()) } returns Unit

        val result = useCase(listOf(1L, 99L))

        assertEquals(1, result.successCount)
        assertEquals(1, result.failCount)
        assertTrue(result.isPartialSuccess)
        assertTrue(result.errors.any { it.contains("99") })
    }

    // ── Exception handling ────────────────────────────────────────────────────

    @Test
    fun `invoke when repository throws exception records error and continues`() = runTest {
        coEvery { mangaRepository.getMangaById(1L) } returns sampleManga
        coEvery { mangaRepository.addToFavorites(1L) } throws RuntimeException("DB error")
        coEvery { mangaRepository.getMangaById(2L) } returns sampleManga.copy(id = 2L)
        coEvery { mangaRepository.addToFavorites(2L) } returns Unit

        val result = useCase(listOf(1L, 2L))

        assertEquals(1, result.successCount)
        assertEquals(1, result.failCount)
        assertTrue(result.errors.any { it.contains("DB error") })
    }

    // ── Category assignment ───────────────────────────────────────────────────

    @Test
    fun `invoke with categoryId assigns category to each successfully added manga`() = runTest {
        coEvery { mangaRepository.getMangaById(1L) } returns sampleManga
        coEvery { mangaRepository.addToFavorites(any()) } returns Unit
        coEvery { mangaRepository.addMangaToCategory(any(), any()) } returns Unit

        useCase(listOf(1L), categoryId = 5L)

        coVerify(exactly = 1) { mangaRepository.addMangaToCategory(1L, 5L) }
    }

    @Test
    fun `invoke without categoryId does not call addMangaToCategory`() = runTest {
        coEvery { mangaRepository.getMangaById(1L) } returns sampleManga
        coEvery { mangaRepository.addToFavorites(any()) } returns Unit

        useCase(listOf(1L), categoryId = null)

        coVerify(exactly = 0) { mangaRepository.addMangaToCategory(any(), any()) }
    }
}
