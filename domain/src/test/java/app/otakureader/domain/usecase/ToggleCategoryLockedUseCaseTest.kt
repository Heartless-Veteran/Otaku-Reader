package app.otakureader.domain.usecase

import app.otakureader.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ToggleCategoryLockedUseCaseTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var useCase: ToggleCategoryLockedUseCase

    @Before
    fun setUp() {
        categoryRepository = mockk()
        useCase = ToggleCategoryLockedUseCase(categoryRepository)
    }

    @Test
    fun invoke_delegatesToRepository() = runTest {
        val categoryId = 7L
        coEvery { categoryRepository.toggleCategoryLocked(categoryId) } returns Unit

        useCase(categoryId)

        coVerify(exactly = 1) { categoryRepository.toggleCategoryLocked(categoryId) }
    }

    @Test
    fun invoke_withDifferentId_passesCorrectId() = runTest {
        val categoryId = 99L
        coEvery { categoryRepository.toggleCategoryLocked(categoryId) } returns Unit

        useCase(categoryId)

        coVerify(exactly = 1) { categoryRepository.toggleCategoryLocked(99L) }
    }
}
