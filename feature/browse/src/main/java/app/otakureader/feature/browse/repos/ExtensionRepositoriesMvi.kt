package app.otakureader.feature.browse.repos

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState

data class RepositoryItem(
    val url: String,
    /** True when this is the bundled default repo (per ExtensionRepoRepository.DEFAULT_REPO_URL). */
    val isDefault: Boolean,
)

data class ExtensionRepositoriesState(
    val repositories: List<RepositoryItem> = emptyList(),
    val isLoading: Boolean = true,
    /** Active validation error for the add-URL field; null when input is acceptable. */
    val urlValidationError: String? = null,
) : UiState

sealed interface ExtensionRepositoriesEvent : UiEvent {
    data class AddRepository(val url: String) : ExtensionRepositoriesEvent
    data class RemoveRepository(val url: String) : ExtensionRepositoriesEvent
    /** Live validation as the user types; populates state.urlValidationError. */
    data class ValidateUrl(val url: String) : ExtensionRepositoriesEvent
}

sealed interface ExtensionRepositoriesEffect : UiEffect {
    data class ShowSnackbar(val message: String) : ExtensionRepositoriesEffect
}
