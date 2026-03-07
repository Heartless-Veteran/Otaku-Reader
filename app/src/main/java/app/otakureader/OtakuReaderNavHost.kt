package app.otakureader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.otakureader.feature.library.navigation.LIBRARY_ROUTE
import app.otakureader.feature.library.navigation.libraryScreen
import app.otakureader.feature.updates.navigation.UPDATES_ROUTE
import app.otakureader.feature.updates.navigation.updatesScreen
import app.otakureader.feature.browse.navigation.BROWSE_ROUTE
import app.otakureader.feature.browse.navigation.browseScreen
import app.otakureader.feature.reader.navigation.READER_ROUTE
import app.otakureader.feature.reader.navigation.readerScreen

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
                navController.navigate("$READER_ROUTE/$mangaId")
            },
            onNavigateToUpdates = {
                navController.navigate(UPDATES_ROUTE)
            },
            onNavigateToBrowse = {
                navController.navigate(BROWSE_ROUTE)
            }
        )
        
        updatesScreen(
            onMangaClick = { mangaId ->
                navController.navigate("$READER_ROUTE/$mangaId")
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
        
        browseScreen(
            onMangaClick = { mangaId ->
                navController.navigate("$READER_ROUTE/$mangaId")
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
        
        readerScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}
