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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.otakureader.core.ui.theme.LocalOtakuColors
import app.otakureader.core.ui.theme.OtakuReaderTheme
import app.otakureader.feature.reader.R

// Reader-overlay layout tokens.
private const val OVERLAY_BACKGROUND_ALPHA = 0.93f
private val OVERLAY_BACKGROUND = Color(0xFF0A0A0F)
private val FILMSTRIP_BACKGROUND = Color(0xFF12121A)
private const val FILMSTRIP_MAX_THUMBS = 50
private val FILMSTRIP_HEIGHT = 70.dp
private val FILMSTRIP_THUMB_WIDTH = 50.dp
private val NAV_ICON_SIZE = 16.dp

/**
 * Reader chrome overlay shown at the top of the screen while the menu is visible: a top bar
 * (back, title/chapter, optional download + bookmark, settings), a page counter with a
 * filmstrip/slider toggle, the filmstrip **or** page slider, and previous/next-chapter buttons.
 *
 * This is the clean Mihon/Komikku-style overlay — plain Material 3 widgets only. (It previously
 * branched on content type into ink/panel and neon/glassmorphism variants; those Otaku-exclusive
 * styles were dropped for visual parity with Komikku.)
 *
 * @param title         Manga series title displayed in the top bar.
 * @param chapterTitle  Current chapter title displayed below the series title.
 * @param currentPage   1-based current page number.
 * @param totalPages    Total pages in the current chapter.
 * @param isVisible     Whether the overlay is shown.
 * @param onDismiss     Called when the back arrow is tapped — typically navigates back.
 * @param onSettingsClick Called when the settings icon is tapped.
 * @param onDownloadChapter Called when the download icon is tapped; null hides the button.
 * @param isCurrentChapterDownloaded When true, the download button is hidden.
 * @param onBookmarkPage Called when the bookmark icon is tapped; null hides the button.
 * @param isCurrentPageBookmarked When true, shows a filled bookmark icon.
 * @param onPrevChapter Called when "Previous chapter" is tapped.
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
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onPageSliderChange: (Float) -> Unit,
    onThumbnailClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDownloadChapter: (() -> Unit)? = null,
    isCurrentChapterDownloaded: Boolean = false,
    onBookmarkPage: (() -> Unit)? = null,
    isCurrentPageBookmarked: Boolean = false,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { -it / 4 }),
        exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -it / 4 }),
        modifier = modifier
    ) {
        var showFilmstrip by rememberSaveable { mutableStateOf(true) }
        val mutedColor = LocalOtakuColors.current.unselectedPageIndicator

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OVERLAY_BACKGROUND.copy(alpha = OVERLAY_BACKGROUND_ALPHA))
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
                        contentDescription = stringResource(R.string.reader_back),
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
                        color = mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onDownloadChapter != null && !isCurrentChapterDownloaded) {
                    IconButton(onClick = onDownloadChapter) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = stringResource(R.string.reader_download_chapter),
                            tint = mutedColor
                        )
                    }
                }
                if (onBookmarkPage != null) {
                    IconButton(onClick = onBookmarkPage) {
                        Icon(
                            imageVector = if (isCurrentPageBookmarked) {
                                Icons.Default.Bookmark
                            } else {
                                Icons.Outlined.BookmarkBorder
                            },
                            contentDescription = stringResource(R.string.reader_bookmark_page),
                            tint = if (isCurrentPageBookmarked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                mutedColor
                            }
                        )
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.reader_settings),
                        tint = mutedColor
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
                    stringResource(R.string.reader_page_indicator, currentPage, totalPages),
                    style = MaterialTheme.typography.labelMedium,
                    color = mutedColor,
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
                        tint = mutedColor
                    )
                }
            }

            // Filmstrip OR slider — never both
            if (showFilmstrip) {
                val filmstripCount = totalPages.coerceAtMost(FILMSTRIP_MAX_THUMBS)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FILMSTRIP_HEIGHT)
                        .background(FILMSTRIP_BACKGROUND),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(count = filmstripCount, key = { it + 1 }) { idx ->
                        val page = idx + 1
                        val isSelected = page == currentPage
                        val indicatorColor = if (isSelected) {
                            LocalOtakuColors.current.selectedPageIndicator
                        } else {
                            mutedColor
                        }
                        Box(
                            modifier = Modifier
                                .width(FILMSTRIP_THUMB_WIDTH)
                                .fillMaxHeight()
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = indicatorColor,
                                    shape = RoundedCornerShape(2.dp)
                                )
                                .clickable { onThumbnailClick(page) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$page",
                                style = MaterialTheme.typography.labelSmall,
                                color = indicatorColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                val sliderValue = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Slider(value = sliderValue, onValueChange = onPageSliderChange)
                }
            }

            // Previous / next chapter buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = onPrevChapter) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(NAV_ICON_SIZE)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.reader_previous_chapter))
                }
                OutlinedButton(onClick = onNextChapter) {
                    Text(stringResource(R.string.reader_next_chapter))
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null,
                        modifier = Modifier.size(NAV_ICON_SIZE)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun ReaderContentOverlayPreview() {
    OtakuReaderTheme {
        ReaderContentOverlay(
            title = "Berserk",
            chapterTitle = "Chapter 364: The Elf King",
            currentPage = 12,
            totalPages = 28,
            isVisible = true,
            onDismiss = {},
            onSettingsClick = {},
            onPrevChapter = {},
            onNextChapter = {},
            onPageSliderChange = {},
            onThumbnailClick = {}
        )
    }
}
