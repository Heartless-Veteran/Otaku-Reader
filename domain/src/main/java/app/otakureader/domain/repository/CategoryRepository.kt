package app.otakureader.domain.repository

import app.otakureader.domain.model.Category
import app.otakureader.domain.model.Manga
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun createCategory(name: String): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: Long)
    suspend fun addMangaToCategory(mangaId: Long, categoryId: Long)
    suspend fun removeMangaFromCategory(mangaId: Long, categoryId: Long)
    fun getMangaIdsByCategoryId(categoryId: Long): Flow<List<Long>>
}
