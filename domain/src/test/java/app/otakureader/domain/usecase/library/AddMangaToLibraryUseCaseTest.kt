package app.otakureader.domain.usecase.library

import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.sourceapi.SourceManga
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddMangaToLibraryUseCaseTest {

    private lateinit var mangaRepository: MangaRepository
    private lateinit var useCase: AddMangaToLibraryUseCase

    private val sourceManga = SourceManga(
        url = "/m/1",
        title = "Naruto",
        thumbnailUrl = "https://example.com/thumb.jpg",
        author = "Kishimoto",
        genre = "Action, Adventure"
    )

    private val existingManga = Manga(
        id = 10L,
        sourceId = 1L,
        url = "/m/1",
        title = "Naruto",
        favorite = true
    )

    @Before
    fun setUp() {
        mangaRepository = mockk()
        useCase = AddMangaToLibraryUseCase(mangaRepository)
    }

    @Test
    fun `invoke adds new manga when not already in library`() = runTest {
        coEvery { mangaRepository.getMangaBySourceAndUrl(any(), "/m/1") } returns null
        coEvery { mangaRepository.insertManga(any()) } returns 42L

        val result = useCase(sourceManga, "en.mangadex")

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())
        coVerify(exactly = 1) { mangaRepository.insertManga(any()) }
    }

    @Test
    fun `invoke returns existing id when manga already in library and is favorite`() = runTest {
        coEvery {
            mangaRepository.getMangaBySourceAndUrl(any(), "/m/1")
        } returns existingManga

        val result = useCase(sourceManga, "en.mangadex")

        assertTrue(result.isSuccess)
        assertEquals(10L, result.getOrNull())
        coVerify(exactly = 0) { mangaRepository.insertManga(any()) }
        coVerify(exactly = 0) { mangaRepository.toggleFavorite(any()) }
    }

    @Test
    fun `invoke toggles favorite when manga exists but is not favorited`() = runTest {
        val nonFavoriteManga = existingManga.copy(favorite = false)
        coEvery {
            mangaRepository.getMangaBySourceAndUrl(any(), "/m/1")
        } returns nonFavoriteManga
        coEvery { mangaRepository.toggleFavorite(10L) } returns Unit

        val result = useCase(sourceManga, "en.mangadex")

        assertTrue(result.isSuccess)
        assertEquals(10L, result.getOrNull())
        coVerify(exactly = 1) { mangaRepository.toggleFavorite(10L) }
    }

    @Test
    fun `invoke with multiple source mangas adds all and returns count`() = runTest {
        val sourceMangas = listOf(
            SourceManga(url = "/m/1", title = "Naruto"),
            SourceManga(url = "/m/2", title = "Bleach"),
            SourceManga(url = "/m/3", title = "One Piece")
        )
        coEvery { mangaRepository.getMangaBySourceAndUrl(any(), any()) } returns null
        coEvery { mangaRepository.insertManga(any()) } returnsMany listOf(1L, 2L, 3L)

        val result = useCase(sourceMangas, "en.mangadex")

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun `invoke inserts manga with favorite set to true`() = runTest {
        coEvery { mangaRepository.getMangaBySourceAndUrl(any(), "/m/1") } returns null
        coEvery { mangaRepository.insertManga(any()) } returns 1L

        useCase(sourceManga, "en.mangadex")

        coVerify {
            mangaRepository.insertManga(match { it.favorite })
        }
    }

    @Test
    fun `invoke parses genres from comma-separated string`() = runTest {
        val mangaWithGenres = SourceManga(url = "/m/1", title = "Test", genre = "Action, Adventure, Fantasy")
        coEvery { mangaRepository.getMangaBySourceAndUrl(any(), any()) } returns null
        coEvery { mangaRepository.insertManga(any()) } returns 1L

        useCase(mangaWithGenres, "en.source")

        coVerify {
            mangaRepository.insertManga(match { it.genre == listOf("Action", "Adventure", "Fantasy") })
        }
    }

    @Test
    fun `invoke with empty list returns zero count`() = runTest {
        val result = useCase(emptyList(), "en.mangadex")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }
}
