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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
                            Icon(Icons.Default.RadioButtonUnchecked, contentDescription = stringResource(R.string.library_mark_selected_unread))
                        }
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.DownloadSelected) }) {
                            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.library_download_selected))
                        }
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.RemoveSelectedFromLibrary) }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.library_remove_selected))
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
}

@OptIn(ExperimentalMaterial3Api::class)
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

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(LibraryEvent.Refresh) },
            modifier = Modifier.weight(1f)
        ) {
            when {
                state.isLoading && state.mangaList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
