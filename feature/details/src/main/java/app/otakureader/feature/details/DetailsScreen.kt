@file:Suppress("MaxLineLength")
package app.otakureader.feature.details

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.otakureader.core.ui.adaptive.isExpanded
import app.otakureader.core.ui.adaptive.rememberWindowWidthSizeClass
import app.otakureader.core.ui.component.ErrorScreen
import app.otakureader.core.ui.component.LoadingScreen
import app.otakureader.core.ui.theme.MangaDynamicTheme
import app.otakureader.core.ui.theme.rememberCoverColorScheme
import app.otakureader.feature.details.R
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview
import app.otakureader.core.ui.theme.OtakuReaderTheme

// Two-pane split for the Expanded width class. The info pane is given slightly
// more horizontal space than the chapter list because the manga header and
// description benefit from extra width; the two weights must sum to 1f.
private const val INFO_PANE_WEIGHT = 0.55f
private const val CHAPTER_PANE_WEIGHT = 0.45f

// Controls how far the user must scroll before the TopAppBar reaches full opacity.
private const val HERO_TOP_BAR_FADE_RANGE = 600f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    mangaId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToReader: (mangaId: Long, chapterId: Long) -> Unit,
    onNavigateToTracking: (mangaId: Long, mangaTitle: String) -> Unit = { _, _ -> },
    onNavigateToGlobalSearch: (query: String) -> Unit = {},
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val isExpanded = rememberWindowWidthSizeClass().isExpanded
    val listState = rememberLazyListState()
    val heroScrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0)
                listState.firstVisibleItemScrollOffset.toFloat()
            else
                Float.MAX_VALUE
        }
    }
    // In expanded (tablet) layout there is no parallax hero — keep the TopAppBar fully opaque.
    val topBarAlpha by remember(isExpanded) {
        derivedStateOf {
            if (isExpanded) 1f
            else (heroScrollOffset / HERO_TOP_BAR_FADE_RANGE).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is DetailsContract.Effect.NavigateToReader -> {
                    onNavigateToReader(effect.mangaId, effect.chapterId)
                }
                is DetailsContract.Effect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is DetailsContract.Effect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is DetailsContract.Effect.ShareManga -> {
                    val shareText = if (effect.url.isNotEmpty()) {
                        "${effect.title}\n${effect.url}"
                    } else {
                        effect.title
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_manga)))
                }
                is DetailsContract.Effect.NavigateToTracking -> {
                    onNavigateToTracking(effect.mangaId, effect.mangaTitle)
                }
                is DetailsContract.Effect.NavigateToGlobalSearch -> {
                    onNavigateToGlobalSearch(effect.query)
                }
                else -> { /* no-op */ }
            }
        }
    }

    val dynamicScheme = rememberCoverColorScheme(
        imageUrl = state.manga?.thumbnailUrl,
        darkTheme = androidx.compose.foundation.isSystemInDarkTheme(),
        // Per-manga override (#947) takes precedence over the global autoThemeColor pref.
        enabled = state.manga?.mangaThemeOverride ?: state.autoThemeEnabled
    )

    MangaDynamicTheme(colorScheme = dynamicScheme) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.manga?.title ?: stringResource(R.string.details_title_fallback),
                        modifier = Modifier.alpha(topBarAlpha),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.details_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = topBarAlpha),
                ),
                actions = {
                    IconButton(onClick = { viewModel.onEvent(DetailsContract.Event.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.details_refresh))
                    }
                    IconButton(onClick = { viewModel.onEvent(DetailsContract.Event.ShareManga) }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.details_share))
                    }
                    IconButton(onClick = { viewModel.onEvent(DetailsContract.Event.OpenTracking) }) {
                        Icon(Icons.Default.QueryStats, contentDescription = stringResource(R.string.details_tracking))
                    }
                    var overflowExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { overflowExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Download all chapters") },
                            onClick = {
                                viewModel.onEvent(DetailsContract.Event.DownloadAllChapters)
                                overflowExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Download unread chapters") },
                            onClick = {
                                viewModel.onEvent(DetailsContract.Event.DownloadUnreadChapters)
                                overflowExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (state.manga?.userCompleted == true) {
                                        stringResource(R.string.details_unmark_completed)
                                    } else {
                                        stringResource(R.string.details_mark_completed)
                                    }
                                )
                            },
                            onClick = {
                                viewModel.onEvent(DetailsContract.Event.ToggleUserCompleted)
                                overflowExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (state.manga?.userDropped == true) {
                                        stringResource(R.string.details_unmark_dropped)
                                    } else {
                                        stringResource(R.string.details_mark_dropped)
                                    }
                                )
                            },
                            onClick = {
                                viewModel.onEvent(DetailsContract.Event.ToggleUserDropped)
                                overflowExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                val label = when (state.manga?.mangaThemeOverride) {
                                    null -> stringResource(R.string.details_theme_inherit)
                                    true -> stringResource(R.string.details_theme_force_on)
                                    false -> stringResource(R.string.details_theme_force_off)
                                }
                                Text(label)
                            },
                            onClick = {
                                viewModel.onEvent(DetailsContract.Event.CycleMangaThemeOverride)
                                overflowExpanded = false
                            },
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.canStartReading) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (state.hasUnreadChapters) {
                            viewModel.onEvent(DetailsContract.Event.ContinueReading)
                        } else {
                            viewModel.onEvent(DetailsContract.Event.StartReading)
                        }
                    },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.details_continue_reading)) },
                    text = {
                        Text(
                            if (state.hasUnreadChapters) {
                                stringResource(R.string.details_continue_reading)
                            } else {
                                stringResource(R.string.details_start_reading)
                            }
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.padding(paddingValues))
            state.error != null -> ErrorScreen(
                message = state.error ?: stringResource(R.string.details_unknown_error),
                onRetry = { viewModel.onEvent(DetailsContract.Event.Refresh) },
                modifier = Modifier.padding(paddingValues)
            )
            state.manga != null -> DetailsContent(
                state = state,
                onEvent = viewModel::onEvent,
                listState = listState,
                modifier = Modifier.padding(paddingValues)
            )
            else -> EmptyScreen(modifier = Modifier.padding(paddingValues))
        }
    }
}
}

