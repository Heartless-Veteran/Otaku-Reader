package com.otakureader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.otakureader.data.MANGA
import com.otakureader.ui.AppViewModel
import com.otakureader.ui.screens.*

sealed class Screen(val route: String) {
    object Library     : Screen("library")
    object Updates     : Screen("updates")
    object History     : Screen("history")
    object Browse      : Screen("browse")
    object More        : Screen("more")
    object Search      : Screen("search")
    object Downloads   : Screen("downloads")
    object Categories  : Screen("categories")
    object Stats       : Screen("stats")
    object Settings    : Screen("settings")
    object Details     : Screen("details/{mangaId}") {
        fun route(id: Int) = "details/$id"
    }
    object Reader      : Screen("reader/{mangaId}") {
        fun route(id: Int) = "reader/$id"
    }
}

val bottomNavScreens = listOf(
    Screen.Library, Screen.Updates, Screen.History, Screen.Browse, Screen.More
)

fun NavHostController.isBottomNavVisible(): Boolean {
    val route = currentBackStackEntry?.destination?.route ?: return true
    return bottomNavScreens.any { it.route == route }
}

@Composable
fun OtakuNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = viewModel(),
) {
    val settings by appViewModel.settings.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        modifier = modifier,
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onOpenManga = { manga -> navController.navigate(Screen.Details.route(manga.id)) },
                onNav = { route -> navController.navigate(route) { launchSingleTop = true } },
            )
        }
        composable(Screen.Updates.route) {
            UpdatesScreen(
                onNav = { navController.navigate(it) { launchSingleTop = true } },
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onOpenManga = { manga -> navController.navigate(Screen.Details.route(manga.id)) },
                onNav = { navController.navigate(it) { launchSingleTop = true } },
            )
        }
        composable(Screen.Browse.route) {
            BrowseScreen(
                onSearch = { navController.navigate(Screen.Search.route) },
                onNav = { navController.navigate(it) { launchSingleTop = true } },
            )
        }
        composable(Screen.More.route) {
            MoreScreen(
                onNav = { navController.navigate(it) { launchSingleTop = true } },
                onSubroute = { navController.navigate(it) },
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenManga = { manga -> navController.navigate(Screen.Details.route(manga.id)) },
            )
        }
        composable(Screen.Downloads.route) {
            DownloadsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Categories.route) {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                settings = settings,
                onSetTheme = { appViewModel.setTheme(it) },
                onSetAccent = { appViewModel.setAccent(it) },
                onSetDynamicColor = { appViewModel.setDynamicColor(it) },
            )
        }
        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("mangaId") { type = NavType.IntType }),
        ) { entry ->
            val mangaId = entry.arguments?.getInt("mangaId") ?: 0
            val manga = MANGA.find { it.id == mangaId } ?: MANGA[0]
            MangaDetailsScreen(
                manga = manga,
                onBack = { navController.popBackStack() },
                onRead = { navController.navigate(Screen.Reader.route(manga.id)) },
            )
        }
        composable(
            route = Screen.Reader.route,
            arguments = listOf(navArgument("mangaId") { type = NavType.IntType }),
        ) { entry ->
            val mangaId = entry.arguments?.getInt("mangaId") ?: 0
            val manga = MANGA.find { it.id == mangaId } ?: MANGA[0]
            ReaderScreen(
                manga = manga,
                initialMode = settings.readerMode,
                initialBrightness = settings.brightness,
                onClose = { navController.popBackStack() },
            )
        }
    }
}
