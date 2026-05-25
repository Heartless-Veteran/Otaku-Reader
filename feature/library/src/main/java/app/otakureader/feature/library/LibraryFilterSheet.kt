package app.otakureader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.otakureader.feature.library.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LibraryFilterSheet(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.filter_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { onEvent(LibraryEvent.ClearAllFilters) }) {
                    Text(stringResource(R.string.filter_sheet_clear_all))
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // Sort section
            Text(
                text = stringResource(R.string.filter_sheet_sort_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    LibrarySortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.sortMode == mode,
                            onClick = { onEvent(LibraryEvent.SetSortMode(mode)) },
                            label = { Text(mode.label()) },
                        )
                    }
                }
                IconToggleButton(
                    checked = state.sortAscending,
                    onCheckedChange = { onEvent(LibraryEvent.SetSortAscending(it)) },
                ) {
                    Icon(
                        imageVector = if (state.sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (state.sortAscending)
                            stringResource(R.string.filter_sort_ascending)
                        else
                            stringResource(R.string.filter_sort_descending),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // Status filter section
            Text(
                text = stringResource(R.string.filter_sheet_status_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryFilterMode.entries.forEach { mode ->
                    if (mode != LibraryFilterMode.READING_LIST) {
                        FilterChip(
                            selected = state.filterMode == mode,
                            onClick = {
                                onEvent(
                                    LibraryEvent.SetFilterMode(
                                        if (state.filterMode == mode) LibraryFilterMode.ALL else mode
                                    )
                                )
                            },
                            label = { Text(mode.label()) },
                        )
                    }
                }
            }

            if (state.availableGenres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))

                // Genre filter section
                Text(
                    text = stringResource(R.string.filter_sheet_genre_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.availableGenres.forEach { genre ->
                        FilterChip(
                            selected = genre in state.filterGenres,
                            onClick = {
                                val updated = if (genre in state.filterGenres) {
                                    state.filterGenres - genre
                                } else {
                                    state.filterGenres + genre
                                }
                                onEvent(LibraryEvent.SetGenreFilter(updated))
                            },
                            label = { Text(genre) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySortMode.label(): String = when (this) {
    LibrarySortMode.ALPHABETICAL -> stringResource(R.string.sort_title)
    LibrarySortMode.LAST_READ -> stringResource(R.string.sort_last_read)
    LibrarySortMode.DATE_ADDED -> stringResource(R.string.sort_date_added)
    LibrarySortMode.UNREAD_COUNT -> stringResource(R.string.sort_unread_count)
    LibrarySortMode.SOURCE -> stringResource(R.string.sort_source)
}

@Composable
private fun LibraryFilterMode.label(): String = when (this) {
    LibraryFilterMode.ALL -> stringResource(R.string.filter_all)
    LibraryFilterMode.DOWNLOADED -> stringResource(R.string.filter_downloaded)
    LibraryFilterMode.UNREAD -> stringResource(R.string.filter_unread)
    LibraryFilterMode.COMPLETED -> stringResource(R.string.filter_completed)
    LibraryFilterMode.DROPPED -> stringResource(R.string.filter_dropped)
    LibraryFilterMode.TRACKING -> stringResource(R.string.filter_tracking)
    LibraryFilterMode.READING_LIST -> stringResource(R.string.filter_reading_list)
}
