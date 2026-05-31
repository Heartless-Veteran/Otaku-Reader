package app.otakureader.domain.usecase

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
 *
 * Membership matching is delegated to [EvaluateDynamicCategoryUseCase] so the rule logic
 * lives in one place (and can be unit tested without coroutines).
 */
class GetDynamicCategoryMembersUseCase @Inject constructor(
    private val dynamicCategoryRepository: DynamicCategoryRepository,
    private val mangaRepository: MangaRepository,
    private val evaluateDynamicCategory: EvaluateDynamicCategoryUseCase,
) {
    operator fun invoke(categoryId: Long): Flow<Set<Long>> {
        return dynamicCategoryRepository.getRulesForCategory(categoryId)
            .flatMapLatest { rules ->
                if (rules.isEmpty()) {
                    flowOf(emptySet())
                } else {
                    mangaRepository.getLibraryManga().combine(flowOf(rules)) { manga, activeRules ->
                        evaluateDynamicCategory(activeRules, manga)
                    }
                }
            }
    }
}