@Composable
private fun DetailsContent(
    state: DetailsContract.State,
    onEvent: (DetailsContract.Event) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val manga = state.manga ?: return
    val widthSizeClass = rememberWindowWidthSizeClass()
    val scrollOffset: () -> Float = {
        if (listState.firstVisibleItemIndex == 0)
            listState.firstVisibleItemScrollOffset.toFloat()
        else Float.MAX_VALUE
    }

    if (widthSizeClass.isExpanded) {
        // Tablet / DeX / desktop: split the screen so the chapter list isn't
        // wasted vertical space below a long header. Each pane scrolls
        // independently. We give the info pane slightly more room because
        // the manga header and description benefit from extra width.
        Row(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(INFO_PANE_WEIGHT)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                detailsInfoItems(manga = manga, state = state, onEvent = onEvent)
            }
            VerticalDivider()
            LazyColumn(
                modifier = Modifier
                    .weight(CHAPTER_PANE_WEIGHT)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                detailsChapterItems(state = state, onEvent = onEvent)
            }
        }
    } else {
        // Phone / small tablet: single scrolling column with info above chapters.
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            detailsInfoItems(manga = manga, state = state, onEvent = onEvent, scrollOffset = scrollOffset)
            detailsChapterItems(state = state, onEvent = onEvent)
        }
    }

    if (state.noteEditorVisible) {
        NoteEditorDialog(
            noteText = state.noteEditorText,
            onTextChange = { onEvent(DetailsContract.Event.UpdateNoteText(it)) },
            onSave = { onEvent(DetailsContract.Event.SaveNote) },
            onDismiss = { onEvent(DetailsContract.Event.HideNoteEditor) }
        )
    }

    if (state.chapterNoteEditorChapterId != null) {
        NoteEditorDialog(
            noteText = state.chapterNoteEditorText,
            onTextChange = { onEvent(DetailsContract.Event.UpdateChapterNoteText(it)) },
            onSave = { onEvent(DetailsContract.Event.SaveChapterNote) },
            onDismiss = { onEvent(DetailsContract.Event.HideChapterNoteEditor) },
            titleRes = R.string.chapter_notes_editor_dialog_title
        )
    }

    if (state.showChapterFilter) {
        ChapterFilterDialog(
            filter = state.chapterFilter,
            scanlators = state.chapters.mapNotNull { it.scanlator }.distinct().sorted(),
            onApply = { newFilter -> onEvent(DetailsContract.Event.SetChapterFilter(newFilter)) },
            onDismiss = { onEvent(DetailsContract.Event.HideChapterFilter) }
        )
    }
}

