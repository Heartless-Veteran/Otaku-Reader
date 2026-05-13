package app.otakureader.feature.reader.ui

import app.otakureader.core.ui.theme.LocalOtakuColors
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.otakureader.feature.reader.PageRotation
import app.otakureader.feature.reader.R
import app.otakureader.feature.reader.TapZone
import app.otakureader.feature.reader.model.ReaderPage
import coil3.compose.AsyncImage

/**
 * Bottom thumbnail strip for quick page navigation.
 * Shows a horizontal scrollable row of page thumbnails.
 */
@Composable
fun PageThumbnailStrip(
    pages: List<ReaderPage>,
    currentPage: Int,
    onPageClick: (Int) -> Unit,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val otaku = LocalOtakuColors.current
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = otaku.surface1.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                // Progress line + page counter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    // Thin progress track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(otaku.surface3)
                            .align(Alignment.Center)
                    )
                    // Progress fill
                    val progress = if (pages.isNotEmpty()) {
                        (currentPage.toFloat() / pages.size.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(2.dp)
                            .background(otaku.accent)
                            .align(Alignment.CenterStart)
                    )

                    Text(
                        text = "${currentPage + 1} / ${pages.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = otaku.fgMuted,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Thumbnail row
                val listState = rememberLazyListState()

                LaunchedEffect(currentPage) {
                    listState.animateScrollToItem(
                        index = currentPage.coerceIn(0, pages.size - 1),
                        scrollOffset = -120
                    )
                }

                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = pages,
                        key = { _, page -> page.id },
                        contentType = { _, _ -> "thumbnail" }
                    ) { index, page ->
                        PageThumbnailItem(
                            page = page,
                            pageNumber = index + 1,
                            isSelected = index == currentPage,
                            onClick = { onPageClick(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageThumbnailItem(
    page: ReaderPage,
    pageNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val otaku = LocalOtakuColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 120.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(otaku.surface2)
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = page.imageUrl ?: page.thumbnailUrl,
                contentDescription = stringResource(R.string.reader_page_number, pageNumber),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )

            // Selection indicator — accent border instead of overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(3.dp)
                        .border(
                            width = 3.dp,
                            color = otaku.accent,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        // Page number
        Text(
            text = pageNumber.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                otaku.accent
            } else {
                otaku.fgMuted
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
