package app.otakureader.domain.usecase

import app.otakureader.domain.model.ChapterWithHistory
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

class GetHistoryUseCaseTest {

    private lateinit var chapterRepository: ChapterRepository
    private lateinit var useCase: GetHistoryUseCase

    @Before
    fun setUp() {
        chapterRepository = mockk()
        useCase = GetHistoryUseCase(chapterRepository)
    }

    @Test
    fun invoke_delegatesToRepository() = runTest {
        val history = listOf(
            ChapterWithHistory(
                chapter = Chapter(id = 1L, mangaId = 10L, url = "/c/1", name = "Chapter 1", read = true),
                readAt = 1000L,
                readDurationMs = 600_000L
            )
        )
        every { chapterRepository.observeHistory() } returns flowOf(history)

        useCase().test {
            assertEquals(history, awaitItem())
            awaitComplete()
        }

        verify(exactly = 1) { chapterRepository.observeHistory() }
    }

    @Test
    fun invoke_withNoHistory_emitsEmptyList() = runTest {
        every { chapterRepository.observeHistory() } returns flowOf(emptyList())

        useCase().test {
            assertEquals(emptyList<ChapterWithHistory>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_emitsMultipleHistoryEntries() = runTest {
        val entries = (1..3).map { i ->
            ChapterWithHistory(
                chapter = Chapter(id = i.toLong(), mangaId = 1L, url = "/c/$i", name = "Chapter $i", read = true),
                readAt = i * 1000L,
                readDurationMs = 300_000L
            )
        }
        every { chapterRepository.observeHistory() } returns flowOf(entries)

        useCase().test {
            assertEquals(3, awaitItem().size)
            awaitComplete()
        }
    }
}
