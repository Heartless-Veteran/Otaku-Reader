package app.otakureader.data.repository

import app.otakureader.core.database.dao.CategoryDao
import app.otakureader.core.database.entity.CategoryEntity
import app.otakureader.core.database.entity.MangaCategoryEntity
import app.otakureader.domain.model.Category
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CategoryRepositoryImplTest {

    private lateinit var categoryDao: CategoryDao
    private lateinit var repository: CategoryRepositoryImpl

    @Before
    fun setUp() {
        categoryDao = mockk()
        repository = CategoryRepositoryImpl(categoryDao)
    }

    // ---- getCategories ----

    @Test
    fun getCategories_returnsMappedDomainCategories() = runTest {
        val entities = listOf(
            CategoryEntity(id = 1L, name = "Favorites", order = 0),
            CategoryEntity(id = 2L, name = "Reading", order = 1)
        )
        every { categoryDao.getCategories() } returns flowOf(entities)

        repository.getCategories().test {
            val categories = awaitItem()
            assertEquals(2, categories.size)
            assertEquals("Favorites", categories[0].name)
            assertEquals("Reading", categories[1].name)
            awaitComplete()
        }
    }

    // ---- getCategoryById ----

    @Test
    fun getCategoryById_existingId_returnsMappedCategory() = runTest {
        coEvery { categoryDao.getCategoryById(1L) } returns CategoryEntity(1L, "Favorites", 0)

        val category = repository.getCategoryById(1L)

        assertEquals(1L, category?.id)
        assertEquals("Favorites", category?.name)
    }

    @Test
    fun getCategoryById_missingId_returnsNull() = runTest {
        coEvery { categoryDao.getCategoryById(999L) } returns null

        val category = repository.getCategoryById(999L)

        assertNull(category)
    }

    // ---- createCategory ----

    @Test
    fun createCategory_withNoExistingCategories_createsWithOrderOne() = runTest {
        coEvery { categoryDao.getMaxCategoryOrder() } returns 0
        coEvery { categoryDao.insert(any()) } returns 1L

        val id = repository.createCategory("New Category")

        assertEquals(1L, id)
        coVerify {
            categoryDao.insert(match { it.name == "New Category" && it.order == 1 })
        }
    }

    @Test
    fun createCategory_withExistingCategories_incrementsMaxOrder() = runTest {
        coEvery { categoryDao.getMaxCategoryOrder() } returns 3
        coEvery { categoryDao.insert(any()) } returns 10L

        val id = repository.createCategory("Extra")

        assertEquals(10L, id)
        coVerify {
            categoryDao.insert(match { it.name == "Extra" && it.order == 4 })
        }
    }

    // ---- updateCategory ----

    @Test
    fun updateCategory_callsDaoWithEntityConversion() = runTest {
        val category = Category(id = 1L, name = "Updated", order = 2)
        coEvery { categoryDao.update(any()) } returns Unit

        repository.updateCategory(category)

        coVerify {
            categoryDao.update(match { it.id == 1L && it.name == "Updated" && it.order == 2 })
        }
    }

    // ---- deleteCategory ----

    @Test
    fun deleteCategory_callsDaoDeleteById() = runTest {
        coEvery { categoryDao.deleteById(5L) } returns Unit

        repository.deleteCategory(5L)

        coVerify { categoryDao.deleteById(5L) }
    }

    // ---- addMangaToCategory ----

    @Test
    fun addMangaToCategory_insertsCorrectRelation() = runTest {
        coEvery { categoryDao.insertMangaCategory(any()) } returns Unit

        repository.addMangaToCategory(mangaId = 10L, categoryId = 2L)

        coVerify {
            categoryDao.insertMangaCategory(MangaCategoryEntity(mangaId = 10L, categoryId = 2L))
        }
    }

    // ---- removeMangaFromCategory ----

    @Test
    fun removeMangaFromCategory_callsDaoWithCorrectArgs() = runTest {
        coEvery { categoryDao.deleteMangaCategory(any(), any()) } returns Unit

        repository.removeMangaFromCategory(mangaId = 10L, categoryId = 2L)

        coVerify { categoryDao.deleteMangaCategory(10L, 2L) }
    }

    // ---- getMangaIdsByCategoryId ----

    @Test
    fun getMangaIdsByCategoryId_returnsMangaIdsForCategory() = runTest {
        val categoryId = 3L
        val mangaIds = listOf(1L, 2L, 5L)
        every { categoryDao.getMangaIdsByCategoryId(categoryId) } returns flowOf(mangaIds)

        repository.getMangaIdsByCategoryId(categoryId).test {
            assertEquals(mangaIds, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun getMangaIdsByCategoryId_emptyCategory_returnsEmptyList() = runTest {
        val categoryId = 99L
        every { categoryDao.getMangaIdsByCategoryId(categoryId) } returns flowOf(emptyList())

        repository.getMangaIdsByCategoryId(categoryId).test {
            assertEquals(emptyList<Long>(), awaitItem())
            awaitComplete()
        }
    }
}
