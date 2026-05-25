package app.otakureader.domain.usecase

import app.otakureader.domain.model.DynamicCategoryRule
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.repository.DynamicCategoryRepository
import app.otakureader.domain.repository.MangaRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetDynamicCategoryMembersUseCaseTest {

    private val dynamicCategoryRepository: DynamicCategoryRepository = mockk()
    private val mangaRepository: MangaRepository = mockk()
    private val useCase = GetDynamicCategoryMembersUseCase(dynamicCategoryRepository, mangaRepository)

    private val now = System.currentTimeMillis()
    private val dayMs = 86_400_000L

    private fun manga(
        id: Long,
        unread: Int = 0,
        lastUpdate: Long = 0L,
        genre: List<String> = emptyList(),
    ) = Manga(
        id = id,
        sourceId = 1L,
        url = "/m/$id",
        title = "Manga $id",
        favorite = true,
        unreadCount = unread,
        lastUpdate = lastUpdate,
        genre = genre,
        status = MangaStatus.ONGOING,
    )

    @Before
    fun setUp() {
        every { mangaRepository.getLibraryManga() } returns flowOf(emptyList())
    }

    @Test
    fun `empty rules returns empty set`() = runTest {
        every { dynamicCategoryRepository.getRulesForCategory(1L) } returns flowOf(emptyList())

        useCase(1L).test {
            assertEquals(emptySet<Long>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UnreadAtLeast filters manga with sufficient unread chapters`() = runTest {
        val library = listOf(manga(1L, unread = 5), manga(2L, unread = 2), manga(3L, unread = 10))
        every { dynamicCategoryRepository.getRulesForCategory(1L) } returns flowOf(listOf(DynamicCategoryRule.UnreadAtLeast(5)))
        every { mangaRepository.getLibraryManga() } returns flowOf(library)

        useCase(1L).test {
            val result = awaitItem()
            assertTrue(1L in result)
            assertFalse(2L in result)
            assertTrue(3L in result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `RecentlyUpdated uses lastUpdate not lastRead`() = runTest {
        val recentMs = now - dayMs          // updated 1 day ago — within 7 days
        val staleMs = now - 10 * dayMs     // updated 10 days ago — outside 7 days
        val library = listOf(manga(1L, lastUpdate = recentMs), manga(2L, lastUpdate = staleMs))
        every { dynamicCategoryRepository.getRulesForCategory(1L) } returns flowOf(listOf(DynamicCategoryRule.RecentlyUpdated(withinDays = 7)))
        every { mangaRepository.getLibraryManga() } returns flowOf(library)

        useCase(1L).test {
            val result = awaitItem()
            assertTrue("manga updated recently must match", 1L in result)
            assertFalse("manga updated 10 days ago must not match", 2L in result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GenreContains matches genre substring case-insensitively`() = runTest {
        val library = listOf(
            manga(1L, genre = listOf("Action", "Adventure")),
            manga(2L, genre = listOf("Romance", "Drama")),
            manga(3L, genre = listOf("action comedy")),
        )
        every { dynamicCategoryRepository.getRulesForCategory(1L) } returns flowOf(listOf(DynamicCategoryRule.GenreContains("action")))
        every { mangaRepository.getLibraryManga() } returns flowOf(library)

        useCase(1L).test {
            val result = awaitItem()
            assertTrue(1L in result)
            assertFalse(2L in result)
            assertTrue(3L in result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple rules are AND-combined`() = runTest {
        val library = listOf(
            manga(1L, unread = 5, genre = listOf("Action")),  // matches both rules
            manga(2L, unread = 5, genre = listOf("Romance")), // fails genre rule
            manga(3L, unread = 1, genre = listOf("Action")),  // fails unread rule
        )
        val rules = listOf(
            DynamicCategoryRule.UnreadAtLeast(3),
            DynamicCategoryRule.GenreContains("Action"),
        )
        every { dynamicCategoryRepository.getRulesForCategory(1L) } returns flowOf(rules)
        every { mangaRepository.getLibraryManga() } returns flowOf(library)

        useCase(1L).test {
            val result = awaitItem()
            assertEquals(setOf(1L), result)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