/** Manga header, description, notes, source suggestions, and per-manga options. */
private fun LazyListScope.detailsInfoItems(
    manga: app.otakureader.domain.model.Manga,
    state: DetailsContract.State,
    onEvent: (DetailsContract.Event) -> Unit,
    scrollOffset: () -> Float = { 0f },
) {
    item {
        MangaHeader(
            manga = manga,
            isFavorite = state.isFavorite,
            showPanoramaCover = state.showPanoramaCover,
            onToggleFavorite = { onEvent(DetailsContract.Event.ToggleFavorite) },
            onTogglePanoramaCover = { onEvent(DetailsContract.Event.TogglePanoramaCover) },
            scrollOffset = scrollOffset,
        )
    }

    item {
        MangaDescription(
            description = manga.description,
            expanded = state.descriptionExpanded,
            onToggle = { onEvent(DetailsContract.Event.ToggleDescription) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    item {
        MangaNotes(
            notes = manga.notes,
            onEditClick = { onEvent(DetailsContract.Event.ShowNoteEditor) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    item {
        SourceSuggestionsSection(
            suggestions = state.sourceSuggestions,
            isLoading = state.isLoadingSourceSuggestions,
            error = state.sourceSuggestionsError,
            onSuggestionClick = { suggestion ->
                onEvent(DetailsContract.Event.OnSourceSuggestionClick(suggestion))
            },
            onLoadClick = { onEvent(DetailsContract.Event.LoadSourceSuggestions) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

    item {
        DeleteAfterReadOption(
            override = state.deleteAfterReadOverride,
            globalEnabled = state.globalDeleteAfterRead,
            onChange = { onEvent(DetailsContract.Event.SetDeleteAfterReadOverride(it)) }
        )
    }

    item {
        NotificationOption(
            notifyEnabled = manga.notifyNewChapters,
            onToggle = { onEvent(DetailsContract.Event.ToggleNotifications) }
        )
    }

    item {
        ReaderSettingsSection(
            manga = manga,
            onEvent = onEvent
        )
    }
}

/** Chapter list header followed by the sorted chapter rows. */
private fun LazyListScope.detailsChapterItems(
    state: DetailsContract.State,
    onEvent: (DetailsContract.Event) -> Unit,
) {
    item {
        ChapterListHeader(
            chapterCount = state.chapters.size,
            sortOrder = state.chapterSortOrder,
            isFilterActive = state.chapterFilter.isActive,
            estimatedRemainingTimeMs = state.estimatedRemainingTimeMs,
            onToggleSort = { onEvent(DetailsContract.Event.ToggleSortOrder) },
            onShowFilter = { onEvent(DetailsContract.Event.ShowChapterFilter) }
        )
    }

    items(state.sortedChapters, key = { it.id }) { chapter ->
        ChapterListItem(
            chapter = chapter,
            isSelected = state.selectedChapters.contains(chapter.id),
            onClick = { onEvent(DetailsContract.Event.ChapterClick(chapter.id)) },
            onLongClick = { onEvent(DetailsContract.Event.ChapterLongClick(chapter.id)) },
            onToggleRead = { onEvent(DetailsContract.Event.ToggleChapterRead(chapter.id)) },
            onToggleBookmark = { onEvent(DetailsContract.Event.ToggleChapterBookmark(chapter.id)) },
            onDownload = { onEvent(DetailsContract.Event.DownloadChapter(chapter.id)) },
            onDeleteDownload = { onEvent(DetailsContract.Event.DeleteChapterDownload(chapter.id)) },
            onMarkPreviousRead = { onEvent(DetailsContract.Event.MarkPreviousAsRead(chapter.id)) },
            onExportAsCbz = { onEvent(DetailsContract.Event.ExportChapterAsCbz(chapter.id)) },
            onEditNote = { onEvent(DetailsContract.Event.ShowChapterNoteEditor(chapter.id)) },
            onLoadThumbnail = { onEvent(DetailsContract.Event.LoadChapterThumbnail(chapter.id)) },
        )
    }
}

@Composable
private fun MangaNotes(
    notes: String?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.notes_section_title),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.notes_edit_content_description)
                )
            }
        }

        if (!notes.isNullOrBlank()) {
            Text(
                text = renderMarkdown(notes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.notes_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoteEditorDialog(
    noteText: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    @StringRes titleRes: Int = R.string.notes_editor_dialog_title
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.notes_editor_placeholder)) },
                minLines = 5,
                maxLines = 12
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.notes_editor_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.notes_editor_cancel)) }
        }
    )
}

@Composable
private fun MangaDescription(
    description: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (description.isNullOrBlank()) return

    Column(modifier = modifier) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )

        if (description.length > 100) {
            TextButton(onClick = onToggle) {
                Text(if (expanded) stringResource(R.string.details_show_less) else stringResource(R.string.details_show_more))
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF12121A)
@Composable
private fun MangaDescriptionPreview() {
    OtakuReaderTheme {
        MangaDescription(
            description = "A young man is reincarnated into another world as an overpowered mage. " +
                "He must navigate a dangerous world of monsters and politics while hiding his true power.",
            expanded = false,
            onToggle = {}
        )
    }
}
