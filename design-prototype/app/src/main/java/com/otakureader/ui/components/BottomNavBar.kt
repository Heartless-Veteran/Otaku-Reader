package com.otakureader.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.otakureader.ui.navigation.Screen

data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

val navItems = listOf(
    NavItem(Screen.Library, "Library", Icons.Outlined.CollectionsBookmark, Icons.Filled.CollectionsBookmark),
    NavItem(Screen.Updates, "Updates", Icons.Outlined.NewReleases, Icons.Filled.NewReleases),
    NavItem(Screen.History, "History", Icons.Outlined.History, Icons.Filled.History),
    NavItem(Screen.Browse, "Browse", Icons.Outlined.Explore, Icons.Filled.Explore),
    NavItem(Screen.More, "More", Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz),
)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        navItems.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(Screen.Library.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}
