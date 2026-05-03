package com.otakureader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.data.HISTORY
import com.otakureader.data.model.Manga
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun HistoryScreen(
    onOpenManga: (Manga) -> Unit,
    onNav: (String) -> Unit,
) {
    val colors = LocalOtakuColors.current

    Scaffold(
        bottomBar = { LibraryBottomBar(active = "history", onNav = onNav) },
        containerColor = colors.bg,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        MonoLabel("History", fontSize = 11, color = colors.fgDim)
                        Text(
                            "Recently read",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                            color = colors.fg,
                        )
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface1),
                    ) {
                        Icon(Icons.Default.Search, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            items(HISTORY) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp, 76.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    ) {
                        CoverArtPlaceholder(
                            hue = entry.manga.hue,
                            title = entry.manga.title,
                            author = entry.manga.author,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = entry.manga.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = entry.chapter,
                            fontFamily = JetbrainsMonoFamily,
                            fontSize = 12.sp,
                            color = colors.accentSoft,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            MonoLabel(entry.page, fontSize = 11)
                            Text("·", fontSize = 11.sp, color = colors.fgDim)
                            Text(entry.when_, fontSize = 11.sp, color = colors.fgDim)
                        }
                    }

                    IconButton(
                        onClick = { onOpenManga(entry.manga) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface1)
                            .align(Alignment.CenterVertically),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = colors.accentSoft,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = colors.border)
            }
        }
    }
}
