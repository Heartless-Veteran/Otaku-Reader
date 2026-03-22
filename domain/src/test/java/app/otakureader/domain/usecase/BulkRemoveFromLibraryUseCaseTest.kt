package app.otakureader.domain.usecase

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
 * Unit tests for [BulkRemoveFromLibraryUseCase].
 *
 * Mirrors [BulkAddToLibraryUseCaseTest] for the remove path.
 */
class BulkRemoveFromLibraryUseCaseTest {

    private lateinit var mangaRepository: MangaRepository
    private lateinit var useCase: BulkRemoveFromLibraryUseCase

    @Before
    fun setUp() {
        mangaRepository = mockk(relaxed = true)
        useCase = BulkRemoveFromLibraryUseCase(mangaRepository)
    }

    @Test
    fun `invoke with empty list returns zero counts without touching repository`() = runTest {
        val result = useCase(emptyList())

        assertEquals(0, result.successCount)
        assertEquals(0, result.failCount)
        coVerify(exactly = 0) { mangaRepository.removeFromFavorites(any()) }
    }

    @Test
    fun `invoke with non-positive IDs returns fail count without touching repository`() = runTest {
        val result = useCase(listOf(0L, -5L))

        assertEquals(0, result.successCount)
        assertEquals(2, result.failCount)
        assertTrue(result.failCount > 0)
        coVerify(exactly = 0) { mangaRepository.removeFromFavorites(any()) }
    }

    @Test
    fun `invoke with valid IDs removes all manga and returns full success`() = runTest {
        coEvery { mangaRepository.removeFromFavorites(any()) } returns Unit

        val result = useCase(listOf(1L, 2L, 3L))

        assertEquals(3, result.successCount)
        assertEquals(0, result.failCount)
        assertEquals(0, result.failCount)
    }

    @Test
    fun `invoke when repository throws exception records error and continues`() = runTest {
        coEvery { mangaRepository.removeFromFavorites(1L) } throws RuntimeException("DB locked")
        coEvery { mangaRepository.removeFromFavorites(2L) } returns Unit

        val result = useCase(listOf(1L, 2L))

        assertEquals(1, result.successCount)
        assertEquals(1, result.failCount)
        // Partial success: at least one succeeded and at least one failed
        assertTrue(result.successCount > 0 && result.failCount > 0)
        assertTrue(result.errors.any { it.contains("DB locked") })
    }
}
