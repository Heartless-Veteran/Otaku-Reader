package app.otakureader.feature.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LibraryBottomNavigation(
    selectedRoute: String,
    onNavigateToLibrary: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToBrowse: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    newUpdatesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.CollectionsBookmark, contentDescription = null) },
            label = { Text("Library") },
            selected = selectedRoute == "library",
            onClick = onNavigateToLibrary
        )
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
                    Icon(Icons.Default.NewReleases, contentDescription = null)
                }
            },
            label = { Text("Updates") },
            selected = selectedRoute == "updates",
            onClick = onNavigateToUpdates
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = null) },
            label = { Text("Browse") },
            selected = selectedRoute == "browse",
            onClick = onNavigateToBrowse
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text("History") },
            selected = selectedRoute == "history",
            onClick = onNavigateToHistory
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.QueryStats, contentDescription = null) },
            label = { Text("Stats") },
            selected = selectedRoute == "statistics",
            onClick = onNavigateToStatistics
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = selectedRoute == "settings",
            onClick = onNavigateToSettings
        )
    }
}
