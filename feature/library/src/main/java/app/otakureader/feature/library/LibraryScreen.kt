package app.otakureader.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onMangaClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToMigration: (List<Long>) -> Unit = {},
    onNavigateToCategoryManagement: () -> Unit = {},
    onNavigateToReader: (mangaId: Long, chapterId: Long) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    
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
            }
        }
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
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.RemoveSelectedFromLibrary) }) {
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
                                text = { Text(stringResource(R.string.library_mark_selected_completed)) },
                                onClick = {
                                    viewModel.onEvent(LibraryEvent.MarkSelectedAsCompleted)
                                    selectionOverflowExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_mark_selected_dropped)) },
                                onClick = {
                                    viewModel.onEvent(LibraryEvent.MarkSelectedAsDropped)
                                    selectionOverflowExpanded = false
                                },
                            )
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
                    title = { Text(stringResource(R.string.library_title)) },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleIncognito) }) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = "Toggle incognito mode",
                                tint = if (state.incognitoMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleSearchBar) }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.library_search))
                        }
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleFilterSheet) }) {
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
            modifier = Modifier.padding(padding)
        )
    }

    if (state.showFilterSheet) {
        LibraryFilterSheet(
            state = state,
            onEvent = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(LibraryEvent.ToggleFilterSheet) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LibraryContent(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
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

        if (state.showAdvancedSearch) {
            AdvancedSearchSheet(
                onApply = { author, tag -> onEvent(LibraryEvent.ApplyAdvancedSearch(author, tag)) },
                onDismiss = { onEvent(LibraryEvent.ToggleAdvancedSearch) },
            )
        }

        val hasActiveFilters = state.filterMode != LibraryFilterMode.ALL || state.filterGenres.isNotEmpty()
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
            }
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
                        EmptyLibraryMessage(modifier = Modifier.align(Alignment.Center))
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
    val statusFilters = listOf(
        LibraryFilterMode.ALL to stringResource(R.string.library_filter_all),
        LibraryFilterMode.UNREAD to stringResource(R.string.library_filter_unread),
        LibraryFilterMode.DOWNLOADED to stringResource(R.string.library_filter_downloaded),
        LibraryFilterMode.COMPLETED to stringResource(R.string.library_filter_completed),
        LibraryFilterMode.DROPPED to stringResource(R.string.library_filter_dropped),
    )

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
