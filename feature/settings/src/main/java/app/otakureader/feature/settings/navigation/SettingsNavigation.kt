package app.otakureader.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.settings.SettingsScreen
import app.otakureader.feature.settings.settingsAppearanceScreen
import app.otakureader.feature.settings.settingsBackupScreen
import app.otakureader.feature.settings.settingsDownloadsScreen
import app.otakureader.feature.settings.settingsLibraryScreen
import app.otakureader.feature.settings.settingsReaderScreen
import app.otakureader.feature.settings.settingsTrackingScreen

/**
 * Registers the settings hub and all settings sub-screen destinations in the NavGraph.
 *
 * Each sub-screen is backed by its own focused ViewModel so that only the preferences
 * relevant to that screen are observed and written.
 */
fun NavGraphBuilder.settingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToMigrationEntry: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToReader: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToTracking: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
) {
    composable<Route.Settings> {
        SettingsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToMigrationEntry = onNavigateToMigrationEntry,
            onNavigateToAppearance = onNavigateToAppearance,
            onNavigateToLibrary = onNavigateToLibrary,
            onNavigateToReader = onNavigateToReader,
            onNavigateToDownloads = onNavigateToDownloads,
            onNavigateToTracking = onNavigateToTracking,
            onNavigateToBackup = onNavigateToBackup,
        )
    }

    settingsAppearanceScreen(onNavigateBack = onNavigateBack)
    settingsLibraryScreen(onNavigateBack = onNavigateBack)
    settingsReaderScreen(onNavigateBack = onNavigateBack)
    settingsDownloadsScreen(onNavigateBack = onNavigateBack)
    settingsTrackingScreen(onNavigateBack = onNavigateBack)
    settingsBackupScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToMigrationEntry = onNavigateToMigrationEntry,
    )
}
