package app.otakureader

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.otakureader.core.navigation.Route
import app.otakureader.feature.about.navigation.aboutScreen
import app.otakureader.feature.browse.navigation.browseScreen
import app.otakureader.feature.browse.navigation.extensionInstallScreen
import app.otakureader.feature.browse.navigation.extensionsBottomSheet
import app.otakureader.feature.browse.navigation.globalSearchScreen
import app.otakureader.feature.browse.navigation.sourceMangaDetailScreen
import app.otakureader.feature.browse.navigation.sourceDetailScreen
import app.otakureader.feature.details.navigation.detailsScreen
import app.otakureader.feature.feed.navigation.feedScreen
import app.otakureader.feature.history.navigation.historyScreen
import app.otakureader.feature.library.category.navigation.categoryManagementScreen
import app.otakureader.feature.library.navigation.libraryScreen
import app.otakureader.feature.migration.navigation.migrationEntryScreen
import app.otakureader.feature.migration.navigation.migrationScreen
import app.otakureader.feature.more.navigation.moreScreen
import app.otakureader.feature.onboarding.navigation.onboardingScreen
import app.otakureader.feature.opds.navigation.opdsScreen
import app.otakureader.feature.reader.navigation.readerScreen
import app.otakureader.feature.settings.navigation.settingsScreen
import app.otakureader.feature.statistics.navigation.statisticsScreen
import app.otakureader.feature.tracking.navigation.trackingScreen
import app.otakureader.feature.updates.navigation.downloadsScreen
import app.otakureader.feature.updates.navigation.updatesScreen
import app.otakureader.util.DeepLinkResult

