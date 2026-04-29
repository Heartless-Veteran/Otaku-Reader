package app.otakureader.feature.library.category.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.library.category.CategoryManagementScreen

fun NavGraphBuilder.categoryManagementScreen(
    onNavigateBack: () -> Unit
) {
    composable<Route.CategoryManagement> {
        CategoryManagementScreen(
            onNavigateBack = onNavigateBack
        )
    }
}
