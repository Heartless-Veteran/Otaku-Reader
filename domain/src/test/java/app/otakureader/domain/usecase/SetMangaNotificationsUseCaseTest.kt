package app.otakureader.domain.usecase

import app.otakureader.domain.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SetMangaNotificationsUseCaseTest {

    private lateinit var mangaRepository: MangaRepository
    private lateinit var useCase: SetMangaNotificationsUseCase

    @Before
    fun setUp() {
        mangaRepository = mockk()
        useCase = SetMangaNotificationsUseCase(mangaRepository)
    }

    @Test
    fun `invoke with notify true calls updateNotifyNewChapters with true`() = runTest {
        coEvery { mangaRepository.updateNotifyNewChapters(1L, true) } returns Unit

        useCase(1L, true)

        coVerify(exactly = 1) { mangaRepository.updateNotifyNewChapters(1L, true) }
    }

    @Test
    fun `invoke with notify false calls updateNotifyNewChapters with false`() = runTest {
        coEvery { mangaRepository.updateNotifyNewChapters(2L, false) } returns Unit

        useCase(2L, false)

        coVerify(exactly = 1) { mangaRepository.updateNotifyNewChapters(2L, false) }
    }

    @Test
    fun `invoke passes mangaId correctly`() = runTest {
        val mangaId = 99L
        coEvery { mangaRepository.updateNotifyNewChapters(mangaId, any()) } returns Unit

        useCase(mangaId, true)

        coVerify { mangaRepository.updateNotifyNewChapters(mangaId, true) }
    }
}
