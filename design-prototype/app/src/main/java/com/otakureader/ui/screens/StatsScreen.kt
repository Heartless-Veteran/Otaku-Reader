package com.otakureader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.AccentCyan
import com.otakureader.ui.theme.AccentPink
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val colors = LocalOtakuColors.current
    val weekData = listOf(12, 24, 18, 32, 28, 45, 38)
    val maxVal = weekData.max().toFloat()

    Scaffold(containerColor = colors.bg) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        MonoLabel("Stats", fontSize = 11, color = colors.fgDim)
                        Text("Reading life", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, color = colors.fg)
                    }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface1),
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Hero stats card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(colors.accent, Color.hsl(290f, 0.6f, 0.45f))
                            )
                        )
                        .padding(20.dp),
                ) {
                    // Decorative circle
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 30.dp, y = (-30).dp)
                            .clip(RoundedCornerShape(80.dp))
                            .background(
                                Brush.radialGradient(listOf(Color.White.copy(0.18f), Color.Transparent))
                            ),
                    )
                    Column {
                        Text("This year", fontFamily = JetbrainsMonoFamily, fontSize = 11.sp, color = Color.White.copy(0.7f), letterSpacing = 1.5.sp)
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text("1,284", fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp, color = Color.White, lineHeight = 44.sp)
                            Text("chapters", fontSize = 14.sp, color = Color.White.copy(0.8f), fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            listOf(
                                Pair("87h", "time read"),
                                Pair("21,486", "pages"),
                                Pair("34", "completed"),
                            ).forEach { (val_, label) ->
                                Column {
                                    Text(val_, fontFamily = JetbrainsMonoFamily, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Text(label, fontSize = 11.sp, color = Color.White.copy(0.7f))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Weekly bar chart
            item {
                OtakuCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            SectionHeader("This week")
                            Text("197 chapters", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.fg, modifier = Modifier.padding(top = 4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.TrendingUp, null, tint = colors.success, modifier = Modifier.size(14.dp))
                            Text("+12%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.success)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        weekData.forEachIndexed { i, v ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight((v / maxVal) * 0.8f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (i == 5) colors.accent else colors.surface3),
                                )
                                Spacer(Modifier.height(6.dp))
                                MonoLabel(days[i], fontSize = 9)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Top genres
            item {
                OtakuCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    SectionHeader("Top genres")
                    Spacer(Modifier.height(14.dp))
                    listOf(
                        Triple("Action", 38, colors.accent),
                        Triple("Romance", 24, AccentPink),
                        Triple("Sci-Fi", 18, AccentCyan),
                        Triple("Drama", 12, colors.warning),
                        Triple("Other", 8, colors.fgDim),
                    ).forEach { (name, pct, color) ->
                        Column(modifier = Modifier.padding(bottom = 10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.fg)
                                MonoLabel("$pct%", fontSize = 12, color = colors.fgMuted)
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colors.surface3),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(pct / 40f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Streak + avg time cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OtakuCard(modifier = Modifier.weight(1f).padding(0.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = colors.accentSoft, modifier = Modifier.size(20.dp))
                        Text("23", fontFamily = JetbrainsMonoFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.fg, modifier = Modifier.padding(top = 10.dp))
                        Text("day streak", fontSize = 11.sp, color = colors.fgMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                    OtakuCard(modifier = Modifier.weight(1f).padding(0.dp)) {
                        Icon(Icons.Default.Timer, null, tint = colors.accentSoft, modifier = Modifier.size(20.dp))
                        Text("2.4m", fontFamily = JetbrainsMonoFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.fg, modifier = Modifier.padding(top = 10.dp))
                        Text("avg / page", fontSize = 11.sp, color = colors.fgMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}
