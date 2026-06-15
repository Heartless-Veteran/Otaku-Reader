package app.otakureader.domain.usecase

import app.otakureader.domain.repository.CategoryRepository
import app.otakureader.domain.repository.DynamicCategoryRepository
import app.otakureader.domain.repository.MangaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Applies all dynamic category rules to automatically reorganize the library.
 *
 * This implements the "Clean Up My Library" feature: iterates through all
 * dynamic categories, evaluates their rules against the current library,
 * and updates manga category assignments based on matches.
 *
 * Pure business logic—no side effects beyond the repository calls.
 */
class ApplyDynamicCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val dynamicCategoryRepository: DynamicCategoryRepository,
    private val mangaRepository: MangaRepository,
    private val evaluateRule: EvaluateDynamicCategoryUseCase,
) {

    data class Result(
        val totalCategories: Int,
        val categoriesApplied: Int,
        val mangaUpdated: Int,
    )

    /**
     * Apply all dynamic categories to the library.
     *
     * For each dynamic category:
     * 1. Fetch its rules
     * 2. Evaluate rules against the current library
     * 3. Add matched manga to the category
     *
     * @return Summary of operation: categories processed, manga updated
     */
    suspend operator fun invoke(): Result {
        val allCategories = categoryRepository.getCategories().first()
        val dynamicCategories = allCategories.filter { it.isDynamic }

        if (dynamicCategories.isEmpty()) {
            return Result(totalCategories = 0, categoriesApplied = 0, mangaUpdated = 0)
        }

        val libraryManga = mangaRepository.getLibraryManga().first()
        var totalUpdated = 0

        for (category in dynamicCategories) {
            val rules = dynamicCategoryRepository.getRulesForCategory(category.id).first()
            if (rules.isEmpty()) continue

            val matchedMangaIds = evaluateRule(rules, libraryManga)
            if (matchedMangaIds.isNotEmpty()) {
                // Get current category assignments for matched manga
                for (mangaId in matchedMangaIds) {
                    val currentCategories = mangaRepository.getCategoryIdsForManga(mangaId)
                    // Add category if not already there
                    if (!currentCategories.contains(category.id)) {
                        mangaRepository.addMangaToCategory(mangaId, category.id)
                        totalUpdated++
                    }
                }
            }
        }

        return Result(
            totalCategories = dynamicCategories.size,
            categoriesApplied = dynamicCategories.size,
            mangaUpdated = totalUpdated,
        )
    }
}
