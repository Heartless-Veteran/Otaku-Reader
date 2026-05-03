package com.otakureader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.otakureader.data.SOURCES
import com.otakureader.data.model.Source
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun BrowseScreen(
    onSearch: () -> Unit,
    onNav: (String) -> Unit,
) {
    val colors = LocalOtakuColors.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sources", "Extensions", "Migrate")

    Scaffold(
        bottomBar = { LibraryBottomBar(active = "browse", onNav = onNav) },
        containerColor = colors.bg,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    MonoLabel("Browse", fontSize = 11, color = colors.fgDim)
                    Text(
                        "Sources",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        color = colors.fg,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )

                    // Search bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surface1)
                            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                            .clickable(onClick = onSearch)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Search, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                        Text(
                            "Search across all sources",
                            fontSize = 14.sp,
                            color = colors.fgDim,
                            modifier = Modifier.weight(1f),
                        )
                        MonoLabel("5 sources", fontSize = 10, color = colors.fgDim)
                    }
                    Spacer(Modifier.height(12.dp))

                    // Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface1)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        tabs.forEachIndexed { i, tab ->
                            val selected = i == selectedTab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) colors.surface3 else Color.Transparent)
                                    .clickable { selectedTab = i },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    tab,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) colors.fg else colors.fgMuted,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            items(SOURCES) { source ->
                SourceCard(source)
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SectionHeader("Pinned categories")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Latest", "Popular", "New series", "Random").forEach { tag ->
                            OtakuChip(label = tag)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceCard(source: Source) {
    val colors = LocalOtakuColors.current
    val gradHue = (source.name.length * 47f) % 360f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .clickable {}
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.hsl(gradHue, 0.55f, 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = source.name[0].toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(source.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.fg)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                MonoLabel(source.lang, fontSize = 11)
                Text("·", fontSize = 11.sp, color = colors.fgDim)
                MonoLabel(
                    "%,d".format(source.count),
                    fontSize = 11,
                )
                if (source.status == "local") {
                    Text("·", fontSize = 11.sp, color = colors.fgDim)
                    Text(
                        "LOCAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = JetbrainsMonoFamily,
                        color = colors.success,
                        letterSpacing = 0.6.sp,
                    )
                }
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = colors.fgDim, modifier = Modifier.size(16.dp))
    }
}
