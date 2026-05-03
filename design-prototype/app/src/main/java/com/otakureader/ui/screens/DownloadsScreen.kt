package com.otakureader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.data.DOWNLOADS
import com.otakureader.data.model.DownloadItem
import com.otakureader.data.model.DownloadState
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    val colors = LocalOtakuColors.current

    Scaffold(containerColor = colors.bg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface1),
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    MonoLabel("WorkManager", fontSize = 10, color = colors.fgDim)
                    Text("Download queue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.fg)
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface1),
                ) {
                    Icon(Icons.Default.Settings, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Status banner
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.accentDim)
                            .border(1.dp, colors.accent, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("2 downloading · 2 queued", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.fg)
                            Text("CBZ output · Wi-Fi only · High quality", fontSize = 11.sp, color = colors.fgMuted, modifier = Modifier.padding(top = 2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("Pause all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                items(DOWNLOADS) { item ->
                    DownloadRow(item)
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem) {
    val colors = LocalOtakuColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp, 56.dp)
                .clip(RoundedCornerShape(6.dp)),
        ) {
            CoverArtPlaceholder(
                hue = item.manga.hue,
                title = item.manga.title,
                author = item.manga.author,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.manga.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                MonoLabel(item.chapter, fontSize = 11, color = colors.accentSoft)
                Text("·", fontSize = 11.sp, color = colors.fgDim)
                MonoLabel(item.size, fontSize = 11)
                Spacer(Modifier.weight(1f))
                val stateColor = when (item.state) {
                    DownloadState.DOWNLOADING -> colors.accentSoft
                    DownloadState.DONE -> colors.success
                    DownloadState.PAUSED -> colors.warning
                    DownloadState.QUEUED -> colors.fgDim
                }
                Text(
                    item.state.name,
                    fontFamily = JetbrainsMonoFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = stateColor,
                    letterSpacing = 0.5.sp,
                )
            }
            if (item.state != DownloadState.DONE) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.surface3),
                ) {
                    val fillColor = if (item.state == DownloadState.PAUSED) colors.warning else colors.accent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(fillColor),
                    )
                }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), color = colors.border)
}
