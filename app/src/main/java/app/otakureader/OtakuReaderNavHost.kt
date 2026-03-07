package app.otakureader.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.otakureader.feature.library.navigation.LIBRARY_ROUTE
import app.otakureader.feature.library.navigation.libraryScreen

@Composable
fun OtakuReaderNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = LIBRARY_ROUTE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        libraryScreen(
            onMangaClick = { mangaId ->
                // TODO: Navigate to manga details
            },
            onNavigateToUpdates = {
                // TODO: Navigate to updates
            },
            onNavigateToBrowse = {
                // TODO: Navigate to browse
            }
        )
        
        // TODO: Add other screens
    }
}
