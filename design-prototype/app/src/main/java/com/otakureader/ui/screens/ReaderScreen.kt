package com.otakureader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.otakureader.data.model.Manga
import com.otakureader.data.model.ReaderMode
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    manga: Manga,
    initialMode: ReaderMode = ReaderMode.PAGED,
    initialBrightness: Float = 0.8f,
    onClose: () -> Unit,
) {
    val colors = LocalOtakuColors.current
    var chrome by remember { mutableStateOf(true) }
    var page by remember { mutableIntStateOf(14) }
    var mode by remember { mutableStateOf(initialMode) }
    var brightness by remember { mutableFloatStateOf(initialBrightness) }
    var crop by remember { mutableStateOf(true) }
    var prefetch by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf("High") }
    var showSettings by remember { mutableStateOf(false) }
    val totalPages = 22

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer { alpha = 0.4f + brightness * 0.6f }
            .pointerInput(Unit) { detectTapGestures { chrome = !chrome } },
    ) {
        // Page content
        when (mode) {
            ReaderMode.PAGED -> {
                ReaderPageMock(
                    pageIdx = page,
                    hue = manga.hue,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ReaderMode.SPREAD -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    ReaderPageMock(
                        pageIdx = page,
                        hue = manga.hue,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                    ReaderPageMock(
                        pageIdx = page + 1,
                        hue = (manga.hue + 30f) % 360f,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
            ReaderMode.WEBTOON -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(3) { i ->
                        ReaderPageMock(
                            pageIdx = page + i,
                            hue = (manga.hue + i * 20f) % 360f,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.9f),
                        )
                    }
                }
            }
        }

        // Top chrome
        if (chrome) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent))
                    )
                    .padding(vertical = 14.dp, horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            manga.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Text(
                            "Chapter ${manga.read + 1}",
                            fontFamily = JetbrainsMonoFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Bookmark, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Bottom chrome
        if (chrome) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            ) {
                Column {
                    // Brightness slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 14.dp),
                    ) {
                        Icon(Icons.Default.BrightnessHigh, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        OtakuSlider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            modifier = Modifier.weight(1f),
                            trackColor = Color.White.copy(alpha = 0.15f),
                            fillColor = Color.White,
                        )
                        Text(
                            text = (brightness * 100).toInt().toString(),
                            fontFamily = JetbrainsMonoFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.width(28.dp),
                        )
                    }

                    // Page scrubber
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconButton(
                            onClick = { if (page > 1) page-- },
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f)),
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        OtakuSlider(
                            value = page.toFloat(),
                            onValueChange = { page = it.toInt().coerceIn(1, totalPages) },
                            valueRange = 1f..totalPages.toFloat(),
                            modifier = Modifier.weight(1f),
                            trackColor = Color.White.copy(alpha = 0.15f),
                            fillColor = colors.accent,
                        )
                        Text(
                            text = "$page/$totalPages",
                            fontFamily = JetbrainsMonoFamily,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.width(48.dp),
                        )
                        IconButton(
                            onClick = { if (page < totalPages) page++ },
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f)),
                        ) {
                            Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Settings sheet
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = colors.surface1,
        ) {
            ReaderSettingsSheet(
                mode = mode, onSetMode = { mode = it },
                crop = crop, onSetCrop = { crop = it },
                prefetch = prefetch, onSetPrefetch = { prefetch = it },
                quality = quality, onSetQuality = { quality = it },
            )
        }
    }
}

@Composable
private fun ReaderPageMock(pageIdx: Int, hue: Float, modifier: Modifier = Modifier) {
    val topColor = Color.hsl(hue.coerceIn(0f, 360f), 0.15f, 0.92f)
    val botColor = Color.hsl(hue.coerceIn(0f, 360f), 0.20f, 0.85f)
    val panelColor = Color.hsl(hue.coerceIn(0f, 360f), 0.25f, 0.55f)

    Canvas(modifier = modifier.background(Brush.linearGradient(listOf(topColor, botColor)))) {
        val panels = listOf(
            listOf(0.06f, 0.04f, 0.88f, 0.25f),
            listOf(0.06f, 0.31f, 0.41f, 0.22f),
            listOf(0.52f, 0.31f, 0.41f, 0.22f),
            listOf(0.06f, 0.56f, 0.88f, 0.28f),
        )
        panels.forEachIndexed { i, p ->
            val ox = p[0] * size.width
            val oy = p[1] * size.height
            val pw = p[2] * size.width
            val ph = p[3] * size.height
            val c = Color.hsl(((hue + i * 40f) % 360f).coerceIn(0f, 360f), 0.3f, 0.35f + i * 0.1f)
            drawRect(color = c, topLeft = Offset(ox, oy), size = Size(pw, ph))
            // Panel border
            drawRect(color = Color.Black.copy(alpha = 0.15f), topLeft = Offset(ox, oy), size = Size(pw, ph), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            // Speech bubble
            if (i % 2 == 0) {
                drawOval(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(ox + pw * 0.55f, oy + ph * 0.2f),
                    size = Size(pw * 0.3f, ph * 0.5f),
                )
            }
        }
    }
}

@Composable
private fun ReaderSettingsSheet(
    mode: ReaderMode,
    onSetMode: (ReaderMode) -> Unit,
    crop: Boolean,
    onSetCrop: (Boolean) -> Unit,
    prefetch: Boolean,
    onSetPrefetch: (Boolean) -> Unit,
    quality: String,
    onSetQuality: (String) -> Unit,
) {
    val colors = LocalOtakuColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("Reader settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.fg)
        Spacer(Modifier.height(16.dp))

        SectionHeader("Layout")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Triple(ReaderMode.PAGED, Icons.Default.MenuBook, "Paged"),
                Triple(ReaderMode.SPREAD, Icons.Default.AutoStories, "Spread"),
                Triple(ReaderMode.WEBTOON, Icons.Default.ViewDay, "Webtoon"),
            ).forEach { (m, icon, label) ->
                val sel = mode == m
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sel) colors.accentDim else colors.surface2)
                        .border(1.dp, if (sel) colors.accent else colors.border, RoundedCornerShape(14.dp))
                        .clickable { onSetMode(m) }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(icon, null, tint = if (sel) colors.accentSoft else colors.fg, modifier = Modifier.size(20.dp))
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (sel) colors.accentSoft else colors.fg)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        listOf(
            Triple("Crop borders", "Trim white margins", crop) to { v: Boolean -> onSetCrop(v) },
            Triple("Prefetch chapters", "Preload next 2 chapters", prefetch) to { v: Boolean -> onSetPrefetch(v) },
        ).forEach { (data, setter) ->
            val (label, sub, checked) = data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .clickable { setter(!checked) }
                    .padding(12.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.fg)
                    Text(sub, fontSize = 11.sp, color = colors.fgDim, modifier = Modifier.padding(top = 2.dp))
                }
                OtakuToggle(checked = checked, onCheckedChange = setter)
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(10.dp))
        SectionHeader("Download quality")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                Triple("low", "Data Saver", "~0.4 MB"),
                Triple("med", "Standard", "~1.2 MB"),
                Triple("high", "High", "~3.5 MB"),
            ).forEach { (id, label, sub) ->
                val sel = quality == id
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) colors.accentDim else colors.surface2)
                        .border(1.dp, if (sel) colors.accent else colors.border, RoundedCornerShape(12.dp))
                        .clickable { onSetQuality(id) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (sel) colors.accentSoft else colors.fg)
                    MonoLabel(sub, fontSize = 10, color = (if (sel) colors.accentSoft else colors.fgMuted).copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
