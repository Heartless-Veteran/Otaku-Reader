package app.otakureader.feature.migration.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.otakureader.core.navigation.Route
import app.otakureader.feature.migration.MigrationEntryScreen
import app.otakureader.feature.migration.MigrationScreen

fun NavGraphBuilder.migrationScreen(
    onNavigateBack: () -> Unit
) {
    composable<Route.Migration> { backStackEntry ->
        val migrationRoute = backStackEntry.toRoute<Route.Migration>()
        MigrationScreen(
            selectedMangaIds = migrationRoute.selectedMangaIds,
            onNavigateBack = onNavigateBack
        )
    }
}

fun NavGraphBuilder.migrationEntryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMigration: (List<Long>) -> Unit
) {
    composable<Route.MigrationEntry> {
        MigrationEntryScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToMigration = onNavigateToMigration
        )
    }
}
