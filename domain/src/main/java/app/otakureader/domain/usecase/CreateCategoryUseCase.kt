package app.otakureader.domain.usecase

import app.otakureader.domain.model.CategoryUpdateFrequency
import app.otakureader.domain.repository.CategoryRepository
import javax.inject.Inject

class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        name: String,
        frequency: CategoryUpdateFrequency = CategoryUpdateFrequency.DAILY,
    ): Long {
        return categoryRepository.createCategory(name.trim(), frequency)
    }
}
