package app.otakureader.feature.browse.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.BrowseRoute
import app.otakureader.feature.browse.BrowseScreen

fun NavGraphBuilder.browseScreen(
    onMangaClick: (sourceId: String, mangaUrl: String) -> Unit,
) {
    composable<BrowseRoute> {
        BrowseScreen(onMangaClick = onMangaClick)
    }
}
