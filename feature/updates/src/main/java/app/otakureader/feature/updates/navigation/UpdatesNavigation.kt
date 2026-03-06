package app.otakureader.feature.updates.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.UpdatesRoute
import app.otakureader.feature.updates.UpdatesScreen

fun NavGraphBuilder.updatesScreen(
    onChapterClick: (mangaId: Long, chapterId: Long) -> Unit,
) {
    composable<UpdatesRoute> {
        UpdatesScreen(onChapterClick = onChapterClick)
    }
}
