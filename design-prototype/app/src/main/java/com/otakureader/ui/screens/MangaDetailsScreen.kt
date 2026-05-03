package com.otakureader.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.data.chaptersForManga
import com.otakureader.data.model.Chapter
import com.otakureader.data.model.Manga
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun MangaDetailsScreen(
    manga: Manga,
    onBack: () -> Unit,
    onRead: () -> Unit,
) {
    val colors = LocalOtakuColors.current
    val listState = rememberLazyListState()
    val scrollOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset.toFloat() } }
    val headerAlpha by animateFloatAsState((scrollOffset / 180f).coerceIn(0f, 1f), label = "headerAlpha")
    var selectedTab by remember { mutableIntStateOf(0) }
    var autoDownload by remember { mutableStateOf(true) }
    val chapters = remember(manga) { chaptersForManga(manga) }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(state = listState) {
            // Parallax hero
            item {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    // Blurred background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = scrollOffset * 0.4f
                                scaleX = 1.15f
                                scaleY = 1.15f
                            },
                    ) {
                        val topColor = Color.hsl(manga.hue.coerceIn(0f, 360f), 0.7f, 0.45f)
                        val botColor = Color.hsl(manga.hue.coerceIn(0f, 360f), 0.3f, 0.15f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(topColor, botColor)))
                        )
                    }

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to colors.bg.copy(alpha = 0.4f),
                                    0.4f to colors.bg.copy(alpha = 0.05f),
                                    1f to colors.bg,
                                )
                            ),
                    )

                    // Cover + meta info
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .graphicsLayer { translationY = scrollOffset * 0.15f },
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp, 162.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        ) {
                            CoverArtPlaceholder(
                                hue = manga.hue,
                                title = manga.title,
                                author = manga.author,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp)) {
                            Text(
                                manga.title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                letterSpacing = (-0.3).sp,
                                color = colors.fg,
                            )
                            Text(
                                manga.author,
                                fontSize = 13.sp,
                                color = colors.fgMuted,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.accentDim)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        manga.status.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accentSoft,
                                        letterSpacing = 0.6.sp,
                                    )
                                }
                                MonoLabel(manga.year.toString(), fontSize = 11, color = colors.fgMuted)
                                Text("·", fontSize = 11.sp, color = colors.fgDim)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Default.Star, null, tint = colors.warning, modifier = Modifier.size(11.dp))
                                    MonoLabel("%.1f".format(manga.rating), fontSize = 11, color = colors.fg)
                                }
                            }
                        }
                    }
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onRead,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Continue Ch. ${manga.read + 1}", fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.accentDim)
                            .border(1.dp, colors.accent, RoundedCornerShape(16.dp)),
                    ) {
                        Icon(Icons.Default.Bookmark, null, tint = colors.accentSoft, modifier = Modifier.size(17.dp))
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surface1)
                            .border(1.dp, colors.borderStrong, RoundedCornerShape(16.dp)),
                    ) {
                        Icon(Icons.Default.Download, null, tint = colors.fgMuted, modifier = Modifier.size(17.dp))
                    }
                }
            }

            // Genre chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    manga.genres.forEach { g -> OtakuChip(label = g) }
                    OtakuChip(label = manga.scanlator)
                }
                Spacer(Modifier.height(14.dp))
            }

            // Description
            item {
                Text(
                    "A coastal town hides a tide that bleeds. When a lighthouse keeper's daughter vanishes during the new moon, an exiled fisherman returns home only to find his oldest enemy has become his only ally.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = colors.fgMuted,
                )
                Spacer(Modifier.height(16.dp))
            }

            // Auto-download toggle
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface1)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable { autoDownload = !autoDownload }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.accentDim),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Download, null, tint = colors.accentSoft, modifier = Modifier.size(15.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-download new chapters", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.fg)
                        Text("High quality · Wi-Fi only", fontSize = 11.sp, color = colors.fgDim, modifier = Modifier.padding(top = 2.dp))
                    }
                    OtakuToggle(checked = autoDownload, onCheckedChange = { autoDownload = it })
                }
                Spacer(Modifier.height(20.dp))
            }

            // Tabs
            item {
                val tabs = listOf("Chapters", "Related", "Tracking")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface1)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tabs.forEachIndexed { i, tab ->
                        val sel = i == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) colors.surface3 else Color.Transparent)
                                .clickable { selectedTab = i },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(tab, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (sel) colors.fg else colors.fgMuted)
                                if (i == 0) {
                                    MonoLabel(manga.chapters.toString(), fontSize = 11, color = if (sel) colors.fgMuted else colors.fgDim)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Chapter list
            items(chapters) { chapter ->
                ChapterRow(chapter = chapter, onRead = if (chapter.reading) onRead else null)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        // Sticky top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.bg.copy(alpha = headerAlpha))
                .align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Text(
                    manga.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.fg.copy(alpha = headerAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Share, null, tint = Color.White)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, onRead: (() -> Unit)?) {
    val colors = LocalOtakuColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onRead != null, onClick = onRead ?: {})
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Status icon box
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        chapter.reading -> colors.accentDim
                        chapter.read -> Color.Transparent
                        else -> colors.surface2
                    }
                )
                .then(
                    if (chapter.reading) Modifier.border(1.dp, colors.accent, RoundedCornerShape(10.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                chapter.reading -> Icon(Icons.Default.PlayArrow, null, tint = colors.accentSoft, modifier = Modifier.size(11.dp))
                chapter.read -> Icon(Icons.Default.Check, null, tint = colors.fgDim, modifier = Modifier.size(13.dp))
                else -> MonoLabel(chapter.num.toString(), fontSize = 11, color = colors.fgMuted)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Chapter ${chapter.num}",
                fontSize = 14.sp,
                fontWeight = if (chapter.reading) FontWeight.SemiBold else FontWeight.Medium,
                color = if (chapter.reading) colors.accentSoft else colors.fg.copy(alpha = if (chapter.read) 0.5f else 1f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                MonoLabel(chapter.date, fontSize = 11)
                Text("·", fontSize = 11.sp, color = colors.fgDim)
                MonoLabel("${chapter.pages}p", fontSize = 11)
                if (chapter.reading) {
                    Text("·", fontSize = 11.sp, color = colors.fgDim)
                    Text("Page ${chapter.readingPage}/${chapter.totalPages}", fontSize = 11.sp, color = colors.accentSoft, fontWeight = FontWeight.Medium)
                }
            }
            if (chapter.reading) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.surface3),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(chapter.readingPage.toFloat() / chapter.totalPages)
                            .fillMaxHeight()
                            .background(colors.accent),
                    )
                }
            }
        }

        // Download icon
        if (chapter.downloaded) {
            Icon(Icons.Default.CheckCircle, null, tint = colors.success, modifier = Modifier.size(15.dp))
        } else {
            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Download, null, tint = colors.fgMuted, modifier = Modifier.size(15.dp))
            }
        }
    }
}