@Composable
fun OtakuReaderNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onboardingCompleted: Boolean = false,
    deepLinkResult: DeepLinkResult? = null,
    onDeepLinkConsumed: () -> Unit = {},
    onOnboardingComplete: () -> Unit = {},
) {
    // Determine start destination based on onboarding status
    val startDestination: Route = if (onboardingCompleted) Route.Library else Route.Onboarding

    // Handle deep link navigation - only trigger once when deepLinkResult changes
    LaunchedEffect(deepLinkResult) {
        when (deepLinkResult) {
            is DeepLinkResult.MangaUrl -> {
                navController.navigate(Route.Search(query = deepLinkResult.mangaUrl))
                onDeepLinkConsumed()
            }
            is DeepLinkResult.SearchQuery -> {
                navController.navigate(Route.Search(query = deepLinkResult.query))
                onDeepLinkConsumed()
            }
            is DeepLinkResult.NavigateToLibrary -> {
                navController.navigate(Route.Library) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
                onDeepLinkConsumed()
            }
            is DeepLinkResult.NavigateToUpdates -> {
                navController.navigate(Route.Updates) {
                    launchSingleTop = true
                }
                onDeepLinkConsumed()
            }
            is DeepLinkResult.ContinueReading -> {
                navController.navigate(
                    Route.Reader(deepLinkResult.mangaId, deepLinkResult.chapterId)
                ) {
                    launchSingleTop = true
                }
                onDeepLinkConsumed()
            }
            is DeepLinkResult.Invalid, null -> {
                // No deep link to handle
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // Library screen - main entry point
        libraryScreen(
            onMangaClick = { mangaId ->
                navController.navigate(Route.MangaDetails(mangaId))
            },
            onNavigateToSettings = {
                navController.navigate(Route.Settings)
            },
            onNavigateToDownloads = {
                navController.navigate(Route.Downloads)
            },
            onNavigateToMigration = {
                navController.navigate(Route.MigrationEntry)
            },
            onNavigateToCategoryManagement = {
                navController.navigate(Route.CategoryManagement)
            },
            onNavigateToReader = { mangaId, chapterId ->
                navController.navigate(Route.Reader(mangaId, chapterId))
            }
        )

        // Updates screen - new chapters
        updatesScreen(
            onMangaClick = { mangaId ->
                navController.navigate(Route.MangaDetails(mangaId))
            },
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToDownloads = {
                navController.navigate(Route.Downloads)
            },
        )

        // Browse - sources catalog
        browseScreen(
            onMangaClick = { sourceId, mangaUrl, mangaTitle ->
                navController.navigate(Route.SourceMangaDetail(sourceId, mangaUrl, mangaTitle))
            },
            onNavigateToSource = { sourceId ->
                navController.navigate(Route.SourceListing(sourceId))
            },
            onNavigateToExtensions = {
                navController.navigate(Route.ExtensionCatalog)
            },
            onNavigateToGlobalSearch = {
                navController.navigate(Route.Search(query = ""))
            },
            onNavigateToOpds = {
                navController.navigate(Route.OpdsCatalog())
            },
        )

        // OPDS catalog
        opdsScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToMangaDetail = { mangaUrl, mangaTitle ->
                navController.navigate(Route.Search(query = mangaUrl))
            }
        )

        // Global search across all sources
        globalSearchScreen(
            onMangaClick = { sourceId, mangaUrl ->
                navController.navigate(Route.SourceMangaDetail(sourceId, mangaUrl))
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Source detail — manga listing from a specific source
        sourceDetailScreen(
            onMangaClick = { sourceId, mangaUrl, mangaTitle ->
                navController.navigate(Route.SourceMangaDetail(sourceId, mangaUrl, mangaTitle))
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Source manga detail — resolves URL to DB id then forwards to MangaDetails
        sourceMangaDetailScreen(
            onNavigateToMangaDetail = { mangaId ->
                navController.navigate(Route.MangaDetails(mangaId)) {
                    popUpTo { inclusive = true }
                }
            }
        )

        // Extensions bottom sheet
        extensionsBottomSheet(
            onDismiss = {
                navController.popBackStack()
            }
        )

        // Extension install screen
        extensionInstallScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // History screen
        historyScreen(
            onChapterClick = { mangaId, chapterId ->
                navController.navigate(Route.Reader(mangaId, chapterId))
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Manga details
        detailsScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToReader = { mangaId, chapterId ->
                navController.navigate(Route.Reader(mangaId, chapterId))
            },
            onNavigateToTracking = { mangaId, mangaTitle ->
                navController.navigate(Route.Tracking(mangaId, mangaTitle))
            },
            onNavigateToGlobalSearch = { query ->
                navController.navigate(Route.Search(query = query))
            }
        )

        // Reader
        readerScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Settings
        settingsScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToMigrationEntry = {
                navController.navigate(Route.MigrationEntry)
            },
            onNavigateToAbout = {
                navController.navigate(Route.About)
            }
        )

        downloadsScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Statistics
        statisticsScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Migration
        migrationScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Migration entry
        migrationEntryScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToMigration = { selectedMangaIds ->
                navController.navigate(Route.Migration(selectedMangaIds))
            }
        )

        // Tracking
        trackingScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Feed
        feedScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToReader = { mangaId, chapterId ->
                navController.navigate(Route.Reader(mangaId, chapterId))
            }
        )

        // About
        aboutScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // More screen - access to settings, downloads, statistics, about, extensions, feed
        moreScreen(
            onNavigateToSettings = {
                navController.navigate(Route.Settings)
            },
            onNavigateToDownloads = {
                navController.navigate(Route.Downloads)
            },
            onNavigateToStatistics = {
                navController.navigate(Route.Statistics)
            },
            onNavigateToAbout = {
                navController.navigate(Route.About)
            },
            onNavigateToExtensions = {
                navController.navigate(Route.ExtensionCatalog)
            },
            onNavigateToFeed = {
                navController.navigate(Route.Feed)
            },
        )

        // Category management
        categoryManagementScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Onboarding — shows first for new users, navigates to Library on completion
        onboardingScreen(
            onComplete = {
                onOnboardingComplete()
                navController.navigate(Route.Library) {
                    popUpTo { inclusive = true }
                }
            },
            onNavigateToExtensions = {
                navController.navigate(Route.ExtensionCatalog)
            }
        )
    }
}
