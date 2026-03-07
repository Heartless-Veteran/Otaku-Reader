package app.otakureader.feature.reader.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val READER_ROUTE = "reader"
const val READER_MANGA_ID_ARG = "mangaId"

fun NavGraphBuilder.readerScreen(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = "$READER_ROUTE/{$READER_MANGA_ID_ARG}",
        arguments = listOf(
            navArgument(READER_MANGA_ID_ARG) { type = NavType.LongType }
        )
    ) { backStackEntry ->
        val mangaId = backStackEntry.arguments?.getLong(READER_MANGA_ID_ARG) ?: 0L
        ReaderScreen(
            mangaId = mangaId,
            onNavigateBack = onNavigateBack
        )
    }
}
