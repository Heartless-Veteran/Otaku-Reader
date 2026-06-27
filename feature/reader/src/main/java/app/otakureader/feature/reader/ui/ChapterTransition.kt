package app.otakureader.feature.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.otakureader.feature.reader.R
import kotlin.math.floor
import kotlin.math.max

private val TRANSITION_MAX_WIDTH = 460.dp
private val TRANSITION_CONTENT_PADDING = 24.dp
private val TRANSITION_SPACER_HEIGHT = 24.dp
private val TRANSITION_CARD_HORIZONTAL_PADDING = 16.dp
private val TRANSITION_CARD_VERTICAL_PADDING = 12.dp
private const val TRANSITION_BG_ALPHA = 0.92f
private const val CHAPTER_TITLE_MAX_LINES = 5

/**
 * Full-screen overlay shown at chapter boundaries when [isVisible] is true.
 *
 * Matches Komikku's ChapterTransition composable: shows a "Finished / Next" card at the
 * last page of a chapter, and a "Previous / Current" card at the first page. Includes a
 * gap warning card if the chapter numbers indicate missing chapters between the two.
 *
 * @param isVisible           Whether the overlay is currently shown.
 * @param isTransitionToNext  True when at the last page (forward → next chapter).
 *                            False when at the first page (backward → previous chapter).
 * @param currentChapterTitle Title of the chapter currently loaded in the reader.
 * @param currentChapterNumber Chapter number of the current chapter (−1 if unknown).
 * @param isCurrentChapterDownloaded Whether the current chapter is downloaded.
 * @param adjacentChapterTitle Title of the next (or previous) chapter, or null if none.
 * @param adjacentChapterNumber Chapter number of the adjacent chapter, or null if none.
 * @param isAdjacentChapterDownloaded Whether the adjacent chapter is downloaded.
 * @param modifier Optional [Modifier] for the root container.
 */
@Composable
fun ChapterTransition(
    isVisible: Boolean,
    isTransitionToNext: Boolean,
    currentChapterTitle: String,
    currentChapterNumber: Float,
    isCurrentChapterDownloaded: Boolean,
    adjacentChapterTitle: String?,
    adjacentChapterNumber: Float?,
    isAdjacentChapterDownloaded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = TRANSITION_BG_ALPHA),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    if (isTransitionToNext) {
                        TransitionText(
                            topLabel = stringResource(R.string.reader_transition_finished),
                            topChapterTitle = currentChapterTitle,
                            topChapterDownloaded = isCurrentChapterDownloaded,
                            bottomLabel = stringResource(R.string.reader_transition_next),
                            bottomChapterTitle = adjacentChapterTitle,
                            bottomChapterDownloaded = isAdjacentChapterDownloaded,
                            fallbackLabel = stringResource(R.string.reader_transition_no_next),
                            chapterGap = calculateChapterGap(adjacentChapterNumber, currentChapterNumber),
                        )
                    } else {
                        TransitionText(
                            topLabel = stringResource(R.string.reader_transition_previous),
                            topChapterTitle = adjacentChapterTitle,
                            topChapterDownloaded = isAdjacentChapterDownloaded,
                            bottomLabel = stringResource(R.string.reader_transition_current),
                            bottomChapterTitle = currentChapterTitle,
                            bottomChapterDownloaded = isCurrentChapterDownloaded,
                            fallbackLabel = stringResource(R.string.reader_transition_no_previous),
                            chapterGap = calculateChapterGap(currentChapterNumber, adjacentChapterNumber),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitionText(
    topLabel: String,
    topChapterTitle: String?,
    topChapterDownloaded: Boolean,
    bottomLabel: String,
    bottomChapterTitle: String?,
    bottomChapterDownloaded: Boolean,
    fallbackLabel: String,
    chapterGap: Int,
) {
    Column(
        modifier = Modifier
            .widthIn(max = TRANSITION_MAX_WIDTH)
            .fillMaxWidth()
            .padding(TRANSITION_CONTENT_PADDING),
    ) {
        if (topChapterTitle != null) {
            ChapterText(header = topLabel, name = topChapterTitle, downloaded = topChapterDownloaded)
            Spacer(Modifier.height(TRANSITION_SPACER_HEIGHT))
        } else {
            NoChapterNotification(
                text = fallbackLabel,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        if (bottomChapterTitle != null) {
            if (chapterGap > 0) {
                ChapterGapWarning(
                    gapCount = chapterGap,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(TRANSITION_SPACER_HEIGHT))
            }
            ChapterText(header = bottomLabel, name = bottomChapterTitle, downloaded = bottomChapterDownloaded)
        } else {
            NoChapterNotification(
                text = fallbackLabel,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun ChapterText(
    header: String,
    name: String,
    downloaded: Boolean,
) {
    Column {
        Text(
            text = header,
            modifier = Modifier.padding(bottom = 4.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (downloaded) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
            Text(
                text = name,
                fontSize = 20.sp,
                maxLines = CHAPTER_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun NoChapterNotification(
    text: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier, colors = TransitionCardColors) {
        Row(
            modifier = Modifier.padding(
                horizontal = TRANSITION_CARD_HORIZONTAL_PADDING,
                vertical = TRANSITION_CARD_VERTICAL_PADDING,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChapterGapWarning(
    gapCount: Int,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier, colors = TransitionCardColors) {
        Row(
            modifier = Modifier.padding(
                horizontal = TRANSITION_CARD_HORIZONTAL_PADDING,
                vertical = TRANSITION_CARD_VERTICAL_PADDING,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = null,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.reader_transition_missing_chapters,
                    gapCount,
                    gapCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private val TransitionCardColors: CardColors
    @Composable
    get() = CardDefaults.outlinedCardColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

/**
 * Returns the number of missing chapters between [higherChapterNumber] and [lowerChapterNumber].
 * Returns 0 if either value is null, negative, or there is no gap.
 */
private fun calculateChapterGap(higherChapterNumber: Float?, lowerChapterNumber: Float?): Int {
    if (higherChapterNumber == null || lowerChapterNumber == null) return 0
    if (higherChapterNumber < 0f || lowerChapterNumber < 0f) return 0
    return max(
        0,
        floor(higherChapterNumber.toDouble()).toInt() - floor(lowerChapterNumber.toDouble()).toInt() - 1,
    )
}
