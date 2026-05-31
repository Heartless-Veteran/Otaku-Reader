package app.otakureader.feature.library.category

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.CategoryUpdateFrequency

data class CategoryUiItem(
    val id: Long,
    val name: String,
    val mangaCount: Int,
    val isHidden: Boolean,
    val isNsfw: Boolean,
    val isLocked: Boolean = false,
    val isDynamic: Boolean = false,
    val updateFrequency: CategoryUpdateFrequency = CategoryUpdateFrequency.DAILY,
)

data class CategoryManagementState(
    val categories: List<CategoryUiItem> = emptyList(),
    val isLoading: Boolean = false,
    /** Whether hidden categories are currently revealed (after biometric unlock). */
    val hiddenRevealed: Boolean = false,
) : UiState {
    /** True when at least one category is hidden (so the reveal control is worth showing). */
    val hasHiddenCategories: Boolean get() = categories.any { it.isHidden }
}

sealed interface CategoryEvent : UiEvent {
    data class CreateCategory(
        val name: String,
        val frequency: CategoryUpdateFrequency = CategoryUpdateFrequency.DAILY,
    ) : CategoryEvent
    data class UpdateCategory(
        val categoryId: Long,
        val name: String,
        val frequency: CategoryUpdateFrequency,
    ) : CategoryEvent
    data class DeleteCategory(val categoryId: Long) : CategoryEvent
    data class ToggleHidden(val categoryId: Long) : CategoryEvent
    data class ToggleNsfw(val categoryId: Long) : CategoryEvent
    data class ToggleLocked(val categoryId: Long) : CategoryEvent
    data class SetDynamic(val categoryId: Long, val enabled: Boolean) : CategoryEvent
    /** Reveal or re-hide hidden categories (reveal should be gated by biometric in the UI). */
    data class SetHiddenRevealed(val revealed: Boolean) : CategoryEvent
}

sealed interface CategoryEffect : UiEffect {
    data class ShowSnackbar(val message: String) : CategoryEffect
    data object DismissDialog : CategoryEffect
}
