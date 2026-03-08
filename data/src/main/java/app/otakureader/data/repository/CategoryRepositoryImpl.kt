package app.otakureader.data.repository

import app.otakureader.core.database.dao.CategoryDao
import app.otakureader.core.database.entity.CategoryEntity
import app.otakureader.core.database.entity.MangaCategoryEntity
import app.otakureader.domain.model.Category
import app.otakureader.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    
    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }
    
    override suspend fun createCategory(name: String): Long {
        val maxOrder = categoryDao.getMaxCategoryOrder()
        val entity = CategoryEntity(name = name, order = maxOrder + 1)
        return categoryDao.insert(entity)
    }
    
    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }
    
    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteById(id)
    }
    
    override suspend fun addMangaToCategory(mangaId: Long, categoryId: Long) {
        categoryDao.insertMangaCategory(MangaCategoryEntity(mangaId, categoryId))
    }
    
    override suspend fun removeMangaFromCategory(mangaId: Long, categoryId: Long) {
        categoryDao.deleteMangaCategory(mangaId, categoryId)
    }
    
    override fun getMangaIdsByCategoryId(categoryId: Long): Flow<List<Long>> {
        return categoryDao.getMangaIdsByCategoryId(categoryId)
    }
    
    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        order = order
    )
    
    private fun Category.toEntity() = CategoryEntity(
        id = id,
        name = name,
        order = order
    )
}
