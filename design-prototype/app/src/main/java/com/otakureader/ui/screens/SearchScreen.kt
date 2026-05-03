package com.otakureader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.otakureader.data.MANGA
import com.otakureader.data.model.Manga
import com.otakureader.ui.components.CoverArtPlaceholder
import com.otakureader.ui.components.MonoLabel
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun SearchScreen(
    query: String = "kaiju",
    onBack: () -> Unit,
    onOpenManga: (Manga) -> Unit,
) {
    val colors = LocalOtakuColors.current
    val groups = listOf(
        Triple("MangaDex", 12, MANGA.take(4)),
        Triple("Mangaplus", 6, MANGA.drop(4).take(3)),
        Triple("Bato.to", 18, MANGA.drop(7).take(3)),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Search bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface1),
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface1)
                    .border(1.dp, colors.borderStrong, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Search, null, tint = colors.fgMuted, modifier = Modifier.size(16.dp))
                Text(query, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.fg, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(1.dp, 18.dp).background(colors.accent))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "● Searching · 5 sources",
                        fontFamily = JetbrainsMonoFamily,
                        fontSize = 11.sp,
                        color = colors.accentSoft,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f),
                    )
                    MonoLabel("36 results", fontSize = 11)
                }
            }

            groups.forEach { (source, count, results) ->
                item(key = "header-$source") {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val gradHue = (source.length * 47f) % 360f
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color.hsl(gradHue, 0.55f, 0.4f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(source[0].toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text(source, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.fg)
                            MonoLabel(count.toString(), fontSize = 11)
                        }
                        TextButton(onClick = {}) {
                            Text("See all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.accentSoft)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                item(key = "row-$source") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.offset(x = (-20).dp).padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        items(results) { manga ->
                            Column(
                                modifier = Modifier
                                    .width(96.dp)
                                    .clickable { onOpenManga(manga) },
                            ) {
                                CoverArtPlaceholder(
                                    hue = manga.hue,
                                    title = manga.title,
                                    author = manga.author,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    manga.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.fg,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
