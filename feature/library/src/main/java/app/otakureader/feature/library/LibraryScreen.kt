package app.otakureader.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.otakureader.core.ui.components.IncognitoBanner
import app.otakureader.core.ui.adaptive.WindowWidthSizeClass
import app.otakureader.core.ui.adaptive.isExpanded
import app.otakureader.core.ui.adaptive.rememberWindowWidthSizeClass
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import app.otakureader.domain.model.SavedLibraryView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onMangaClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToMigration: (List<Long>) -> Unit = {},
    onNavigateToCategoryManagement: () -> Unit = {},
    onNavigateToReader: (mangaId: Long, chapterId: Long) -> Unit = { _, _ -> },
    onNavigateToMergeDuplicates: () -> Unit = {},
    onNavigateToShareLibrary: () -> Unit = {},
    onNavigateToScanLibrary: () -> Unit = {},
    onNavigateToMaintenance: () -> Unit = {},
    onBrowseClick: (() -> Unit)? = null,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var pendingBulkAction: LibraryEvent? by remember { mutableStateOf(null) }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is LibraryEffect.NavigateToManga -> onMangaClick(effect.mangaId)
                is LibraryEffect.NavigateToReader -> onNavigateToReader(effect.mangaId, effect.chapterId)
                is LibraryEffect.ShowError -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is LibraryEffect.NavigateToMigration -> {
                    onNavigateToMigration(effect.selectedMangaIds)
                }
                is LibraryEffect.ShowSnackbar -> scope.launch {
                    val msg = if (effect.formatArgs.isEmpty()) {
                        context.getString(effect.messageRes)
                    } else {
                        context.getString(effect.messageRes, *effect.formatArgs.toTypedArray())
                    }
                    snackbarHostState.showSnackbar(msg)
                }
                is LibraryEffect.ShareManga -> {
                    val shareText = if (effect.url.isNotEmpty()) "${effect.title}\n${effect.url}" else effect.title
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    runCatching {
                        context.startActivity(
                            android.content.Intent.createChooser(shareIntent, effect.title)
                        )
                    }.onFailure {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.library_share_error)) }
                    }
                }
            }
        }
    }

    pendingBulkAction?.let { action ->
        val actionLabel = when (action) {
            is LibraryEvent.RemoveSelectedFromLibrary -> stringResource(R.string.library_remove_selected)
            is LibraryEvent.MarkSelectedAsCompleted -> stringResource(R.string.library_mark_selected_completed)
            is LibraryEvent.MarkSelectedAsDropped -> stringResource(R.string.library_mark_selected_dropped)
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { pendingBulkAction = null },
            title = { Text(stringResource(R.string.library_bulk_action_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.library_bulk_action_confirm_message,
                        state.selectedManga.size,
                        actionLabel,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(action)
                    pendingBulkAction = null
                }) {
                    Text(stringResource(R.string.library_bulk_action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBulkAction = null }) {
                    Text(stringResource(R.string.library_bulk_action_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            when {
                state.selectedManga.isNotEmpty() -> TopAppBar(
                    title = {
                        Text(stringResource(R.string.library_selected_count, state.selectedManga.size))
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.library_deselect_all))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.MarkSelectedAsRead) }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.library_mark_selected_read))
                        }
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.MarkSelectedAsUnread) }) {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = stringResource(R.string.library_mark_selected_unread)
                            )
                        }
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.DownloadSelected) }) {
                            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.library_download_selected))
                        }
                        IconButton(onClick = { pendingBulkAction = LibraryEvent.RemoveSelectedFromLibrary }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.library_remove_selected))
                        }
                        var selectionOverflowExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { selectionOverflowExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.library_more_actions),
                            )
                        }
                        DropdownMenu(
                            expanded = selectionOverflowExpanded,
                            onDismissRequest = { selectionOverflowExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_select_all)) },
                                onClick = {
                                    viewModel.onEvent(LibraryEvent.SelectAllManga)
                                    selectionOverflowExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_invert_selection)) },
                                onClick = {
                                    viewModel.onEvent(LibraryEvent.InvertSelection)
                                    selectionOverflowExpanded = false
                                },
                            )
                            androidx.compose.material3.HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_mark_selected_completed)) },
                                onClick = {
                                    pendingBulkAction = LibraryEvent.MarkSelectedAsCompleted
                                    selectionOverflowExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_mark_selected_dropped)) },
                                onClick = {
                                    pendingBulkAction = LibraryEvent.MarkSelectedAsDropped
                                    selectionOverflowExpanded = false
                                },
                            )
                            if (state.selectedManga.size == 1) {
                                androidx.compose.material3.HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_share_manga)) },
                                    onClick = {
                                        viewModel.onEvent(LibraryEvent.ShareSelectedManga)
                                        selectionOverflowExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_view_details)) },
                                    onClick = {
                                        viewModel.onEvent(LibraryEvent.ViewSelectedManga)
                                        selectionOverflowExpanded = false
                                    },
                                )
                            }
                        }
                    }
                )
                state.showSearchBar -> TopAppBar(
                    title = {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onEvent(LibraryEvent.OnSearchQueryChange(it)) },
                            placeholder = { Text(stringResource(R.string.library_search_placeholder)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {}),
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onEvent(LibraryEvent.OnSearchQueryChange("")) }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.library_search_clear)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleSearchBar) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.library_search_close))
                        }
                    }
                )
                else -> TopAppBar(
                    title = { Text(stringResource(R.string.library_title_with_count, state.mangaList.size)) },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleIncognito) }) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = stringResource(R.string.library_toggle_incognito),
                                tint = if (state.incognitoMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        val inListMode = state.displayMode == LibraryDisplayMode.LIST
                        IconButton(onClick = {
                            viewModel.onEvent(
                                LibraryEvent.SetDisplayMode(
                                    if (inListMode) LibraryDisplayMode.GRID else LibraryDisplayMode.LIST
                                )
                            )
                        }) {
                            Icon(
                                imageVector = if (inListMode) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = if (inListMode)
                                    stringResource(R.string.library_view_mode_grid)
                                else
                                    stringResource(R.string.library_view_mode_list),
                            )
                        }
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleSearchBar) }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.library_search))
                        }
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleBottomSheet) }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.filter_sheet_title),
                                tint = if (state.filterGenres.isNotEmpty() || state.filterMode != LibraryFilterMode.ALL)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.library_more))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_update_library)) },
                                onClick = { showMenu = false; viewModel.onEvent(LibraryEvent.UpdateLibrary) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_sync_library)) },
                                onClick = { showMenu = false; viewModel.onEvent(LibraryEvent.SyncLibrary) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_update_category)) },
                                onClick = { showMenu = false; viewModel.onEvent(LibraryEvent.UpdateCategory) },
                                enabled = state.selectedCategory != null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_open_random)) },
                                onClick = { showMenu = false; viewModel.onEvent(LibraryEvent.OpenRandomEntry) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_reindex_downloads)) },
                                onClick = { showMenu = false; viewModel.onEvent(LibraryEvent.ReindexDownloads) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_menu_maintenance)) },
                                onClick = { showMenu = false; onNavigateToMaintenance() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_downloads)) },
                                onClick = { showMenu = false; onNavigateToDownloads() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_settings)) },
                                onClick = { showMenu = false; onNavigateToSettings() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_filter_has_notes)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(LibraryEvent.FilterHasNotes(!state.filterHasNotes))
                                },
                                trailingIcon = {
                                    Checkbox(
                                        checked = state.filterHasNotes,
                                        onCheckedChange = { checked ->
                                            showMenu = false
                                            viewModel.onEvent(LibraryEvent.FilterHasNotes(checked))
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_manage_categories)) },
                                onClick = { showMenu = false; onNavigateToCategoryManagement() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.merge_duplicates_menu_item)) },
                                onClick = { showMenu = false; onNavigateToMergeDuplicates() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_share_via_qr)) },
                                onClick = { showMenu = false; onNavigateToShareLibrary() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_scan_qr)) },
                                onClick = { showMenu = false; onNavigateToScanLibrary() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_save_view)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(LibraryEvent.ShowSaveViewDialog)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_sync_eh_favorites)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(LibraryEvent.SyncEhFavorites)
                                }
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LibraryContent(
            state = state,
            onEvent = viewModel::onEvent,
            onBrowseClick = onBrowseClick,
            modifier = Modifier.padding(padding)
        )
    }

    if (state.showBottomSheet) {
        LibraryBottomSheet(
            state = state,
            onEvent = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(LibraryEvent.ToggleBottomSheet) },
        )
    }

    if (state.showSaveViewDialog) {
        SaveViewDialog(
            name = state.saveViewName,
            onNameChange = { viewModel.onEvent(LibraryEvent.UpdateSaveViewName(it)) },
            onConfirm = { viewModel.onEvent(LibraryEvent.ConfirmSaveView) },
            onDismiss = { viewModel.onEvent(LibraryEvent.HideSaveViewDialog) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LibraryContent(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
    onBrowseClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val widthSizeClass = rememberWindowWidthSizeClass()
    val isExpandedWidth = widthSizeClass.isExpanded
    val adaptiveColumns = when (widthSizeClass) {
        WindowWidthSizeClass.Expanded -> (state.gridSize + 2).coerceAtLeast(5)
        WindowWidthSizeClass.Medium -> (state.gridSize + 1).coerceAtLeast(4)
        WindowWidthSizeClass.Compact -> state.gridSize
    }
    var detailManga by remember { mutableStateOf<LibraryMangaItem?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.incognitoMode) {
            IncognitoBanner()
        }

        if (state.showSearchBar) {
            LibrarySearchFiltersRow(state = state, onEvent = onEvent)
        }

        if (state.showSearchBar && state.showAdvancedSearch) {
            AdvancedSearchSheet(
                onApply = { author, tag -> onEvent(LibraryEvent.ApplyAdvancedSearch(author, tag)) },
                onDismiss = { onEvent(LibraryEvent.ToggleAdvancedSearch) },
            )
        }

        val hasActiveFilters = state.filterMode != LibraryFilterMode.ALL ||
            state.filterGenres.isNotEmpty() ||
            state.sortMode != LibrarySortMode.ALPHABETICAL
        if (hasActiveFilters && !state.showSearchBar) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.filterMode != LibraryFilterMode.ALL) {
                    FilterChip(
                        selected = true,
                        onClick = { onEvent(LibraryEvent.SetFilterMode(LibraryFilterMode.ALL)) },
                        label = { Text(state.filterMode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                    )
                }
                state.filterGenres.forEach { genre ->
                    FilterChip(
                        selected = true,
                        onClick = { onEvent(LibraryEvent.SetGenreFilter(state.filterGenres - genre)) },
                        label = { Text(genre) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                    )
                }
                if (state.sortMode != LibrarySortMode.ALPHABETICAL) {
                    FilterChip(
                        selected = true,
                        onClick = { onEvent(LibraryEvent.SetSortMode(LibrarySortMode.ALPHABETICAL)) },
                        label = {
                            Text(
                                stringResource(
                                    R.string.library_sort_chip,
                                    state.sortMode.name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' ')
                                )
                            )
                        },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                    )
                }
            }
        }

        // Saved views row: shown when there are saved views and search bar is closed.
        if (state.savedViews.isNotEmpty() && !state.showSearchBar) {
            SavedViewsRow(
                savedViews = state.savedViews,
                onApply = { onEvent(LibraryEvent.ApplySavedView(it)) },
                onDelete = { onEvent(LibraryEvent.DeleteSavedView(it)) },
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(LibraryEvent.Refresh) },
            modifier = Modifier.weight(1f)
        ) {
            when {
                (state.isLoading || state.isSearching) && state.mangaList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                state.mangaList.isEmpty() && state.searchQuery.isNotBlank() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EmptyLibrarySearchMessage(
                            query = state.searchQuery,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                state.mangaList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EmptyLibraryMessage(
                            onBrowseClick = onBrowseClick,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                isExpandedWidth -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        MangaGrid(
                            state = state,
                            onEvent = onEvent,
                            adaptiveColumns = adaptiveColumns,
                            onMangaSelect = { manga -> detailManga = manga },
                            modifier = Modifier.weight(0.55f)
                        )
                        VerticalDivider()
                        MangaDetailPanel(
                            manga = detailManga,
                            onOpenFullDetails = { onEvent(LibraryEvent.OnMangaClick(it)) },
                            onClose = { detailManga = null },
                            modifier = Modifier.weight(0.45f)
                        )
                    }
                }
                else -> {
                    MangaGrid(
                        state = state,
                        onEvent = onEvent,
                        adaptiveColumns = adaptiveColumns
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySearchFiltersRow(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
) {
    val allLabel = stringResource(R.string.library_filter_all)
    val unreadLabel = stringResource(R.string.library_filter_unread)
    val downloadedLabel = stringResource(R.string.library_filter_downloaded)
    val completedLabel = stringResource(R.string.library_filter_completed)
    val droppedLabel = stringResource(R.string.library_filter_dropped)
    val statusFilters = remember(allLabel, unreadLabel, downloadedLabel, completedLabel, droppedLabel) {
        listOf(
            LibraryFilterMode.ALL to allLabel,
            LibraryFilterMode.UNREAD to unreadLabel,
            LibraryFilterMode.DOWNLOADED to downloadedLabel,
            LibraryFilterMode.COMPLETED to completedLabel,
            LibraryFilterMode.DROPPED to droppedLabel,
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(statusFilters, key = { it.first.name }) { (mode, label) ->
            FilterChip(
                selected = state.filterMode == mode,
                onClick = { onEvent(LibraryEvent.SetFilterMode(mode)) },
                label = { Text(label) },
            )
        }
        if (state.availableGenres.isNotEmpty()) {
            items(state.availableGenres.take(12), key = { "genre_$it" }) { genre ->
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
        item(key = "advanced") {
            AssistChip(
                onClick = { onEvent(LibraryEvent.ToggleAdvancedSearch) },
                label = { Text(stringResource(R.string.library_advanced_search_open)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

// ─── Saved views UI ────────────────────────────────────────────────────────────

/**
 * A horizontally scrollable row of [FilterChip]s, one per saved view.
 *
 * - Tap to apply a view (restores filter+sort).
 * - Long-press to delete a saved view.
 *
 * The row is only shown when [savedViews] is non-empty (caller's responsibility).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedViewsRow(
    savedViews: List<SavedLibraryView>,
    onApply: (SavedLibraryView) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.library_saved_views),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(savedViews, key = { it.id }) { view ->
                FilterChip(
                    selected = false,
                    onClick = { onApply(view) },
                    label = { Text(view.name) },
                    modifier = Modifier.combinedClickable(
                        onClick = { onApply(view) },
                        onLongClick = { onDelete(view.id) },
                    ),
                )
            }
        }
    }
}

/**
 * AlertDialog that lets the user type a name for the current filter+sort combination
 * before saving it.
 *
 * @param name Current value of the name text field (from MVI state).
 * @param onNameChange Called on every keystroke.
 * @param onConfirm Called when the user taps Save; the ViewModel snapshots the current state.
 * @param onDismiss Called when the user taps Cancel or dismisses the dialog.
 */
@Composable
private fun SaveViewDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_save_view)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text(stringResource(R.string.library_save_view_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.library_save_view_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
