package app.otakureader.feature.reader.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.otakureader.core.navigation.Route
import app.otakureader.feature.reader.ReaderScreen

fun NavGraphBuilder.readerScreen(
    onNavigateBack: () -> Unit,
) {
    composable<Route.Reader> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Reader>()
        ReaderScreen(
            mangaId = route.mangaId,
            chapterId = route.chapterId,
            onNavigateBack = onNavigateBack
        )
    }
}
