package app.otakureader.feature.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.otakureader.domain.model.ReadingDirection
import app.otakureader.feature.reader.R

// Layout tokens for the chapter-navigator row (slider flanked by prev/next-chapter buttons).
private val NAV_ROW_PADDING_HORIZONTAL = 8.dp
private val NAV_ROW_PADDING_VERTICAL = 8.dp
private val NAV_ROW_ITEM_SPACING = 4.dp
private const val SLIDER_SURFACE_ALPHA = 0.95f
private val SLIDER_SURFACE_ELEVATION = 8.dp

/**
 * Draggable page slider (seekbar) for the reader.
 *
 * Displays a Material 3 [Slider] that lets users scrub through pages by dragging.
 * For RTL reading direction the slider is horizontally mirrored so that dragging
 * right moves to earlier pages, matching the manga reading flow.
 *
 * Mirrors Mihon/Komikku's `ChapterNavigator`: the page seekbar is flanked by previous- and
 * next-chapter buttons. For RTL the two buttons swap which chapter they load (the left button
 * always carries the "skip previous" glyph but moves to the next chapter under RTL), matching
 * the mirrored slider.
 *
 * @param currentPage   0-based index of the currently displayed page.
 * @param totalPages    Total number of pages in the chapter.
 * @param onPageSeek    Callback invoked with a 0-based page index while the user drags.
 * @param onPreviousChapter Loads the chapter before the current one (reading order).
 * @param onNextChapter Loads the chapter after the current one (reading order).
 * @param hasPreviousChapter Whether a previous chapter exists (disables the button otherwise).
 * @param hasNextChapter Whether a next chapter exists (disables the button otherwise).
 * @param readingDirection Current reading direction; RTL reverses the slider visually.
 * @param isVisible     Controls the animated visibility of the slider.
 * @param modifier      Optional [Modifier] for the outer container.
 */
@Composable
fun PageSlider(
    currentPage: Int,
    totalPages: Int,
    onPageSeek: (Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    readingDirection: ReadingDirection = ReadingDirection.LTR,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        // Guard against totalPages == 0 (can occur during the AnimatedVisibility exit transition).
        // Wrapping the Surface — rather than returning from its content lambda — keeps the elevated
        // bar from flashing empty while the menu animates away.
        if (totalPages > 0) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = SLIDER_SURFACE_ALPHA),
                tonalElevation = SLIDER_SURFACE_ELEVATION
            ) {
                val isRtl = readingDirection == ReadingDirection.RTL
                // Under RTL the on-screen left/right buttons swap which chapter they load, so the
                // physical layout still reads skip-previous | slider | skip-next.
                val leftEnabled = if (isRtl) hasNextChapter else hasPreviousChapter
                val leftClick = if (isRtl) onNextChapter else onPreviousChapter
                val leftDesc = stringResource(
                    if (isRtl) R.string.reader_next_chapter else R.string.reader_previous_chapter
                )
                val rightEnabled = if (isRtl) hasPreviousChapter else hasNextChapter
                val rightClick = if (isRtl) onPreviousChapter else onNextChapter
                val rightDesc = stringResource(
                    if (isRtl) R.string.reader_previous_chapter else R.string.reader_next_chapter
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = NAV_ROW_PADDING_HORIZONTAL,
                            vertical = NAV_ROW_PADDING_VERTICAL,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NAV_ROW_ITEM_SPACING),
                ) {
                    FilledIconButton(onClick = leftClick, enabled = leftEnabled) {
                        Icon(imageVector = Icons.Outlined.SkipPrevious, contentDescription = leftDesc)
                    }

                    PageSeekColumn(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageSeek = onPageSeek,
                        readingDirection = readingDirection,
                        modifier = Modifier.weight(1f),
                    )

                    FilledIconButton(onClick = rightClick, enabled = rightEnabled) {
                        Icon(imageVector = Icons.Outlined.SkipNext, contentDescription = rightDesc)
                    }
                }
            }
        }
    }
}

/**
 * The page label row plus the draggable seekbar. Extracted so [PageSlider] can flank it with the
 * previous/next-chapter buttons.
 */
@Composable
private fun PageSeekColumn(
    currentPage: Int,
    totalPages: Int,
    onPageSeek: (Int) -> Unit,
    readingDirection: ReadingDirection,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
                // Page label row: e.g. "5 / 120"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val firstLabel = if (readingDirection == ReadingDirection.RTL) totalPages.toString() else "1"
                    val lastLabel = if (readingDirection == ReadingDirection.RTL) "1" else totalPages.toString()

                    Text(
                        text = firstLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = lastLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Track slider drag value locally so scrubbing feels responsive.
                // We initialize from the external currentPage and keep in sync when
                // the external value changes (e.g. page turned via tap zone).
                var sliderValue by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }

                // Only emit onPageSeek when the integer page index actually changes to
                // avoid unnecessary churn from sub-integer float movements during a drag.
                var lastEmittedPage by remember(currentPage) { mutableIntStateOf(currentPage) }

                val maxValue = (totalPages - 1).coerceAtLeast(0).toFloat()

                // Apply horizontal mirror for RTL so dragging right → earlier pages.
                val sliderModifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (readingDirection == ReadingDirection.RTL) {
                            Modifier.graphicsLayer(scaleX = -1f)
                        } else {
                            Modifier
                        }
                    )

                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        sliderValue = newValue
                        val newPage = newValue.toInt()
                        if (newPage != lastEmittedPage) {
                            lastEmittedPage = newPage
                            onPageSeek(newPage)
                        }
                    },
                    valueRange = 0f..maxValue,
                    steps = if (totalPages > 2) totalPages - 2 else 0,
                    modifier = sliderModifier
                )
    }
}
