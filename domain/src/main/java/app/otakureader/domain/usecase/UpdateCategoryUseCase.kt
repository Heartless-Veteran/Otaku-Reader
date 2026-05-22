package app.otakureader.domain.usecase

import app.otakureader.domain.model.CategoryUpdateFrequency
import app.otakureader.domain.repository.CategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        categoryId: Long,
        name: String,
        frequency: CategoryUpdateFrequency? = null,
    ) {
        val category = categoryRepository.getCategoryById(categoryId) ?: return
        categoryRepository.updateCategory(
            category.copy(
                name = name.trim(),
                updateFrequency = frequency ?: category.updateFrequency,
            )
        )
    }
}
