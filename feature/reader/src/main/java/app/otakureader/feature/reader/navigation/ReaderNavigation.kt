package app.otakureader.feature.reader.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.otakureader.core.navigation.ReaderRoute
import app.otakureader.feature.reader.ReaderScreen

fun NavGraphBuilder.readerScreen(
    onNavigateBack: () -> Unit,
) {
    composable<ReaderRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ReaderRoute>()
        ReaderScreen(
            mangaId = route.mangaId,
            chapterId = route.chapterId,
            onNavigateBack = onNavigateBack
        )
    }
}
