package app.otakureader.domain.repository

import app.otakureader.domain.model.Category
import app.otakureader.domain.model.CategoryUpdateFrequency
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    fun getVisibleCategories(): Flow<List<Category>>
    fun getHiddenCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun createCategory(name: String, frequency: CategoryUpdateFrequency = CategoryUpdateFrequency.DAILY): Long
    suspend fun updateCategory(category: Category)
    suspend fun updateCategoryFrequency(categoryId: Long, frequency: CategoryUpdateFrequency)
    suspend fun deleteCategory(id: Long)
    suspend fun addMangaToCategory(mangaId: Long, categoryId: Long)
    suspend fun removeMangaFromCategory(mangaId: Long, categoryId: Long)
    fun getMangaIdsByCategoryId(categoryId: Long): Flow<List<Long>>
    suspend fun toggleCategoryHidden(categoryId: Long)
    suspend fun toggleCategoryNsfw(categoryId: Long)
    suspend fun toggleCategoryLocked(categoryId: Long)
}
