package app.otakureader.domain.usecase

import app.otakureader.domain.model.Chapter
import app.otakureader.domain.repository.ChapterRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetChaptersUseCaseTest {

    private lateinit var chapterRepository: ChapterRepository
    private lateinit var useCase: GetChaptersUseCase

    @Before
    fun setUp() {
        chapterRepository = mockk()
        useCase = GetChaptersUseCase(chapterRepository)
    }

    @Test
    fun invoke_withMangaId_delegatesToRepository() = runTest {
        val mangaId = 42L
        val chapters = listOf(
            Chapter(id = 1L, mangaId = mangaId, url = "/c/1", name = "Chapter 1"),
            Chapter(id = 2L, mangaId = mangaId, url = "/c/2", name = "Chapter 2")
        )
        every { chapterRepository.getChaptersByMangaId(mangaId) } returns flowOf(chapters)

        useCase(mangaId).test {
            assertEquals(chapters, awaitItem())
            awaitComplete()
        }

        verify(exactly = 1) { chapterRepository.getChaptersByMangaId(mangaId) }
    }

    @Test
    fun invoke_withEmptyChapterList_emitsEmptyList() = runTest {
        val mangaId = 1L
        every { chapterRepository.getChaptersByMangaId(mangaId) } returns flowOf(emptyList())

        useCase(mangaId).test {
            assertEquals(emptyList<Chapter>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_propagatesMultipleEmissions() = runTest {
        val mangaId = 5L
        val first = listOf(Chapter(id = 1L, mangaId = mangaId, url = "/c/1", name = "Ch 1"))
        val second = first + Chapter(id = 2L, mangaId = mangaId, url = "/c/2", name = "Ch 2")

        every { chapterRepository.getChaptersByMangaId(mangaId) } returns
            kotlinx.coroutines.flow.flow {
                emit(first)
                emit(second)
            }

        useCase(mangaId).test {
            assertEquals(first, awaitItem())
            assertEquals(second, awaitItem())
            awaitComplete()
        }
    }
}
