package app.otakureader

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import app.otakureader.core.navigation.BrowseRoute
import app.otakureader.core.navigation.HistoryRoute
import app.otakureader.core.navigation.LibraryRoute
import app.otakureader.core.navigation.MoreRoute
import app.otakureader.core.navigation.UpdatesRoute
import app.otakureader.core.preferences.GeneralPreferences

/**
 * Bottom navigation bar for the main app navigation.
 * Provides quick access to Library, Updates, Browse, History, and More.
 */
@Composable
fun OtakuReaderBottomBar(
    navController: NavController,
    generalPreferences: GeneralPreferences,
    newUpdatesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom bar on top-level destinations
    val isTopLevelDestination = currentDestination?.hierarchy?.any { destination ->
        destination.hasRoute(LibraryRoute::class) ||
        destination.hasRoute(UpdatesRoute::class) ||
        destination.hasRoute(BrowseRoute::class) ||
        destination.hasRoute(HistoryRoute::class) ||
        destination.hasRoute(MoreRoute::class)
    } == true

    if (!isTopLevelDestination) {
        return
    }

    NavigationBar(
        modifier = modifier
    ) {
        // Library
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.CollectionsBookmark, 
                    contentDescription = stringResource(R.string.nav_library)
                ) 
            },
            label = { Text(stringResource(R.string.nav_library)) },
            selected = currentDestination?.hasRoute(LibraryRoute::class) == true,
            onClick = {
                navController.navigate(LibraryRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // Updates
        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        if (newUpdatesCount > 0) {
                            Badge {
                                Text(
                                    text = if (newUpdatesCount > 99) "99+" else newUpdatesCount.toString()
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.NewReleases, 
                        contentDescription = stringResource(R.string.nav_updates)
                    )
                }
            },
            label = { Text(stringResource(R.string.nav_updates)) },
            selected = currentDestination?.hasRoute(UpdatesRoute::class) == true,
            onClick = {
                navController.navigate(UpdatesRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // Browse
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.Explore, 
                    contentDescription = stringResource(R.string.nav_browse)
                ) 
            },
            label = { Text(stringResource(R.string.nav_browse)) },
            selected = currentDestination?.hasRoute(BrowseRoute::class) == true,
            onClick = {
                navController.navigate(BrowseRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // History
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.History, 
                    contentDescription = stringResource(R.string.nav_history)
                ) 
            },
            label = { Text(stringResource(R.string.nav_history)) },
            selected = currentDestination?.hasRoute(HistoryRoute::class) == true,
            onClick = {
                navController.navigate(HistoryRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // More
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.MoreHoriz, 
                    contentDescription = stringResource(R.string.nav_more)
                ) 
            },
            label = { Text(stringResource(R.string.nav_more)) },
            selected = currentDestination?.hasRoute(MoreRoute::class) == true,
            onClick = {
                navController.navigate(MoreRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}
