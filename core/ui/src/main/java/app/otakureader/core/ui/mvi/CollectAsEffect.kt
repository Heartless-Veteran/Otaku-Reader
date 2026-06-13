package app.otakureader.core.ui.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

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
        // Use collect, not collectLatest: effects are one-shot events (navigation, snackbars).
        // collectLatest cancels an in-flight collector when a new effect arrives, so a suspending
        // handler (e.g. snackbarHostState.showSnackbar, which suspends until dismissed) would be
        // cancelled mid-way and that effect lost. collect processes each effect to completion.
        effectFlow.collect(collector)
    }
}
