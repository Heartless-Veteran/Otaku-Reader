package app.otakureader.feature.tracking.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.otakureader.feature.tracking.TrackingScreen

const val TRACKING_ROUTE = "tracking"
const val MANGA_ID_ARG = "mangaId"
const val MANGA_TITLE_ARG = "mangaTitle"

fun NavController.navigateToTracking(
    mangaId: Long,
    mangaTitle: String,
    navOptions: NavOptions? = null
) {
    navigate("$TRACKING_ROUTE/$mangaId/$mangaTitle", navOptions)
}

fun NavGraphBuilder.trackingScreen(
    onNavigateBack: () -> Unit
) {
    composable(
        route = "$TRACKING_ROUTE/{$MANGA_ID_ARG}/{$MANGA_TITLE_ARG}",
        arguments = listOf(
            navArgument(MANGA_ID_ARG) { type = NavType.LongType },
            navArgument(MANGA_TITLE_ARG) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val mangaId = backStackEntry.arguments?.getLong(MANGA_ID_ARG) ?: 0L
        val mangaTitle = backStackEntry.arguments?.getString(MANGA_TITLE_ARG) ?: ""

        TrackingScreen(
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            onNavigateBack = onNavigateBack
        )
    }
}
