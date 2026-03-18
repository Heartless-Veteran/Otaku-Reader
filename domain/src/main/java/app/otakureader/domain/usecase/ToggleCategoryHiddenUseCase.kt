package app.otakureader.domain.usecase

import app.otakureader.domain.repository.CategoryRepository
import javax.inject.Inject

class ToggleCategoryHiddenUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(categoryId: Long) {
        categoryRepository.toggleCategoryHidden(categoryId)
    }
}
