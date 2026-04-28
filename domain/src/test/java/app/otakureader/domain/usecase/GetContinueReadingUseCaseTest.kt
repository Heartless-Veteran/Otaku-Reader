package app.otakureader.domain.usecase

import app.otakureader.domain.model.ContinueReadingItem
import app.otakureader.domain.repository.ChapterRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetContinueReadingUseCaseTest {

    private val chapterRepository: ChapterRepository = mockk()
    private val useCase = GetContinueReadingUseCase(chapterRepository)

    @Test
    fun `invoke returns continue reading items`() = runTest {
        val items = listOf(
            ContinueReadingItem(mangaId = 1L, chapterId = 5L, mangaTitle = "Manga 1", thumbnailUrl = null, chapterName = "Ch 5", chapterNumber = 5f, lastPageRead = 3, readAt = 0L),
            ContinueReadingItem(mangaId = 2L, chapterId = 10L, mangaTitle = "Manga 2", thumbnailUrl = null, chapterName = "Ch 10", chapterNumber = 10f, lastPageRead = 0, readAt = 0L)
        )
        every { chapterRepository.observeContinueReading() } returns flowOf(items)

        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals("Manga 1", result[0].mangaTitle)
    }

    @Test
    fun `invoke returns empty when nothing to continue`() = runTest {
        every { chapterRepository.observeContinueReading() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(0, result.size)
    }
}
