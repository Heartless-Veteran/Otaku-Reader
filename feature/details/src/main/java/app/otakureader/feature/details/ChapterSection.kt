@file:Suppress("MaxLineLength")
package app.otakureader.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.otakureader.feature.details.R

@Composable
internal fun ChapterListHeader(
    chapterCount: Int,
    sortOrder: DetailsContract.ChapterSortOrder,
    isFilterActive: Boolean = false,
    estimatedRemainingTimeMs: Long = 0L,
    onToggleSort: () -> Unit,
    onShowFilter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pluralStringResource(R.plurals.details_chapter_count, chapterCount, chapterCount),
                style = MaterialTheme.typography.titleMedium
            )
            if (estimatedRemainingTimeMs > 0L) {
                val minutes = (estimatedRemainingTimeMs / 60_000L).toInt()
                Text(
                    text = stringResource(
                        R.string.details_remaining_read_time,
                        app.otakureader.core.common.formatReadTime(minutes),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShowFilter) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.details_filter_chapters),
                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onToggleSort) {
                Text(
                    when (sortOrder) {
                        DetailsContract.ChapterSortOrder.ASCENDING -> stringResource(R.string.details_sort_ascending)
                        DetailsContract.ChapterSortOrder.DESCENDING -> stringResource(R.string.details_sort_descending)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChapterFilterDialog(
    filter: DetailsContract.ChapterFilter,
    scanlators: List<String>,
    onApply: (DetailsContract.ChapterFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var read by remember { mutableStateOf(filter.read) }
    var bookmarked by remember { mutableStateOf(filter.bookmarked) }
    var downloaded by remember { mutableStateOf(filter.downloaded) }
    var selectedScanlator by remember { mutableStateOf(filter.scanlator) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_filter_chapters)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Read / Unread filter
                Text(
                    text = stringResource(R.string.details_filter_read_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TriStateRow(
                    labelAll = stringResource(R.string.details_filter_all),
                    labelOnly = stringResource(R.string.details_filter_read),
                    labelExclude = stringResource(R.string.details_filter_unread),
                    state = read,
                    onStateChange = { read = it }
                )

                HorizontalDivider()

                // Bookmark filter
                Text(
                    text = stringResource(R.string.details_filter_bookmarked_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TriStateRow(
                    labelAll = stringResource(R.string.details_filter_all),
                    labelOnly = stringResource(R.string.details_filter_bookmarked),
                    labelExclude = stringResource(R.string.details_filter_not_bookmarked),
                    state = bookmarked,
                    onStateChange = { bookmarked = it }
                )

                HorizontalDivider()

                // Downloaded filter
                Text(
                    text = stringResource(R.string.details_filter_downloaded_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TriStateRow(
                    labelAll = stringResource(R.string.details_filter_all),
                    labelOnly = stringResource(R.string.details_filter_downloaded),
                    labelExclude = stringResource(R.string.details_filter_not_downloaded),
                    state = downloaded,
                    onStateChange = { downloaded = it }
                )

                // Scanlator filter (only shown when multiple scanlators exist)
                if (scanlators.size > 1) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.details_filter_scanlator_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedScanlator == null,
                            onClick = { selectedScanlator = null },
                            label = { Text(stringResource(R.string.details_filter_all)) }
                        )
                        scanlators.forEach { s ->
                            FilterChip(
                                selected = selectedScanlator == s,
                                onClick = { selectedScanlator = if (selectedScanlator == s) null else s },
                                label = { Text(s) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(DetailsContract.ChapterFilter(read, bookmarked, downloaded, selectedScanlator))
            }) {
                Text(stringResource(R.string.details_filter_apply))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onApply(DetailsContract.ChapterFilter())
                }) {
                    Text(stringResource(R.string.details_filter_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.details_filter_cancel))
                }
            }
        }
    )
}

@Composable
internal fun TriStateRow(
    labelAll: String,
    labelOnly: String,
    labelExclude: String,
    state: DetailsContract.TriState,
    onStateChange: (DetailsContract.TriState) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state == DetailsContract.TriState.ALL,
            onClick = { onStateChange(DetailsContract.TriState.ALL) },
            label = { Text(labelAll) }
        )
        FilterChip(
            selected = state == DetailsContract.TriState.ONLY,
            onClick = { onStateChange(DetailsContract.TriState.ONLY) },
            label = { Text(labelOnly) }
        )
        FilterChip(
            selected = state == DetailsContract.TriState.EXCLUDE,
            onClick = { onStateChange(DetailsContract.TriState.EXCLUDE) },
            label = { Text(labelExclude) }
        )
    }
}

@Composable
internal fun EmptyScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.details_no_manga))
    }
}
