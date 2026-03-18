package app.otakureader.domain.usecase

import app.otakureader.domain.model.Category
import app.otakureader.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVisibleCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return categoryRepository.getVisibleCategories()
    }
}
