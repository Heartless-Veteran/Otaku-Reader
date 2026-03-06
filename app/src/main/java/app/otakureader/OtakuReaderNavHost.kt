package app.otakureader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.otakureader.core.navigation.LibraryRoute
import app.otakureader.core.navigation.MangaDetailRoute
import app.otakureader.core.navigation.ReaderRoute
import app.otakureader.feature.browse.navigation.browseScreen
import app.otakureader.feature.history.navigation.historyScreen
import app.otakureader.feature.library.navigation.libraryScreen
import app.otakureader.feature.reader.navigation.readerScreen
import app.otakureader.feature.settings.navigation.settingsScreen
import app.otakureader.feature.updates.navigation.updatesScreen

/**
 * Top-level navigation graph for Komikku.
 * Delegates each destination to its feature-module navigation extension.
 */
@Composable
fun OtakuReaderNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = LibraryRoute,
        modifier = modifier,
    ) {
        libraryScreen(
            onMangaClick = { mangaId ->
                navController.navigate(MangaDetailRoute(mangaId))
            },
        )
        updatesScreen(
            onChapterClick = { mangaId, chapterId ->
                navController.navigate(ReaderRoute(mangaId, chapterId))
            },
        )
        browseScreen(
            onMangaClick = { _, _ -> /* Navigate to source detail — Phase 1 */ },
        )
        historyScreen(
            onChapterClick = { mangaId, chapterId ->
                navController.navigate(ReaderRoute(mangaId, chapterId))
            },
        )
        settingsScreen()
        readerScreen(
            onBackClick = { navController.popBackStack() },
        )
    }
}
