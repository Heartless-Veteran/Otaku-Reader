package app.otakureader.domain.usecase

import app.otakureader.domain.model.Category
import app.otakureader.domain.repository.CategoryRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetCategoriesUseCaseTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var useCase: GetCategoriesUseCase

    @Before
    fun setUp() {
        categoryRepository = mockk()
        useCase = GetCategoriesUseCase(categoryRepository)
    }

    @Test
    fun `invoke with no categories returns empty list`() = runTest {
        every { categoryRepository.getCategories() } returns flowOf(emptyList())

        useCase().test {
            assertEquals(emptyList<Category>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `invoke with categories populates mangaCount from getMangaIdsByCategoryId`() = runTest {
        val categories = listOf(
            Category(id = 1L, name = "Favorites", order = 0),
            Category(id = 2L, name = "Reading", order = 1)
        )
        every { categoryRepository.getCategories() } returns flowOf(categories)
        every { categoryRepository.getMangaIdsByCategoryId(1L) } returns flowOf(listOf(10L, 20L, 30L))
        every { categoryRepository.getMangaIdsByCategoryId(2L) } returns flowOf(listOf(40L))

        useCase().test {
            val result = awaitItem()
            assertEquals(2, result.size)

            val fav = result.first { it.id == 1L }
            assertEquals(3, fav.mangaCount)

            val reading = result.first { it.id == 2L }
            assertEquals(1, reading.mangaCount)

            awaitComplete()
        }
    }

    @Test
    fun `getMangaIdsForCategory delegates to repository`() = runTest {
        val ids = listOf(1L, 2L, 3L)
        every { categoryRepository.getMangaIdsByCategoryId(5L) } returns flowOf(ids)

        useCase.getMangaIdsForCategory(5L).test {
            assertEquals(ids, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getMangaIdsForCategory with empty category returns empty list`() = runTest {
        every { categoryRepository.getMangaIdsByCategoryId(99L) } returns flowOf(emptyList())

        useCase.getMangaIdsForCategory(99L).test {
            assertEquals(emptyList<Long>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `invoke with single category having no manga returns count of zero`() = runTest {
        val category = Category(id = 1L, name = "Empty", order = 0)
        every { categoryRepository.getCategories() } returns flowOf(listOf(category))
        every { categoryRepository.getMangaIdsByCategoryId(1L) } returns flowOf(emptyList())

        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(0, result[0].mangaCount)
            awaitComplete()
        }
    }
}
