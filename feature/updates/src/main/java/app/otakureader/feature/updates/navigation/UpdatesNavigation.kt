package app.otakureader.feature.updates.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.UpdatesRoute
import app.otakureader.feature.updates.UpdatesScreen

fun NavGraphBuilder.updatesScreen(
    onMangaClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable<UpdatesRoute> {
        UpdatesScreen(
            onMangaClick = onMangaClick,
            onNavigateBack = onNavigateBack
        )
    }
}
