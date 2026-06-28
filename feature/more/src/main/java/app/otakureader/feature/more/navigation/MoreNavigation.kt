package app.otakureader.feature.more.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.otakureader.core.navigation.Route
import app.otakureader.domain.model.ShareableLibrary
import app.otakureader.feature.more.MoreScreen
import app.otakureader.feature.more.qr.ScanLibraryScreen
import app.otakureader.feature.more.qr.ShareLibraryScreen

fun NavGraphBuilder.moreScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToExtensions: () -> Unit = {},
    onNavigateToFeed: () -> Unit = {},
    onNavigateToShareLibrary: () -> Unit = {},
    onNavigateToScanLibrary: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToUpdateErrors: () -> Unit = {},
    onNavigateToReadingLists: () -> Unit = {},
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
) {
    composable<Route.More> {
        MoreScreen(
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDownloads = onNavigateToDownloads,
            onNavigateToStatistics = onNavigateToStatistics,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToExtensions = onNavigateToExtensions,
            onNavigateToFeed = onNavigateToFeed,
            onNavigateToShareLibrary = onNavigateToShareLibrary,
            onNavigateToScanLibrary = onNavigateToScanLibrary,
            onNavigateToBackup = onNavigateToBackup,
            onNavigateToUpdateErrors = onNavigateToUpdateErrors,
            onNavigateToReadingLists = onNavigateToReadingLists,
            onNavigateToBookmarks = onNavigateToBookmarks,
            onNavigateToCategories = onNavigateToCategories,
        )
    }
}

fun NavGraphBuilder.shareLibraryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanLibrary: () -> Unit,
) {
    composable<Route.ShareLibrary> {
        ShareLibraryScreen(
            onNavigateBack = onNavigateBack,
            onScanLibrary = onNavigateToScanLibrary,
        )
    }
}

fun NavGraphBuilder.scanLibraryScreen(
    onNavigateBack: () -> Unit,
    onLibraryScanned: (ShareableLibrary) -> Unit,
) {
    composable<Route.ScanLibrary> {
        ScanLibraryScreen(
            onNavigateBack = onNavigateBack,
            onLibraryScanned = onLibraryScanned,
        )
    }
}
