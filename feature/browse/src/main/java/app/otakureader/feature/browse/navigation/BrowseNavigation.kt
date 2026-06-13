package app.otakureader.feature.browse.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.otakureader.core.navigation.Route
import app.otakureader.feature.browse.BrowseScreen
import app.otakureader.feature.browse.ExtensionsBottomSheet
import app.otakureader.feature.browse.GlobalSearchScreen
import app.otakureader.feature.browse.SourceMangaDetailScreen
import app.otakureader.feature.browse.SourceMangaScreen
import app.otakureader.feature.browse.extension.ExtensionInstallScreen
import app.otakureader.feature.browse.extension.extensionDetailScreen

@Suppress("UnusedParameter")
fun NavGraphBuilder.browseScreen(
    onMangaClick: (sourceId: String, mangaUrl: String, mangaTitle: String) -> Unit,
    onNavigateToSource: (sourceId: String) -> Unit,
    onNavigateToExtensions: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToOpds: () -> Unit = {},
    onNavigateToMigration: () -> Unit = {},
) {
    composable<Route.Browse> {
        BrowseScreen(
            viewModel = hiltViewModel(),
            onMangaClick = { sourceId, mangaUrl ->
                onMangaClick(sourceId, mangaUrl, "")
            },
            onInstallExtensionClick = onNavigateToExtensions,
            onGlobalSearchClick = onNavigateToGlobalSearch,
            onOpdsClick = onNavigateToOpds,
            onNavigateToMigration = onNavigateToMigration,
        )
    }
}

fun NavGraphBuilder.sourceDetailScreen(
    onMangaClick: (sourceId: String, mangaUrl: String, mangaTitle: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable<Route.SourceListing> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.SourceListing>()
        SourceMangaScreen(
            sourceId = route.sourceId.toString(),
            onMangaClick = { mangaUrl, mangaTitle ->
                onMangaClick(route.sourceId.toString(), mangaUrl, mangaTitle)
            },
            onNavigateBack = onNavigateBack
        )
    }
}

fun NavGraphBuilder.sourceMangaDetailScreen(
    onNavigateToMangaDetail: (mangaId: Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable<Route.SourceMangaDetail> {
        SourceMangaDetailScreen(
            onNavigateToMangaDetail = onNavigateToMangaDetail,
            onNavigateBack = onNavigateBack,
        )
    }
}

fun NavGraphBuilder.extensionsBottomSheet(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRepositories: () -> Unit = {},
    onNavigateToExtensionDetail: (packageName: String) -> Unit = {},
) {
    composable<Route.ExtensionCatalog>(
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        ExtensionsBottomSheet(
            onDismiss = onDismiss,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToRepositories = onNavigateToRepositories,
            onNavigateToExtensionDetail = onNavigateToExtensionDetail,
        )
    }
}

fun NavGraphBuilder.extensionInstallScreen(
    onNavigateBack: () -> Unit,
) {
    composable<Route.ExtensionInstall> {
        ExtensionInstallScreen(
            onBackClick = onNavigateBack
        )
    }
}

fun NavGraphBuilder.globalSearchScreen(
    onMangaClick: (sourceId: String, mangaUrl: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable<Route.Search> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Search>()
        GlobalSearchScreen(
            initialQuery = route.query,
            onMangaClick = onMangaClick,
            onNavigateBack = onNavigateBack,
            viewModel = hiltViewModel()
        )
    }
}

/**
 * Registers the extension detail full-screen destination in the calling nav graph.
 *
 * Wire this inside the app-level NavHost alongside the other browse destinations,
 * passing a lambda that navigates back (typically `navController::popBackStack`).
 */
fun NavGraphBuilder.browseExtensionDetailScreen(
    onNavigateBack: () -> Unit,
) {
    extensionDetailScreen(onNavigateBack = onNavigateBack)
}
