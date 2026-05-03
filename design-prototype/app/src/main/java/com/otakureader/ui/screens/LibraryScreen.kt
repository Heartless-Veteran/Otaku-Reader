package com.otakureader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.data.*
import com.otakureader.data.model.Manga
import com.otakureader.ui.components.*
import com.otakureader.ui.navigation.Screen
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun LibraryScreen(
    onOpenManga: (Manga) -> Unit,
    onNav: (String) -> Unit,
) {
    val colors = LocalOtakuColors.current
    var selectedCategory by remember { mutableStateOf(CATEGORIES[1]) }
    var showFilter by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { LibraryBottomBar(active = "library", onNav = onNav) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                shape = RoundedCornerShape(18.dp),
                containerColor = colors.accent,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        },
        containerColor = colors.bg,
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(bottom = 4.dp, top = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            MonoLabel("Library", fontSize = 11, color = colors.fgDim)
                            Row(verticalAlignment = Alignment.Baseline) {
                                Text(
                                    text = selectedCategory.name,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.4).sp,
                                    color = colors.fg,
                                )
                                Spacer(Modifier.width(10.dp))
                                MonoLabel(
                                    text = selectedCategory.count.toString(),
                                    fontSize = 14,
                                    color = colors.fgDim,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surface1),
                            ) {
                                Icon(Icons.Default.Search, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { showFilter = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surface1),
                            ) {
                                Icon(Icons.Default.FilterList, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Category chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CATEGORIES.forEach { cat ->
                            OtakuChip(
                                label = cat.name,
                                active = cat.id == selectedCategory.id,
                                trailingLabel = cat.count.toString(),
                                modifier = Modifier.clickable { selectedCategory = cat },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Cover grid
            items(MANGA.take(9)) { manga ->
                LibraryCoverItem(manga = manga, onClick = { onOpenManga(manga) })
            }
        }
    }

    if (showFilter) {
        FilterSheet(onDismiss = { showFilter = false })
    }
}

@Composable
private fun LibraryCoverItem(manga: Manga, onClick: () -> Unit) {
    val colors = LocalOtakuColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        CoverCard(manga = manga, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            text = manga.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.fg,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp,
        )
        MonoLabel(
            text = "${manga.read}/${manga.chapters}",
            fontSize = 10,
            color = colors.fgDim,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun LibraryBottomBar(active: String, onNav: (String) -> Unit) {
    val colors = LocalOtakuColors.current
    val items = listOf(
        Triple("library", Icons.Default.CollectionsBookmark, "Library"),
        Triple("updates", Icons.Default.NewReleases, "Updates"),
        Triple("history", Icons.Default.History, "History"),
        Triple("browse", Icons.Default.Explore, "Browse"),
        Triple("more", Icons.Default.MoreHoriz, "More"),
    )
    NavigationBar(containerColor = colors.surface1, tonalElevation = 0.dp) {
        items.forEach { (id, icon, label) ->
            val selected = id == active
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNav(id) },
                icon = { Icon(icon, null, modifier = Modifier.size(22.dp)) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accentSoft,
                    selectedTextColor = colors.accentSoft,
                    indicatorColor = colors.accentDim,
                    unselectedIconColor = colors.fgDim,
                    unselectedTextColor = colors.fgDim,
                ),
            )
        }
    }
}
