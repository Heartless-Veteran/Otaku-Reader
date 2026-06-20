package app.otakureader.feature.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.otakureader.core.ui.components.GlassmorphismSheet
import app.otakureader.core.ui.components.GlowButton
import app.otakureader.core.ui.components.InkButton
import app.otakureader.core.ui.components.InkSlider
import app.otakureader.core.ui.components.NeonSlider
import app.otakureader.core.ui.theme.ContentType
import app.otakureader.core.ui.theme.LocalOtakuColors
import app.otakureader.core.ui.theme.OtakuReaderTheme
import app.otakureader.core.ui.theme.manhwaAccent
import app.otakureader.feature.reader.R

/**
 * Content-type-aware reader overlay showing the top controls, filmstrip, and page slider.
 *
 * Manga variant uses an ink/panel aesthetic; Manhwa variant uses neon/glassmorphism.
 * When [visualEffectsEnabled] is false both variants fall back to plain Material 3 widgets.
 *
 * The filmstrip and slider are mutually exclusive — a toggle icon button next to the page
 * counter lets the user switch between them.
 *
 * @param title         Manga series title displayed in the top bar.
 * @param chapterTitle  Current chapter title displayed below the series title.
 * @param currentPage   1-based current page number.
 * @param totalPages    Total pages in the current chapter.
 * @param isVisible     Whether the overlay is shown.
 * @param contentType   [ContentType.MANGA] or [ContentType.MANHWA] — selects the visual style.
 * @param visualEffectsEnabled When true, custom ink/neon widgets are used. When false, plain
 *                             Material 3 controls are used (respects user preference).
 * @param onDismiss     Called when the back arrow is tapped — typically navigates back.
 * @param onSettingsClick Called when the settings icon is tapped.
 * @param onDownloadChapter Called when the download icon is tapped; null hides the button.
 * @param isCurrentChapterDownloaded When true, the download button is hidden.
 * @param onPrevChapter Called when "Prev chapter" is tapped.
 * @param onNextChapter Called when "Next chapter" is tapped.
 * @param onPageSliderChange Called with a 0–1 normalized value as the user scrubs the slider.
 * @param onThumbnailClick Called with a 1-based page number when a filmstrip thumbnail is tapped.
 */
