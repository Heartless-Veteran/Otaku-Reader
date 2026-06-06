package app.otakureader.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.library.MergeDuplicatesScreen

fun NavGraphBuilder.mergeDuplicatesScreen(
    onNavigateBack: () -> Unit,
) {
    composable<Route.MergeDuplicates> {
        MergeDuplicatesScreen(onNavigateBack = onNavigateBack)
    }
}
