package app.otakureader.feature.about.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.about.AboutScreen

fun NavGraphBuilder.aboutScreen(
    onNavigateBack: () -> Unit,
) {
    composable<Route.About> {
        AboutScreen(
            onNavigateBack = onNavigateBack
        )
    }
}
