package app.otakureader.feature.reader.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.ReaderRoute
import app.otakureader.feature.reader.ReaderScreen

fun NavGraphBuilder.readerScreen(
    onBackClick: () -> Unit,
) {
    composable<ReaderRoute> {
        ReaderScreen(onNavigateBack = onBackClick)
    }
}
