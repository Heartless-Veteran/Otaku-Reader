package app.otakureader.core.ui.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Collect one-shot effects from a [BaseMviViewModel] in Compose.
 *
 * Usage:
 * ```
 * val viewModel: LibraryViewModel = hiltViewModel()
 * val state by viewModel.state.collectAsStateWithLifecycle()
 *
 * CollectAsEffect(viewModel.effect) { effect ->
 *     when (effect) {
 *         is LibraryUiEffect.NavigateToDetails -> navigator.navigate(effect.route)
 *         is LibraryUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
 *     }
 * }
 * ```
 *
 * @param effectFlow The [Flow] of effects from the ViewModel.
 * @param collector Lambda invoked for each effect. Runs in a [LaunchedEffect] scoped to the composable.
 */
@Composable
fun <F : app.otakureader.core.common.mvi.UiEffect> CollectAsEffect(
    effectFlow: Flow<F>,
    collector: suspend (F) -> Unit,
) {
    LaunchedEffect(effectFlow) {
        effectFlow.collectLatest(collector)
    }
}
