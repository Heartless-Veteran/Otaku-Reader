package app.otakureader.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.HistoryRoute
import app.otakureader.feature.history.HistoryScreen

fun NavGraphBuilder.historyScreen(
    onMangaClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable<HistoryRoute> {
        HistoryScreen(
            onMangaClick = onMangaClick,
            onNavigateBack = onNavigateBack
        )
    }
}
