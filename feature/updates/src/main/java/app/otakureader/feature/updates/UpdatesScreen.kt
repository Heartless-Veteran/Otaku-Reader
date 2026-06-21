package app.otakureader.feature.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.otakureader.domain.model.DownloadItem
import app.otakureader.domain.model.DownloadStatus
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaUpdate
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val UNSET_FETCH_DATE = 0L

/** Updates screen showing newly discovered chapters for library manga. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    onMangaClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpdatesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is UpdatesEffect.NavigateToReader -> onMangaClick(effect.mangaId)
                is UpdatesEffect.ShowSnackbar -> scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
                // Launch independently so collectLatest cancelling on the next swipe
                // does not cut this snackbar short; each mark-as-read is independently undoable.
                is UpdatesEffect.ShowUndoSnackbar -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = context.getString(R.string.updates_undo_action),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(UpdatesEvent.UnmarkChapterAsRead(effect.chapterId))
                    }
                }
                is UpdatesEffect.ShowUndoBulkReadSnackbar -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = context.getString(R.string.updates_undo_action),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(UpdatesEvent.UnmarkSelectedAsRead(effect.chapterIds))
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.selectedItems.isNotEmpty()) {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.updates_selected_count, state.selectedItems.size))
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(UpdatesEvent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.updates_clear_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(UpdatesEvent.SelectAll) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.updates_select_all))
                        }
                        IconButton(onClick = { viewModel.onEvent(UpdatesEvent.InvertSelection) }) {
                            Icon(Icons.Default.FlipToBack, contentDescription = stringResource(R.string.updates_invert_selection))
                        }
                        IconButton(onClick = { viewModel.onEvent(UpdatesEvent.DownloadSelected) }) {
                            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.updates_download_selected))
                        }
                        IconButton(onClick = { viewModel.onEvent(UpdatesEvent.MarkSelectedAsRead) }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.updates_mark_selected_read))
                        }
                        IconButton(onClick = { viewModel.onEvent(UpdatesEvent.MarkSelectedAsUnread) }) {
                            Icon(Icons.Default.RemoveDone, contentDescription = stringResource(R.string.updates_mark_selected_unread))
                        }
                    }
                )
            } else {
                var overflowMenuExpanded by remember { mutableStateOf(false) }
                TopAppBar(
                    title = { Text(stringResource(R.string.updates_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.updates_back))
                        }
                    },
                    actions = {
                        // Trigger a manual library update right from the Updates screen.
                        IconButton(onClick = { viewModel.onEvent(UpdatesEvent.StartLibraryUpdate) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.updates_refresh_now)
                            )
                        }
                        // To-Be-Updated preview icon
                        IconButton(onClick = { viewModel.onEvent(UpdatesEvent.ShowPendingUpdates) }) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = stringResource(R.string.updates_view_pending)
                            )
                        }
                        // Update errors icon with badge
                        if (state.updateErrors.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onEvent(UpdatesEvent.ShowUpdateErrors) }) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text("${state.updateErrors.size}") }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = stringResource(R.string.updates_view_errors)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onNavigateToDownloads) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(R.string.updates_downloads)
                            )
                        }
                        // Overflow menu — display mode toggle
                        IconButton(onClick = { overflowMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.updates_more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = { overflowMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (state.displayMode == UpdatesDisplayMode.GROUPED_BY_MANGA) {
                                            stringResource(R.string.updates_group_by_date)
                                        } else {
                                            stringResource(R.string.updates_group_by_manga)
                                        }
                                    )
                                },
                                onClick = {
                                    viewModel.onEvent(UpdatesEvent.ToggleDisplayMode)
                                    overflowMenuExpanded = false
                                }
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.selectedItems.isEmpty()) {
                val now = System.currentTimeMillis()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = state.dateFilterStart == now - 7L * 24 * 60 * 60 * 1000 &&
                            state.dateFilterEnd == null,
                        onClick = {
                            if (state.dateFilterStart == now - 7L * 24 * 60 * 60 * 1000 &&
                                state.dateFilterEnd == null
                            ) {
                                viewModel.onEvent(UpdatesEvent.ClearDateFilter)
                            } else {
                                viewModel.onEvent(
                                    UpdatesEvent.SetDateFilter(now - 7L * 24 * 60 * 60 * 1000, null)
                                )
                            }
                        },
                        label = { Text(stringResource(R.string.updates_filter_last_7_days)) },
                    )
                    FilterChip(
                        selected = state.dateFilterStart == now - 30L * 24 * 60 * 60 * 1000 &&
                            state.dateFilterEnd == null,
                        onClick = {
                            if (state.dateFilterStart == now - 30L * 24 * 60 * 60 * 1000 &&
                                state.dateFilterEnd == null
                            ) {
                                viewModel.onEvent(UpdatesEvent.ClearDateFilter)
                            } else {
                                viewModel.onEvent(
                                    UpdatesEvent.SetDateFilter(now - 30L * 24 * 60 * 60 * 1000, null)
                                )
                            }
                        },
                        label = { Text(stringResource(R.string.updates_filter_last_30_days)) },
                    )
                }
            }

            val filteredUpdates = remember(state.updates, state.dateFilterStart, state.dateFilterEnd) {
                var result = state.updates
                state.dateFilterStart?.let { start -> result = result.filter { it.chapter.dateFetch >= start } }
                state.dateFilterEnd?.let { end -> result = result.filter { it.chapter.dateFetch <= end } }
                result
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.onEvent(UpdatesEvent.StartLibraryUpdate) },
                modifier = Modifier.weight(1f),
            ) {
                when {
                    state.isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    state.error != null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error ?: stringResource(R.string.updates_unknown_error),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    filteredUpdates.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.updates_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        val onChapterClick: (MangaUpdate) -> Unit = { update ->
                            viewModel.onEvent(
                                UpdatesEvent.OnChapterClick(
                                    mangaId = update.manga.id,
                                    chapterId = update.chapter.id
                                )
                            )
                        }
                        val onChapterLongClick: (MangaUpdate) -> Unit = { update ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onEvent(UpdatesEvent.OnChapterLongClick(update.chapter.id))
                        }
                        val onDownloadClick: (MangaUpdate) -> Unit = { update ->
                            viewModel.onEvent(
                                UpdatesEvent.OnDownloadChapter(
                                    mangaId = update.manga.id,
                                    chapterId = update.chapter.id
                                )
                            )
                        }
                        val onMarkAsRead: (Long) -> Unit = { chapterId ->
                            viewModel.onEvent(UpdatesEvent.MarkChapterAsRead(chapterId))
                        }
                        if (state.displayMode == UpdatesDisplayMode.GROUPED_BY_MANGA) {
                            // Compute the filtered manga groups from the flat filtered list
                            val filteredGroups = remember(state.groupedByManga, filteredUpdates) {
                                val filteredIds = filteredUpdates.map { it.chapter.id }.toSet()
                                state.groupedByManga
                                    .map { group ->
                                        group.copy(chapters = group.chapters.filter { it.chapter.id in filteredIds })
                                    }
                                    .filter { it.chapters.isNotEmpty() }
                            }
                            MangaGroupedUpdatesList(
                                groups = filteredGroups,
                                selectedItems = state.selectedItems,
                                activeDownloads = state.activeDownloads,
                                lastRunSummary = state.lastRunSummary,
                                onChapterClick = onChapterClick,
                                onChapterLongClick = onChapterLongClick,
                                onDownloadClick = onDownloadClick,
                                onMarkAsRead = onMarkAsRead,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            UpdatesList(
                                updates = filteredUpdates,
                                selectedItems = state.selectedItems,
                                activeDownloads = state.activeDownloads,
                                lastRunSummary = state.lastRunSummary,
                                onChapterClick = onChapterClick,
                                onChapterLongClick = onChapterLongClick,
                                onDownloadClick = onDownloadClick,
                                onMarkAsRead = onMarkAsRead,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Update Error Dialog
        if (state.showUpdateErrors) {
            UpdateErrorDialog(
                errors = state.updateErrors,
                onDismiss = { viewModel.onEvent(UpdatesEvent.HideUpdateErrors) },
                onClearError = { viewModel.onEvent(UpdatesEvent.ClearUpdateError(it)) },
                onClearAll = { viewModel.onEvent(UpdatesEvent.ClearAllUpdateErrors) }
            )
        }

        // To-Be-Updated Dialog
        if (state.showPendingUpdates) {
            PendingUpdatesDialog(
                pendingUpdates = state.pendingUpdates,
                onDismiss = { viewModel.onEvent(UpdatesEvent.HidePendingUpdates) },
                onStartUpdate = { viewModel.onEvent(UpdatesEvent.StartLibraryUpdate) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Manga-grouped list (Mihon-style)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaGroupedUpdatesList(
    groups: List<MangaUpdateGroup>,
    selectedItems: Set<Long>,
    activeDownloads: Map<Long, DownloadItem>,
    lastRunSummary: app.otakureader.domain.model.UpdateRunSummary?,
    onChapterClick: (MangaUpdate) -> Unit,
    onChapterLongClick: (MangaUpdate) -> Unit,
    onDownloadClick: (MangaUpdate) -> Unit,
    onMarkAsRead: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (lastRunSummary != null) {
            item(key = "last_run_summary") {
                LastRunSummaryCard(
                    summary = lastRunSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        groups.forEach { group ->
            // Manga header row: cover + title + chapter count chip
            item(key = "manga_header_${group.manga.id}") {
                MangaGroupHeader(manga = group.manga, chapterCount = group.chapters.size)
            }
            // Chapter rows (no cover thumbnail — the group header provides context)
            items(group.chapters, key = { it.chapter.id }) { update ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onMarkAsRead(update.chapter.id)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = androidx.compose.ui.Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                ) {
                    // Indented chapter row without the manga cover (already shown in header)
                    GroupedChapterItem(
                        update = update,
                        isSelected = selectedItems.contains(update.chapter.id),
                        activeDownload = activeDownloads[update.chapter.id],
                        onClick = { onChapterClick(update) },
                        onLongClick = { onChapterLongClick(update) },
                        onDownloadClick = { onDownloadClick(update) }
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MangaGroupHeader(
    manga: Manga,
    chapterCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Manga cover thumbnail
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(width = 40.dp, height = 56.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(manga.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(5f / 7f)
                    .clip(MaterialTheme.shapes.small)
            )
        }
        Text(
            text = manga.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // Chapter count chip
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = "$chapterCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun GroupedChapterItem(
    update: MangaUpdate,
    isSelected: Boolean,
    activeDownload: DownloadItem?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            // Indent to align with title text in the group header (cover width + spacing)
            .padding(start = 62.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            androidx.compose.material3.Checkbox(
                checked = true,
                onCheckedChange = { onClick() },
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = update.chapter.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (update.chapter.dateFetch > UNSET_FETCH_DATE) {
                Text(
                    text = formatFetchDate(update.chapter.dateFetch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (!isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                when (activeDownload?.status) {
                    DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                        progress = { (activeDownload.progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    DownloadStatus.QUEUED, DownloadStatus.PAUSED -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    else -> IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = stringResource(R.string.updates_download_chapter),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Date-grouped list
// ─────────────────────────────────────────────────────────────────────────────

private enum class UpdateDateBucket { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, OLDER }

private fun updateDateBucket(epochMs: Long): UpdateDateBucket {
    if (epochMs <= 0L) return UpdateDateBucket.OLDER
    val today = LocalDate.now()
    val day = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        day == today -> UpdateDateBucket.TODAY
        day == today.minusDays(1) -> UpdateDateBucket.YESTERDAY
        day >= today.minusDays(6) -> UpdateDateBucket.THIS_WEEK
        day >= today.minusDays(29) -> UpdateDateBucket.THIS_MONTH
        else -> UpdateDateBucket.OLDER
    }
}

private sealed interface UpdateListItem {
    data class Header(val bucket: UpdateDateBucket) : UpdateListItem
    data class Entry(val update: MangaUpdate) : UpdateListItem
}

@Composable
private fun bucketLabel(bucket: UpdateDateBucket): String = when (bucket) {
    UpdateDateBucket.TODAY -> stringResource(R.string.updates_date_today)
    UpdateDateBucket.YESTERDAY -> stringResource(R.string.updates_date_yesterday)
    UpdateDateBucket.THIS_WEEK -> stringResource(R.string.updates_date_this_week)
    UpdateDateBucket.THIS_MONTH -> stringResource(R.string.updates_date_this_month)
    UpdateDateBucket.OLDER -> stringResource(R.string.updates_date_older)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatesList(
    updates: List<MangaUpdate>,
    selectedItems: Set<Long>,
    activeDownloads: Map<Long, DownloadItem>,
    lastRunSummary: app.otakureader.domain.model.UpdateRunSummary?,
    onChapterClick: (MangaUpdate) -> Unit,
    onChapterLongClick: (MangaUpdate) -> Unit,
    onDownloadClick: (MangaUpdate) -> Unit,
    onMarkAsRead: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listItems = remember(updates) {
        buildList {
            var lastBucket: UpdateDateBucket? = null
            updates.forEach { update ->
                val bucket = updateDateBucket(update.chapter.dateFetch)
                if (bucket != lastBucket) {
                    add(UpdateListItem.Header(bucket))
                    lastBucket = bucket
                }
                add(UpdateListItem.Entry(update))
            }
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (lastRunSummary != null) {
            item(key = "last_run_summary") {
                LastRunSummaryCard(
                    summary = lastRunSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(listItems, key = { item ->
            when (item) {
                is UpdateListItem.Header -> "header_${item.bucket}"
                is UpdateListItem.Entry -> item.update.chapter.id
            }
        }) { item ->
            when (item) {
                is UpdateListItem.Header -> UpdatesDateHeader(label = bucketLabel(item.bucket))
                is UpdateListItem.Entry -> {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onMarkAsRead(item.update.chapter.id)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = androidx.compose.ui.Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    ) {
                        UpdateItem(
                            update = item.update,
                            isSelected = selectedItems.contains(item.update.chapter.id),
                            activeDownload = activeDownloads[item.update.chapter.id],
                            onClick = { onChapterClick(item.update) },
                            onLongClick = { onChapterLongClick(item.update) },
                            onDownloadClick = { onDownloadClick(item.update) }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun UpdatesDateHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun UpdateItem(
    update: MangaUpdate,
    isSelected: Boolean,
    activeDownload: DownloadItem?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox or cover thumbnail
        if (isSelected) {
            androidx.compose.material3.Checkbox(
                checked = true,
                onCheckedChange = { onClick() },
                modifier = Modifier.size(40.dp)
            )
        } else {
            // Manga cover thumbnail
            Surface(
                shape = MaterialTheme.shapes.small,
                // Tonal backdrop so a failed/missing thumbnail shows a neutral block
                // instead of an empty hole in the row.
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(width = 40.dp, height = 56.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(update.manga.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = update.manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(5f / 7f)
                        .clip(MaterialTheme.shapes.small)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = update.manga.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = update.chapter.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!isSelected) {
            if (update.chapter.dateFetch > UNSET_FETCH_DATE) {
                Text(
                    text = formatFetchDate(update.chapter.dateFetch),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 72.dp)
                )
            }
            // Per-item download button / progress indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                when (activeDownload?.status) {
                    DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                        progress = { (activeDownload.progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    DownloadStatus.QUEUED, DownloadStatus.PAUSED -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    else -> IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = stringResource(R.string.updates_download_chapter),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

// L-9: Return a descriptive fallback instead of an empty string so that the UI
// never shows a blank date field when formatting fails.
private fun formatFetchDate(epochMs: Long): String = runCatching {
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)
}.getOrDefault("Unknown date")

@Composable
private fun UpdateErrorDialog(
    errors: List<UpdateErrorEntry>,
    onDismiss: () -> Unit,
    onClearError: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.updates_error_title)) },
        text = {
            if (errors.isEmpty()) {
                Text(stringResource(R.string.updates_error_empty))
            } else {
                LazyColumn(modifier = modifier.fillMaxWidth()) {
                    items(errors, key = { it.mangaId }) { error ->
                        UpdateErrorItem(
                            error = error,
                            onClear = { onClearError(error.mangaId) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.updates_error_close))
            }
        },
        dismissButton = {
            if (errors.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text(stringResource(R.string.updates_error_clear_all))
                }
            }
        }
    )
}

@Composable
private fun UpdateErrorItem(
    error: UpdateErrorEntry,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = error.mangaTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = error.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (error.timestamp > 0L) {
                Text(
                    text = formatFetchDate(error.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = stringResource(R.string.updates_error_clear),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PendingUpdatesDialog(
    pendingUpdates: List<PendingUpdateManga>,
    onDismiss: () -> Unit,
    onStartUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.updates_pending_title)) },
        text = {
            if (pendingUpdates.isEmpty()) {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.updates_pending_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        text = stringResource(R.string.updates_pending_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column {
                    Text(
                        text = stringResource(R.string.updates_pending_count, pendingUpdates.size),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(modifier = modifier.fillMaxWidth()) {
                        items(pendingUpdates, key = { it.mangaId }) { manga ->
                            PendingUpdateItem(manga = manga)
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.updates_pending_close))
            }
        },
        dismissButton = {
            if (pendingUpdates.isNotEmpty()) {
                TextButton(onClick = onStartUpdate) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.updates_pending_start))
                }
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Last run summary diagnostics card (#1041)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LastRunSummaryCard(
    summary: app.otakureader.domain.model.UpdateRunSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.updates_last_run_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LastRunStat(
                    label = stringResource(R.string.updates_last_run_checked),
                    value = summary.checkedCount.toString(),
                )
                LastRunStat(
                    label = stringResource(R.string.updates_last_run_new),
                    value = summary.newChaptersCount.toString(),
                    highlight = summary.newChaptersCount > 0,
                )
                LastRunStat(
                    label = stringResource(R.string.updates_last_run_skipped),
                    value = summary.skippedCount.toString(),
                )
                LastRunStat(
                    label = stringResource(R.string.updates_last_run_failed),
                    value = summary.failedCount.toString(),
                    highlight = summary.failedCount > 0,
                    highlightError = true,
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.updates_last_run_meta,
                    formatFetchDate(summary.timestamp),
                    formatDurationMs(summary.durationMs),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LastRunStat(
    label: String,
    value: String,
    highlight: Boolean = false,
    highlightError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                highlight && highlightError -> MaterialTheme.colorScheme.error
                highlight -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDurationMs(durationMs: Long): String {
    val seconds = durationMs / 1_000
    return if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"
}

@Composable
private fun PendingUpdateItem(
    manga: PendingUpdateManga,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = manga.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = manga.sourceName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (manga.lastChecked > 0L) {
                Text(
                    text = stringResource(R.string.updates_pending_last_checked, formatFetchDate(manga.lastChecked)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
