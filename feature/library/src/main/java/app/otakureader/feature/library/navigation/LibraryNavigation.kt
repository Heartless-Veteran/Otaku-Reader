package app.otakureader.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.library.LibraryScreen

fun NavGraphBuilder.libraryScreen(
    onMangaClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToMigration: (List<Long>) -> Unit = {},
    onNavigateToCategoryManagement: () -> Unit = {},
    onNavigateToReader: (mangaId: Long, chapterId: Long) -> Unit = { _, _ -> },
    onNavigateToMergeDuplicates: () -> Unit = {},
) {
    composable<Route.Library> {
        LibraryScreen(
            onMangaClick = onMangaClick,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDownloads = onNavigateToDownloads,
            onNavigateToMigration = onNavigateToMigration,
            onNavigateToCategoryManagement = onNavigateToCategoryManagement,
            onNavigateToReader = onNavigateToReader,
            onNavigateToMergeDuplicates = onNavigateToMergeDuplicates,
        )
    }
}
