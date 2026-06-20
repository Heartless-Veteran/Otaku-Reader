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
        DynamicCategoryRule.TYPE_COMPLETED -> DynamicCategoryRule.Completed
        DynamicCategoryRule.TYPE_ONGOING -> DynamicCategoryRule.Ongoing
        DynamicCategoryRule.TYPE_RECENTLY_ADDED ->
            DynamicCategoryRule.RecentlyAdded(ruleParamsJson.toIntOrNull() ?: return null)
        DynamicCategoryRule.TYPE_NEVER_STARTED -> DynamicCategoryRule.NeverStarted
        DynamicCategoryRule.TYPE_READ_WITHIN_DAYS ->
            DynamicCategoryRule.ReadWithinDays(ruleParamsJson.toIntOrNull() ?: return null)
        DynamicCategoryRule.TYPE_NOT_READ_IN_DAYS ->
            DynamicCategoryRule.NotReadInDays(ruleParamsJson.toIntOrNull() ?: return null)
        DynamicCategoryRule.TYPE_MARKED_COMPLETED -> DynamicCategoryRule.MarkedCompleted
        DynamicCategoryRule.TYPE_MARKED_DROPPED -> DynamicCategoryRule.MarkedDropped
        else -> null
    }

    private fun DynamicCategoryRule.toEntity(categoryId: Long) = DynamicCategoryRuleEntity(
        categoryId = categoryId,
        ruleType = when (this) {
            is DynamicCategoryRule.UnreadAtLeast -> DynamicCategoryRule.TYPE_UNREAD_AT_LEAST
            is DynamicCategoryRule.RecentlyUpdated -> DynamicCategoryRule.TYPE_RECENTLY_UPDATED
            is DynamicCategoryRule.GenreContains -> DynamicCategoryRule.TYPE_GENRE_CONTAINS
            is DynamicCategoryRule.Completed -> DynamicCategoryRule.TYPE_COMPLETED
            is DynamicCategoryRule.Ongoing -> DynamicCategoryRule.TYPE_ONGOING
            is DynamicCategoryRule.RecentlyAdded -> DynamicCategoryRule.TYPE_RECENTLY_ADDED
            is DynamicCategoryRule.NeverStarted -> DynamicCategoryRule.TYPE_NEVER_STARTED
            is DynamicCategoryRule.ReadWithinDays -> DynamicCategoryRule.TYPE_READ_WITHIN_DAYS
            is DynamicCategoryRule.NotReadInDays -> DynamicCategoryRule.TYPE_NOT_READ_IN_DAYS
            is DynamicCategoryRule.MarkedCompleted -> DynamicCategoryRule.TYPE_MARKED_COMPLETED
            is DynamicCategoryRule.MarkedDropped -> DynamicCategoryRule.TYPE_MARKED_DROPPED
        },
        ruleParamsJson = when (this) {
            is DynamicCategoryRule.UnreadAtLeast -> count.toString()
            is DynamicCategoryRule.RecentlyUpdated -> withinDays.toString()
            is DynamicCategoryRule.GenreContains -> genre
            is DynamicCategoryRule.Completed -> ""
            is DynamicCategoryRule.Ongoing -> ""
            is DynamicCategoryRule.RecentlyAdded -> withinDays.toString()
            is DynamicCategoryRule.NeverStarted -> ""
            is DynamicCategoryRule.ReadWithinDays -> withinDays.toString()
            is DynamicCategoryRule.NotReadInDays -> withinDays.toString()
            is DynamicCategoryRule.MarkedCompleted -> ""
            is DynamicCategoryRule.MarkedDropped -> ""
        }
    )
}
