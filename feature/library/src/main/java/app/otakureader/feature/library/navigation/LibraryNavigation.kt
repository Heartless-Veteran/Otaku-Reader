package app.otakureader.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.LibraryRoute
import app.otakureader.feature.library.LibraryScreen

fun NavGraphBuilder.libraryScreen(
    onMangaClick: (Long) -> Unit,
) {
    composable<LibraryRoute> {
        LibraryScreen(onMangaClick = onMangaClick)
    }
}
