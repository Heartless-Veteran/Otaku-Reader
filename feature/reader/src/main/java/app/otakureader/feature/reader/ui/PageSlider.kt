package app.otakureader.feature.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.otakureader.domain.model.ReadingDirection
import app.otakureader.feature.reader.R

private val NAV_ROW_PADDING_HORIZONTAL = 8.dp
private val NAV_ROW_PADDING_VERTICAL = 8.dp
private val NAV_ROW_ITEM_SPACING = 4.dp
private val NAV_BAR_ELEVATION = 3.dp
private val NAV_PILL_CORNER = 24.dp
private val NAV_PILL_HORIZONTAL_PADDING = 16.dp
private val NAV_SLIDER_HORIZONTAL_PADDING = 8.dp
private const val BAR_ALPHA_DARK = 0.9f
private const val BAR_ALPHA_LIGHT = 0.95f
private const val SLIDER_MIN_TOTAL_PAGES = 1  // slider only renders when at least one page exists
private const val PAGE_DISPLAY_OFFSET = 1     // converts between 0-based page index and 1-based display

/**
 * Draggable page slider (chapter navigator) for the reader.
 *
 * Matches Komikku's ChapterNavigator: the page seekbar with current page / total pages labels
 * is flanked by previous- and next-chapter buttons. For RTL the layout direction of the slider
 * section is flipped so that dragging right moves to earlier pages. Provides haptic feedback
 * via [LocalHapticFeedback] whenever the slider changes page while the user is dragging.
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
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        if (totalPages >= SLIDER_MIN_TOTAL_PAGES) {
            val isRtl = readingDirection == ReadingDirection.RTL
            val haptic = LocalHapticFeedback.current
            val backgroundColor = MaterialTheme.colorScheme
                .surfaceColorAtElevation(NAV_BAR_ELEVATION)
                .copy(alpha = if (isSystemInDarkTheme()) BAR_ALPHA_DARK else BAR_ALPHA_LIGHT)
            val buttonColors = IconButtonDefaults.filledIconButtonColors(
                containerColor = backgroundColor,
                disabledContainerColor = backgroundColor,
                contentColor = MaterialTheme.colorScheme.primary,
            )

            // Force the outer row to LTR so button positions are stable regardless of system locale.
            // The inner slider section uses the reading direction independently.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(
                            horizontal = NAV_ROW_PADDING_HORIZONTAL,
                            vertical = NAV_ROW_PADDING_VERTICAL,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NAV_ROW_ITEM_SPACING),
                ) {
                    // Under RTL the on-screen left button skips to next chapter so the physical
                    // layout reads skip-previous | slider | skip-next from the reader's perspective.
                    val leftEnabled = if (isRtl) hasNextChapter else hasPreviousChapter
                    val leftClick = if (isRtl) onNextChapter else onPreviousChapter
                    val leftDesc = stringResource(
                        if (isRtl) R.string.reader_next_chapter else R.string.reader_previous_chapter,
                    )
                    val rightEnabled = if (isRtl) hasPreviousChapter else hasNextChapter
                    val rightClick = if (isRtl) onPreviousChapter else onNextChapter
                    val rightDesc = stringResource(
                        if (isRtl) R.string.reader_previous_chapter else R.string.reader_next_chapter,
                    )

                    FilledIconButton(onClick = leftClick, enabled = leftEnabled, colors = buttonColors) {
                        Icon(imageVector = Icons.Outlined.SkipPrevious, contentDescription = leftDesc)
                    }

                    if (totalPages > 1) {
                        val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

                        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(NAV_PILL_CORNER))
                                    .background(backgroundColor)
                                    .padding(horizontal = NAV_PILL_HORIZONTAL_PADDING),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val textColor = MaterialTheme.colorScheme.onSurface

                                // Current page label. Ghost copy uses '8' repeated to match
                                // totalPages digit count — '8' is the widest digit in proportional
                                // fonts, so this reserves the maximum possible width regardless of
                                // which digits actually appear.
                                val ghostText = remember(totalPages) {
                                    buildString { repeat(totalPages.toString().length) { append('8') } }
                                }
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    Text(
                                        text = (currentPage + PAGE_DISPLAY_OFFSET).toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                    )
                                    Text(
                                        text = ghostText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Transparent,
                                    )
                                }

                                val interactionSource = remember { MutableInteractionSource() }
                                val sliderDragged by interactionSource.collectIsDraggedAsState()
                                // Haptic tick every time the integer page changes while dragging.
                                LaunchedEffect(currentPage) {
                                    if (sliderDragged) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }

                                // Local float state keeps scrubbing responsive. Not keyed on
                                // currentPage to avoid thumb jitter during active drags: the
                                // LaunchedEffect below syncs external page changes only when the
                                // user is not dragging.
                                var sliderValue by remember {
                                    mutableFloatStateOf((currentPage + PAGE_DISPLAY_OFFSET).toFloat())
                                }
                                LaunchedEffect(currentPage, sliderDragged) {
                                    if (!sliderDragged) {
                                        sliderValue = (currentPage + PAGE_DISPLAY_OFFSET).toFloat()
                                    }
                                }
                                var lastEmittedPage by remember(currentPage) {
                                    mutableIntStateOf(currentPage)
                                }
                                val maxValue = totalPages.toFloat()

                                Slider(
                                    value = sliderValue,
                                    onValueChange = { newValue ->
                                        sliderValue = newValue
                                        val newPage = newValue.toInt().coerceIn(PAGE_DISPLAY_OFFSET, totalPages) - PAGE_DISPLAY_OFFSET
                                        if (newPage != lastEmittedPage) {
                                            lastEmittedPage = newPage
                                            onPageSeek(newPage)
                                        }
                                    },
                                    valueRange = PAGE_DISPLAY_OFFSET.toFloat()..maxValue,
                                    interactionSource = interactionSource,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = NAV_SLIDER_HORIZONTAL_PADDING),
                                )

                                Text(
                                    text = totalPages.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    FilledIconButton(onClick = rightClick, enabled = rightEnabled, colors = buttonColors) {
                        Icon(imageVector = Icons.Outlined.SkipNext, contentDescription = rightDesc)
                    }
                }
            }
        }
    }
}
