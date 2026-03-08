package app.otakureader.domain.usecase

import app.otakureader.domain.model.LibraryManga
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.repository.MangaRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetLibraryUseCaseTest {

    private lateinit var mangaRepository: MangaRepository
    private lateinit var useCase: GetLibraryUseCase

    private val sampleMangas = listOf(
        Manga(id = 1L, sourceId = 1L, url = "/m/1", title = "Naruto", favorite = true, genre = listOf("Action")),
        Manga(id = 2L, sourceId = 1L, url = "/m/2", title = "Bleach", favorite = true, genre = listOf("Action")),
        Manga(id = 3L, sourceId = 1L, url = "/m/3", title = "One Piece", favorite = true, genre = listOf("Adventure"))
    )

    @Before
    fun setUp() {
        mangaRepository = mockk()
        useCase = GetLibraryUseCase(mangaRepository)
    }

    @Test
    fun invoke_withBlankQuery_returnsAllLibraryManga() = runTest {
        every { mangaRepository.getLibraryManga() } returns flowOf(sampleMangas)

        useCase("").test {
            val items = awaitItem()
            assertEquals(3, items.size)
            assertEquals("Naruto", items[0].manga.title)
            awaitComplete()
        }
    }

    @Test
    fun invoke_withQuery_filtersResultsByTitle() = runTest {
        every { mangaRepository.getLibraryManga() } returns flowOf(sampleMangas)

        useCase("one").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("One Piece", items[0].manga.title)
            awaitComplete()
        }
    }

    @Test
    fun invoke_withCaseInsensitiveQuery_returnsMatches() = runTest {
        every { mangaRepository.getLibraryManga() } returns flowOf(sampleMangas)

        useCase("NARUTO").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Naruto", items[0].manga.title)
            awaitComplete()
        }
    }

    @Test
    fun invoke_withNonMatchingQuery_returnsEmptyList() = runTest {
        every { mangaRepository.getLibraryManga() } returns flowOf(sampleMangas)

        useCase("Dragon Ball").test {
            val items = awaitItem()
            assertEquals(0, items.size)
            awaitComplete()
        }
    }

    @Test
    fun invoke_withEmptyLibrary_returnsEmptyList() = runTest {
        every { mangaRepository.getLibraryManga() } returns flowOf(emptyList())

        useCase("").test {
            assertEquals(emptyList<LibraryManga>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_mapsUnreadCountFromManga() = runTest {
        val mangaWithUnread = Manga(
            id = 1L, sourceId = 1L, url = "/m/1", title = "Naruto", favorite = true,
            unreadCount = 5
        )
        every { mangaRepository.getLibraryManga() } returns flowOf(listOf(mangaWithUnread))

        useCase("").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(5, items[0].unreadCount)
            awaitComplete()
        }
    }
}
