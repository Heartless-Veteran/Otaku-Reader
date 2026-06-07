package app.otakureader.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.library.maintenance.LibraryMaintenanceScreen

fun NavGraphBuilder.libraryMaintenanceScreen(
    onNavigateBack: () -> Unit,
) {
    composable<Route.LibraryMaintenance> {
        LibraryMaintenanceScreen(onNavigateBack = onNavigateBack)
    }
}
