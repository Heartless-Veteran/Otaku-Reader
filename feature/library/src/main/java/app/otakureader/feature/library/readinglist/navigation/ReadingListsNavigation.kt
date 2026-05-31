package app.otakureader.feature.library.readinglist.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.library.readinglist.ReadingListDetailScreen
import app.otakureader.feature.library.readinglist.ReadingListsScreen

fun NavGraphBuilder.readingListsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToList: (Long) -> Unit,
) {
    composable<Route.ReadingLists> {
        ReadingListsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToList = onNavigateToList,
        )
    }
}

fun NavGraphBuilder.readingListDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToManga: (Long) -> Unit,
) {
    composable<Route.ReadingListDetail> {
        ReadingListDetailScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToManga = onNavigateToManga,
        )
    }
}
