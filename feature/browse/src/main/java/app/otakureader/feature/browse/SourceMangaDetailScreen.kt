package app.otakureader.feature.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Transparent redirect screen for [Route.SourceMangaDetail].
 *
 * Shows a loading spinner while [SourceMangaDetailViewModel] resolves the manga's
 * database ID (looking it up by source URL, or inserting a stub entry), then
 * immediately forwards to the [Route.MangaDetails] screen via [onNavigateToMangaDetail].
 *
 * If the lookup fails or times out the screen calls [onNavigateBack] so the user
 * is never left on an infinite loading spinner (fixes #901).
 */
@Composable
fun SourceMangaDetailScreen(
    onNavigateToMangaDetail: (mangaId: Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SourceMangaDetailViewModel = hiltViewModel(),
) {
    val redirectState by viewModel.redirectState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val notFoundMessage = stringResource(R.string.source_manga_detail_not_found)
    val errorPrefix = stringResource(R.string.source_manga_detail_error_prefix)

    LaunchedEffect(redirectState) {
        when (val state = redirectState) {
            is RedirectState.Success -> {
                onNavigateToMangaDetail(state.mangaId)
            }
            is RedirectState.NotFound -> {
                snackbarHostState.showSnackbar(notFoundMessage)
                onNavigateBack()
            }
            is RedirectState.Error -> {
                // Concatenate prefix with detail message for a user-readable string.
                // TODO: replace with a proper string resource once UX copy is finalised.
                snackbarHostState.showSnackbar("$errorPrefix: ${state.message}")
                onNavigateBack()
            }
            RedirectState.Loading -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
