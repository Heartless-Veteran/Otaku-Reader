package app.otakureader.feature.library.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import app.otakureader.feature.library.LibraryScreen

const val LIBRARY_ROUTE = "library"

fun NavController.navigateToLibrary(navOptions: NavOptions? = null) {
    navigate(LIBRARY_ROUTE, navOptions)
}

fun NavGraphBuilder.libraryScreen(
    onMangaClick: (Long) -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToBrowse: () -> Unit
) {
    composable(route = LIBRARY_ROUTE) {
        LibraryScreen(
            onMangaClick = onMangaClick,
            onNavigateToUpdates = onNavigateToUpdates,
            onNavigateToBrowse = onNavigateToBrowse
        )
    }
}
