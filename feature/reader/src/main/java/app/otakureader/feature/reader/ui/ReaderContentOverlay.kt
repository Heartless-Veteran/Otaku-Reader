package app.otakureader.feature.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.otakureader.core.ui.theme.LocalOtakuColors
import app.otakureader.core.ui.theme.OtakuReaderTheme
import app.otakureader.feature.reader.R

// Reader-overlay layout tokens.
private const val OVERLAY_BACKGROUND_ALPHA = 0.93f
private val OVERLAY_BACKGROUND = Color(0xFF0A0A0F)

/**
 * Reader top app bar shown while the menu is visible: back, manga title + chapter subtitle, and
 * optional download / bookmark / settings actions.
 *
 * This is the clean Mihon/Komikku-style top chrome — plain Material 3 widgets only. Page
 * scrubbing and previous/next-chapter navigation live in the bottom bar (`PageSlider`, Komikku's
 * ChapterNavigator) and thumbnails in `PageThumbnailStrip`, so this overlay intentionally carries
 * no slider, filmstrip, or chapter buttons (they would duplicate the bottom controls). It
 * previously branched on content type into ink/panel and neon/glassmorphism variants; those
 * Otaku-exclusive styles were dropped for visual parity with Komikku.
 *
 * @param title         Manga series title displayed in the top bar.
 * @param chapterTitle  Current chapter title displayed below the series title.
 * @param isVisible     Whether the overlay is shown.
 * @param onDismiss     Called when the back arrow is tapped — typically navigates back.
 * @param onSettingsClick Called when the settings icon is tapped.
 * @param onDownloadChapter Called when the download icon is tapped; null hides the button.
 * @param isCurrentChapterDownloaded When true, the download button is hidden.
 * @param onBookmarkPage Called when the bookmark icon is tapped; null hides the button.
 * @param isCurrentPageBookmarked When true, shows a filled bookmark icon.
 */
@Composable
fun ReaderContentOverlay(
    title: String,
    chapterTitle: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
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
        val mutedColor = LocalOtakuColors.current.unselectedPageIndicator

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OVERLAY_BACKGROUND.copy(alpha = OVERLAY_BACKGROUND_ALPHA))
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
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun ReaderContentOverlayPreview() {
    OtakuReaderTheme {
        ReaderContentOverlay(
            title = "Berserk",
            chapterTitle = "Chapter 364: The Elf King",
            isVisible = true,
            onDismiss = {},
            onSettingsClick = {},
            onDownloadChapter = {},
            onBookmarkPage = {}
        )
    }
}
