package app.otakureader.data.repository

import app.otakureader.core.database.dao.DynamicCategoryRuleDao
import app.otakureader.core.database.entity.DynamicCategoryRuleEntity
import app.otakureader.domain.model.DynamicCategoryRule
import app.otakureader.domain.repository.DynamicCategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicCategoryRepositoryImpl @Inject constructor(
    private val dao: DynamicCategoryRuleDao,
) : DynamicCategoryRepository {

    override fun getRulesForCategory(categoryId: Long): Flow<List<DynamicCategoryRule>> =
        dao.getRulesForCategory(categoryId).map { entities -> entities.mapNotNull { it.toDomain() } }

    override suspend fun setRules(categoryId: Long, rules: List<DynamicCategoryRule>) {
        dao.deleteForCategory(categoryId)
        dao.insertAll(rules.map { it.toEntity(categoryId) })
    }

    override suspend fun clearRules(categoryId: Long) {
        dao.deleteForCategory(categoryId)
    }

    override suspend fun hasDynamicRules(categoryId: Long): Boolean =
        dao.countForCategory(categoryId) > 0

    private fun DynamicCategoryRuleEntity.toDomain(): DynamicCategoryRule? = when (ruleType) {
        DynamicCategoryRule.TYPE_UNREAD_AT_LEAST ->
            DynamicCategoryRule.UnreadAtLeast(ruleParamsJson.toIntOrNull() ?: return null)
        DynamicCategoryRule.TYPE_RECENTLY_UPDATED ->
            DynamicCategoryRule.RecentlyUpdated(ruleParamsJson.toIntOrNull() ?: return null)
        DynamicCategoryRule.TYPE_GENRE_CONTAINS ->
            DynamicCategoryRule.GenreContains(ruleParamsJson)
        else -> null
    }

    private fun DynamicCategoryRule.toEntity(categoryId: Long) = DynamicCategoryRuleEntity(
        categoryId = categoryId,
        ruleType = when (this) {
            is DynamicCategoryRule.UnreadAtLeast -> DynamicCategoryRule.TYPE_UNREAD_AT_LEAST
            is DynamicCategoryRule.RecentlyUpdated -> DynamicCategoryRule.TYPE_RECENTLY_UPDATED
            is DynamicCategoryRule.GenreContains -> DynamicCategoryRule.TYPE_GENRE_CONTAINS
        },
        ruleParamsJson = when (this) {
            is DynamicCategoryRule.UnreadAtLeast -> count.toString()
            is DynamicCategoryRule.RecentlyUpdated -> withinDays.toString()
            is DynamicCategoryRule.GenreContains -> genre
        }
    )
}
