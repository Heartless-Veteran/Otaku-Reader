package app.otakureader.feature.webview.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.webview.WebViewScreen

fun NavGraphBuilder.webViewFallbackScreen(
    onNavigateBack: () -> Unit,
) {
    composable<Route.WebViewFallback> { backStackEntry ->
        val args = backStackEntry.toRoute<Route.WebViewFallback>()
        WebViewScreen(
            initialUrl = args.url,
            title = args.title,
            onNavigateBack = onNavigateBack,
        )
    }
}
