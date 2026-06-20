package app.otakureader.feature.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.otakureader.domain.model.Chapter
import app.otakureader.feature.reader.R

/**
 * Right-side sliding panel showing all chapters for the current manga.
 *
 * Slides in from the right edge using [AnimatedVisibility] + [slideInHorizontally].
 * Tapping a chapter triggers [onChapterClick]; tapping outside (the scrim) dismisses it.
 * The list automatically scrolls to the current chapter on first open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListOverlay(
    isVisible: Boolean,
    chapters: List<Chapter>,
    currentChapterId: Long,
    onChapterClick: (chapterId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim — tapping outside the panel dismisses it
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
            )

            // Chapter list panel — right-aligned, ~75% screen width
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = 0.75f)
                    .align(Alignment.CenterEnd),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column {
                    TopAppBar(
                        title = { Text(stringResource(R.string.reader_chapter_list_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.reader_back),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )

                    HorizontalDivider()

                    val listState = rememberLazyListState()
                    val currentIndex = chapters.indexOfFirst { it.id == currentChapterId }

                    LaunchedEffect(isVisible, chapters.size) {
                        if (isVisible && currentIndex >= 0) {
                            listState.scrollToItem(currentIndex)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(chapters, key = { _, c -> c.id }) { _, chapter ->
                            val isCurrent = chapter.id == currentChapterId
                            ChapterListItem(
                                chapter = chapter,
                                isCurrent = isCurrent,
                                onClick = { onChapterClick(chapter.id) },
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterListItem(
    chapter: Chapter,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Unspecified
    }

    ListItem(
        headlineContent = {
            Text(
                text = chapter.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (isCurrent) {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = if (chapter.read) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                },
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chapter.read && !isCurrent) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.reader_chapter_read),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
