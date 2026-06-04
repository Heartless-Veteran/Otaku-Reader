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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LibraryBottomSheet(
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
                .padding(bottom = 32.dp),
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = state.bottomSheetTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LibraryBottomSheetTab.entries.forEach { tab ->
                    Tab(
                        selected = state.bottomSheetTab == tab,
                        onClick = { onEvent(LibraryEvent.SetBottomSheetTab(tab)) },
                        text = { Text(tab.label()) },
                    )
                }
            }

            // Tab content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state.bottomSheetTab) {
                    LibraryBottomSheetTab.DISPLAY -> DisplayTab(state, onEvent)
                    LibraryBottomSheetTab.SORT -> SortTab(state, onEvent)
                    LibraryBottomSheetTab.FILTER -> FilterTab(state, onEvent)
                    LibraryBottomSheetTab.GROUP -> GroupTab(state, onEvent)
                }
            }
        }
    }
}

@Composable
private fun DisplayTab(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Grid size
        Column {
            Text(
                text = stringResource(R.string.display_grid_size_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("1", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = state.gridSize.toFloat(),
                    onValueChange = { onEvent(LibraryEvent.SetGridSize(it.toInt())) },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.weight(1f),
                )
                Text("5", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = stringResource(R.string.display_grid_size_value, state.gridSize),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        HorizontalDivider()

        // Show badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.display_show_badges))
            Switch(
                checked = state.showBadges,
                onCheckedChange = { onEvent(LibraryEvent.ToggleShowBadges) },
            )
        }

        // Show download badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.display_show_download_badge))
            Switch(
                checked = state.showDownloadBadge,
                onCheckedChange = { onEvent(LibraryEvent.ToggleShowDownloadBadge) },
            )
        }

        // Staggered grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.display_staggered_grid))
            Switch(
                checked = state.isStaggeredGrid,
                onCheckedChange = { onEvent(LibraryEvent.ToggleStaggeredGrid) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortTab(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterTab(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header row with clear all
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.filter_sheet_status_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = { onEvent(LibraryEvent.ClearAllFilters) }) {
                Text(stringResource(R.string.filter_sheet_clear_all))
            }
        }

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupTab(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.group_category_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.selectedCategory == null,
                onClick = { onEvent(LibraryEvent.OnCategorySelected(null)) },
                label = { Text(stringResource(R.string.library_category_all)) },
            )
            state.categories.forEach { category ->
                FilterChip(
                    selected = state.selectedCategory == category.id,
                    onClick = { onEvent(LibraryEvent.OnCategorySelected(category.id)) },
                    label = { Text("${category.name} (${category.count})") },
                )
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.group_by_category))
            Switch(
                checked = state.groupByCategory,
                onCheckedChange = { onEvent(LibraryEvent.SetGroupByCategory(it)) },
            )
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

@Composable
private fun LibraryBottomSheetTab.label(): String = when (this) {
    LibraryBottomSheetTab.DISPLAY -> stringResource(R.string.display_tab)
    LibraryBottomSheetTab.SORT -> stringResource(R.string.sort_tab)
    LibraryBottomSheetTab.FILTER -> stringResource(R.string.filter_tab)
    LibraryBottomSheetTab.GROUP -> stringResource(R.string.group_tab)
}
