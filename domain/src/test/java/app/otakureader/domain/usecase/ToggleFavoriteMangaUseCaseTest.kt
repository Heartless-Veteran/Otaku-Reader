package app.otakureader.domain.usecase

import app.otakureader.domain.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ToggleFavoriteMangaUseCaseTest {

    private lateinit var mangaRepository: MangaRepository
    private lateinit var useCase: ToggleFavoriteMangaUseCase

    @Before
    fun setUp() {
        mangaRepository = mockk()
        useCase = ToggleFavoriteMangaUseCase(mangaRepository)
    }

    @Test
    fun invoke_delegatesToRepository() = runTest {
        val mangaId = 42L
        coEvery { mangaRepository.toggleFavorite(mangaId) } returns Unit

        useCase(mangaId)

        coVerify(exactly = 1) { mangaRepository.toggleFavorite(mangaId) }
    }

    @Test
    fun invoke_callsToggleFavoriteWithCorrectId() = runTest {
        val mangaId = 123L
        coEvery { mangaRepository.toggleFavorite(any()) } returns Unit

        useCase(mangaId)

        coVerify { mangaRepository.toggleFavorite(mangaId) }
    }

    @Test(expected = RuntimeException::class)
    fun invoke_propagatesRepositoryException() = runTest {
        val mangaId = 1L
        coEvery { mangaRepository.toggleFavorite(mangaId) } throws RuntimeException("DB error")

        useCase(mangaId)
    }
}
