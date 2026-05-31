package app.otakureader.feature.browse.repos.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.browse.repos.ExtensionRepositoriesScreen

fun NavGraphBuilder.extensionRepositoriesScreen(
    onNavigateBack: () -> Unit,
) {
    composable<Route.ExtensionRepositories> {
        ExtensionRepositoriesScreen(onNavigateBack = onNavigateBack)
    }
}