@Composable
fun ReaderContentOverlay(
    title: String,
    chapterTitle: String,
    currentPage: Int,
    totalPages: Int,
    isVisible: Boolean,
    contentType: ContentType,
    visualEffectsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadChapter: (() -> Unit)? = null,
    isCurrentChapterDownloaded: Boolean = false,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onPageSliderChange: (Float) -> Unit,
    onThumbnailClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { -it / 4 }),
        exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -it / 4 }),
        modifier = modifier
    ) {
        if (contentType == ContentType.MANGA) {
            MangaReaderOverlayContent(
                title = title,
                chapterTitle = chapterTitle,
                currentPage = currentPage,
                totalPages = totalPages,
                visualEffectsEnabled = visualEffectsEnabled,
                onDismiss = onDismiss,
                onSettingsClick = onSettingsClick,
                onDownloadChapter = onDownloadChapter,
                isCurrentChapterDownloaded = isCurrentChapterDownloaded,
                onPrevChapter = onPrevChapter,
                onNextChapter = onNextChapter,
                onPageSliderChange = onPageSliderChange,
                onThumbnailClick = onThumbnailClick
            )
        } else {
            ManhwaReaderOverlayContent(
                title = title,
                chapterTitle = chapterTitle,
                currentPage = currentPage,
                totalPages = totalPages,
                visualEffectsEnabled = visualEffectsEnabled,
                onDismiss = onDismiss,
                onSettingsClick = onSettingsClick,
                onDownloadChapter = onDownloadChapter,
                isCurrentChapterDownloaded = isCurrentChapterDownloaded,
                onPrevChapter = onPrevChapter,
                onNextChapter = onNextChapter,
                onPageSliderChange = onPageSliderChange,
                onThumbnailClick = onThumbnailClick
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Manga overlay — ink / panel aesthetic, dark background
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MangaReaderOverlayContent(
    title: String,
    chapterTitle: String,
    currentPage: Int,
    totalPages: Int,
    visualEffectsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadChapter: (() -> Unit)? = null,
    isCurrentChapterDownloaded: Boolean = false,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onPageSliderChange: (Float) -> Unit,
    onThumbnailClick: (Int) -> Unit
) {
    var showFilmstrip by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0F).copy(alpha = 0.93f))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    chapterTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalOtakuColors.current.unselectedPageIndicator
                )
            }
            if (onDownloadChapter != null && !isCurrentChapterDownloaded) {
                IconButton(onClick = onDownloadChapter) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = stringResource(R.string.reader_download_chapter),
                        tint = LocalOtakuColors.current.unselectedPageIndicator
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = LocalOtakuColors.current.unselectedPageIndicator
                )
            }
        }

        // Page counter + filmstrip/slider toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Page $currentPage / $totalPages",
                style = MaterialTheme.typography.labelMedium,
                color = LocalOtakuColors.current.unselectedPageIndicator,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showFilmstrip = !showFilmstrip }) {
                Icon(
                    imageVector = if (showFilmstrip) Icons.Default.LinearScale else Icons.Default.ViewModule,
                    contentDescription = if (showFilmstrip) {
                        stringResource(R.string.reader_switch_to_slider)
                    } else {
                        stringResource(R.string.reader_switch_to_filmstrip)
                    },
                    tint = LocalOtakuColors.current.unselectedPageIndicator
                )
            }
        }

        // Filmstrip OR slider — never both
        if (showFilmstrip) {
            val filmstripCount = totalPages.coerceAtMost(50)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(Color(0xFF12121A)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count = filmstripCount, key = { it + 1 }) { idx ->
                    val page = idx + 1
                    val isSelected = page == currentPage
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .fillMaxHeight()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) {
                                    LocalOtakuColors.current.selectedPageIndicator
                                } else {
                                    LocalOtakuColors.current.unselectedPageIndicator
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                            .clickable { onThumbnailClick(page) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$page",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) LocalOtakuColors.current.selectedPageIndicator
                                    else LocalOtakuColors.current.unselectedPageIndicator,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val sliderValue = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                if (visualEffectsEnabled) {
                    InkSlider(value = sliderValue, onValueChange = onPageSliderChange)
                } else {
                    Slider(value = sliderValue, onValueChange = onPageSliderChange)
                }
            }
        }

        // Chapter navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (visualEffectsEnabled) {
                InkButton(onClick = onPrevChapter) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.reader_previous_chapter),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("PREV", style = MaterialTheme.typography.labelMedium)
                }
                InkButton(onClick = onNextChapter) {
                    Text("NEXT", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = stringResource(R.string.reader_next_chapter),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                OutlinedButton(onClick = onPrevChapter) { Text("← Prev") }
                OutlinedButton(onClick = onNextChapter) { Text("Next →") }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Manhwa overlay — neon / glassmorphism aesthetic
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ManhwaReaderOverlayContent(
    title: String,
    chapterTitle: String,
    currentPage: Int,
    totalPages: Int,
    visualEffectsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadChapter: (() -> Unit)? = null,
    isCurrentChapterDownloaded: Boolean = false,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onPageSliderChange: (Float) -> Unit,
    onThumbnailClick: (Int) -> Unit
) {
    val accentColor = ContentType.MANHWA.manhwaAccent()
    var showFilmstrip by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D12).copy(alpha = 0.88f))
            .then(
                if (visualEffectsEnabled) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    chapterTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalOtakuColors.current.unselectedPageIndicator
                )
            }
            if (onDownloadChapter != null && !isCurrentChapterDownloaded) {
                IconButton(onClick = onDownloadChapter) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = stringResource(R.string.reader_download_chapter),
                        tint = accentColor
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = accentColor
                )
            }
        }

        // Page counter + filmstrip/slider toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Page $currentPage / $totalPages",
                style = MaterialTheme.typography.labelMedium,
                color = LocalOtakuColors.current.unselectedPageIndicator,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showFilmstrip = !showFilmstrip }) {
                Icon(
                    imageVector = if (showFilmstrip) Icons.Default.LinearScale else Icons.Default.ViewModule,
                    contentDescription = if (showFilmstrip) {
                        stringResource(R.string.reader_switch_to_slider)
                    } else {
                        stringResource(R.string.reader_switch_to_filmstrip)
                    },
                    tint = if (showFilmstrip) accentColor else LocalOtakuColors.current.unselectedPageIndicator
                )
            }
        }

        // Episode thumbnail strip OR slider — never both
        if (showFilmstrip) {
            val filmstripCount = totalPages.coerceAtMost(50)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFF16161F).copy(alpha = 0.6f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(count = filmstripCount, key = { it + 1 }) { idx ->
                    val page = idx + 1
                    val isSelected = page == currentPage
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxHeight()
                            .background(
                                color = if (isSelected) accentColor.copy(alpha = 0.2f)
                                else Color(0xFF1E1E2A),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accentColor else Color(0xFF3A3A4A),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onThumbnailClick(page) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$page",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) accentColor else LocalOtakuColors.current.unselectedPageIndicator,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val sliderValue = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
            if (visualEffectsEnabled) {
                GlassmorphismSheet(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    NeonSlider(
                        value = sliderValue,
                        onValueChange = onPageSliderChange,
                        glowColor = accentColor
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Slider(value = sliderValue, onValueChange = onPageSliderChange)
                }
            }
        }

        // Chapter navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (visualEffectsEnabled) {
                GlowButton(onClick = onPrevChapter, glowColor = accentColor) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.reader_previous_chapter),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("PREV", style = MaterialTheme.typography.labelMedium)
                }
                GlowButton(onClick = onNextChapter, glowColor = Color(0xFF00D2D3)) {
                    Text("NEXT", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = stringResource(R.string.reader_next_chapter),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                OutlinedButton(onClick = onPrevChapter) { Text("← Prev") }
                OutlinedButton(onClick = onNextChapter) { Text("Next →") }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun ReaderContentOverlayMangaPreview() {
    OtakuReaderTheme {
        ReaderContentOverlay(
            title = "Berserk",
            chapterTitle = "Chapter 364: The Elf King",
            currentPage = 12,
            totalPages = 28,
            isVisible = true,
            contentType = ContentType.MANGA,
            visualEffectsEnabled = true,
            onDismiss = {},
            onSettingsClick = {},
            onPrevChapter = {},
            onNextChapter = {},
            onPageSliderChange = {},
            onThumbnailClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun ReaderContentOverlayManhwaPreview() {
    OtakuReaderTheme {
        ReaderContentOverlay(
            title = "Solo Leveling",
            chapterTitle = "Chapter 179",
            currentPage = 8,
            totalPages = 22,
            isVisible = true,
            contentType = ContentType.MANHWA,
            visualEffectsEnabled = true,
            onDismiss = {},
            onSettingsClick = {},
            onPrevChapter = {},
            onNextChapter = {},
            onPageSliderChange = {},
            onThumbnailClick = {}
        )
    }
}
