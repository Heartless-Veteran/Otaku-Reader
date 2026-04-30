package app.otakureader.feature.opds.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.otakureader.core.navigation.Route
import app.otakureader.feature.opds.OpdsScreen

fun NavGraphBuilder.opdsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMangaDetail: (mangaUrl: String, mangaTitle: String) -> Unit,
) {
    composable<Route.OpdsCatalog> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.OpdsCatalog>()
        OpdsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToMangaDetail = onNavigateToMangaDetail
        )
    }
}
