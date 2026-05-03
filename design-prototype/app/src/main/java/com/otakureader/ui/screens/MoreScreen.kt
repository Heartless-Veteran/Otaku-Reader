package com.otakureader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

private data class MoreItem(
    val icon: ImageVector,
    val label: String,
    val sub: String,
    val route: String? = null,
)

@Composable
fun MoreScreen(
    onNav: (String) -> Unit,
    onSubroute: (String) -> Unit,
) {
    val colors = LocalOtakuColors.current

    val items = listOf(
        MoreItem(Icons.Default.Download, "Download queue", "12 in queue · 3 active", "downloads"),
        MoreItem(Icons.Default.Category, "Categories", "6 categories", "categories"),
        MoreItem(Icons.Default.BarChart, "Reading stats", "1,284 chapters this year", "stats"),
        MoreItem(Icons.Default.CloudSync, "Sync & backup", "Last synced 2h ago"),
        MoreItem(Icons.Default.Extension, "Sources & extensions", "5 active"),
        MoreItem(Icons.Default.Settings, "Settings", "Theme, reader, advanced", "settings"),
    )

    Scaffold(
        bottomBar = { LibraryBottomBar(active = "more", onNav = onNav) },
        containerColor = colors.bg,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    MonoLabel("More", fontSize = 11, color = colors.fgDim)
                    Text("Reader", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, color = colors.fg, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(8.dp))
            }

            // App card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface1)
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.MenuBook, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Otaku Reader", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.fg)
                        Text(
                            "v2.1.0 · build 4180",
                            fontFamily = JetbrainsMonoFamily,
                            fontSize = 11.sp,
                            color = colors.fgDim,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Menu items
            items(items.size) { i ->
                val item = items[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = item.route != null) { item.route?.let(onSubroute) }
                        .padding(horizontal = 20.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, null, tint = colors.accentSoft, modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.fg)
                        Text(item.sub, fontSize = 12.sp, color = colors.fgDim, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (item.route != null) {
                        Icon(Icons.Default.ChevronRight, null, tint = colors.fgDim, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
