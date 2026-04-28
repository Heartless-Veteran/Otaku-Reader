package app.otakureader.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.settings.SettingsScreen

fun NavGraphBuilder.settingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMigrationEntry: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
) {
    composable<Route.Settings> {
        SettingsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToMigrationEntry = onNavigateToMigrationEntry,
            onNavigateToAbout = onNavigateToAbout
        )
    }
}
