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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.ui.AppSettings
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settings: AppSettings,
    onSetTheme: (AppTheme) -> Unit,
    onSetAccent: (Color) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
) {
    val colors = LocalOtakuColors.current

    val themes = listOf(
        Triple(AppTheme.DARK, "Dark", Color(0xFF14141F)),
        Triple(AppTheme.OLED, "OLED", Color(0xFF000000)),
        Triple(AppTheme.LIGHT, "Light", Color(0xFFFFFFFF)),
        Triple(AppTheme.SEPIA, "Sepia", Color(0xFFF4ECD8)),
    )
    val themeSubtitles = listOf("Material You", "Pure black", "Daytime", "Reading")

    val accentOptions = listOf(
        Color(0xFF6B4EFF), Color(0xFFFF6B9D), Color(0xFF4ADE80),
        Color(0xFF4DDFFF), Color(0xFFFBBF24), Color(0xFFF87171),
    )

    Scaffold(containerColor = colors.bg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface1),
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                }
                Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.fg)
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SectionHeader("Appearance")
                    Spacer(Modifier.height(12.dp))

                    // Theme grid
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        themes.forEachIndexed { i, (theme, label, bg) ->
                            val selected = settings.theme == theme
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.surface1)
                                    .border(
                                        if (selected) 1.5.dp else 1.dp,
                                        if (selected) colors.accent else colors.border,
                                        RoundedCornerShape(16.dp),
                                    )
                                    .clickable { onSetTheme(theme) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bg)
                                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(AccentPurple)
                                            .align(Alignment.BottomCenter)
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                    )
                                }
                                Column {
                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.fg)
                                    Text(themeSubtitles[i], fontSize = 11.sp, color = colors.fgDim, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                item {
                    SectionHeader("Accent color")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        accentOptions.forEach { accent ->
                            val selected = settings.accent == accent
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accent)
                                    .border(
                                        if (selected) 2.dp else 1.dp,
                                        if (selected) Color.White else colors.border,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable { onSetAccent(accent) },
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                item {
                    listOf(
                        Triple("Dynamic colors", "Match wallpaper (Material You)", settings.dynamicColor),
                        Triple("Page transition animations", "Smooth fades in reader", true),
                        Triple("Cover progress bars", "Show on library grid", true),
                        Triple("Incognito mode", "Pause history tracking", false),
                    ).forEach { (label, sub, checked) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surface1)
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                .clickable(onClick = {
                                    if (label == "Dynamic colors") onSetDynamicColor(!settings.dynamicColor)
                                })
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.fg)
                                Text(sub, fontSize = 11.sp, color = colors.fgDim, modifier = Modifier.padding(top = 2.dp))
                            }
                            OtakuToggle(checked = checked, onCheckedChange = {
                                if (label == "Dynamic colors") onSetDynamicColor(it)
                            })
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
