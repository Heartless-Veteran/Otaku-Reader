package app.otakureader.domain.repository

import app.otakureader.domain.model.DynamicCategoryRule
import kotlinx.coroutines.flow.Flow

interface DynamicCategoryRepository {
    fun getRulesForCategory(categoryId: Long): Flow<List<DynamicCategoryRule>>
    suspend fun setRules(categoryId: Long, rules: List<DynamicCategoryRule>)
    suspend fun clearRules(categoryId: Long)
    suspend fun hasDynamicRules(categoryId: Long): Boolean
}
