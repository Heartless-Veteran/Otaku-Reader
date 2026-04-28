package app.otakureader.feature.feed.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.feed.FeedScreen

fun NavGraphBuilder.feedScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReader: (mangaId: Long, chapterId: Long) -> Unit
) {
    composable<Route.Feed> {
        FeedScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToReader = onNavigateToReader
        )
    }
}
