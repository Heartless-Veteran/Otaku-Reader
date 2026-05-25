package app.otakureader.feature.feed.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.feed.FeedScreen
import app.otakureader.feature.feed.SavedFeedScreen

fun NavGraphBuilder.feedScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReader: (mangaId: Long, chapterId: Long) -> Unit,
    onNavigateToFeedManagement: () -> Unit = {},
) {
    composable<Route.Feed> {
        FeedScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToReader = onNavigateToReader,
            onNavigateToFeedManagement = onNavigateToFeedManagement,
        )
    }
}

fun NavGraphBuilder.feedManagementScreen(
    onNavigateBack: () -> Unit,
) {
    composable<Route.FeedManagement> {
        SavedFeedScreen(onNavigateBack = onNavigateBack)
    }
}
