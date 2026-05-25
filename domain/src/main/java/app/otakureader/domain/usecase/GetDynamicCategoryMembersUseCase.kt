package app.otakureader.domain.usecase

import app.otakureader.domain.model.DynamicCategoryRule
import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.DynamicCategoryRepository
import app.otakureader.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Returns a live set of manga IDs that satisfy all dynamic rules for a given category.
 *
 * Rules are AND-combined: a manga must match every rule to be included.
 * When the rule list is empty the result is always empty (a dynamic category
 * with no rules configured shows nothing).
 */
class GetDynamicCategoryMembersUseCase @Inject constructor(
    private val dynamicCategoryRepository: DynamicCategoryRepository,
    private val mangaRepository: MangaRepository,
) {
    operator fun invoke(categoryId: Long): Flow<Set<Long>> {
        return dynamicCategoryRepository.getRulesForCategory(categoryId)
            .flatMapLatest { rules ->
                if (rules.isEmpty()) {
                    flowOf(emptySet())
                } else {
                    mangaRepository.getLibraryManga().combine(flowOf(rules)) { manga, activeRules ->
                        manga.filter { it.satisfiesAllRules(activeRules) }.map { it.id }.toSet()
                    }
                }
            }
    }

    private fun Manga.satisfiesAllRules(rules: List<DynamicCategoryRule>): Boolean =
        rules.all { rule ->
            when (rule) {
                is DynamicCategoryRule.UnreadAtLeast -> unreadCount >= rule.count
                is DynamicCategoryRule.RecentlyUpdated -> {
                    val cutoff = System.currentTimeMillis() - rule.withinDays * DAY_MS
                    (lastRead ?: 0L) >= cutoff
                }
                is DynamicCategoryRule.GenreContains ->
                    genre.any { it.contains(rule.genre, ignoreCase = true) }
            }
        }

    private companion object {
        private const val DAY_MS = 86_400_000L
    }
}
