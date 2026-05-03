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
import com.otakureader.data.UPDATES
import com.otakureader.data.model.UpdateEntry
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun UpdatesScreen(onNav: (String) -> Unit) {
    val colors = LocalOtakuColors.current

    Scaffold(
        bottomBar = { LibraryBottomBar(active = "updates", onNav = onNav) },
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
                        MonoLabel("Updates", fontSize = 11, color = colors.fgDim)
                        Text(
                            "4 new chapters",
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
                        Icon(Icons.Default.Refresh, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            var lastDate = ""
            UPDATES.forEach { update ->
                val date = when {
                    update.when_.contains("ago") || update.when_ == "Just now" -> "Today"
                    update.when_ == "Yesterday" -> "Yesterday"
                    else -> update.when_
                }
                if (date != lastDate) {
                    lastDate = date
                    item(key = "header-$date") {
                        SectionHeader(
                            text = date,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
                item(key = "${update.manga.id}-${update.chapter}") {
                    UpdateRow(update)
                }
            }
        }
    }
}

@Composable
private fun UpdateRow(update: UpdateEntry) {
    val colors = LocalOtakuColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp, 60.dp)
                .clip(RoundedCornerShape(6.dp)),
        ) {
            CoverArtPlaceholder(
                hue = update.manga.hue,
                title = update.manga.title,
                author = update.manga.author,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = update.manga.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = update.chapter,
                    fontFamily = JetbrainsMonoFamily,
                    fontSize = 12.sp,
                    color = if (update.isNew) colors.accentSoft else colors.fgDim,
                )
                Text("·", fontSize = 12.sp, color = colors.fgDim)
                Text(update.when_, fontSize = 12.sp, color = colors.fgDim)
            }
        }

        IconButton(
            onClick = {},
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.surface1),
        ) {
            if (update.downloaded) {
                Icon(Icons.Default.CheckCircle, null, tint = colors.success, modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.Download, null, tint = colors.fgMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = colors.border,
    )
}
