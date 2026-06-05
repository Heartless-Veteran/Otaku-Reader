package app.otakureader.feature.more.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.more.R
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest

/**
 * Centralized list of every page bookmark in the library.
 *
 * Bookmarks are grouped by manga then by chapter. Each manga group can be expanded or
 * collapsed by tapping the header. A search bar filters the list by manga title, chapter
 * name, or note. Individual rows can be deleted with the delete button or by swiping left.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onOpenBookmark: (mangaId: Long, chapterId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is BookmarksEffect.NavigateToReader ->
                    onOpenBookmark(effect.mangaId, effect.chapterId)
                is BookmarksEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bookmarks_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.bookmarks_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search bar — filters by manga title, chapter name, or note text.
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onIntent(BookmarksIntent.SearchQueryChanged(it)) },
                placeholder = { Text(stringResource(R.string.bookmarks_search_placeholder)) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.onIntent(BookmarksIntent.SearchQueryChanged(""))
                            },
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.bookmarks_clear_search),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.isEmpty -> EmptyBookmarksState(
                    hasQuery = state.searchQuery.isNotEmpty(),
                )

                else -> BookmarkGroupedList(
                    groups = state.grouped,
                    onToggleExpand = { mangaId ->
                        viewModel.onIntent(BookmarksIntent.ToggleMangaExpanded(mangaId))
                    },
                    onOpenBookmark = { mangaId, chapterId ->
                        viewModel.onIntent(BookmarksIntent.OpenBookmark(mangaId, chapterId))
                    },
                    onDeleteBookmark = { item ->
                        viewModel.onIntent(BookmarksIntent.DeleteBookmark(item))
                    },
                )
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyBookmarksState(
    hasQuery: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (hasQuery) {
                stringResource(R.string.bookmarks_no_results)
            } else {
                stringResource(R.string.bookmarks_empty)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Grouped list ─────────────────────────────────────────────────────────────

@Composable
private fun BookmarkGroupedList(
    groups: List<BookmarkGroup>,
    onToggleExpand: (mangaId: Long) -> Unit,
    onOpenBookmark: (mangaId: Long, chapterId: Long) -> Unit,
    onDeleteBookmark: (BookmarkItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        groups.forEach { group ->
            // Manga header row — tap to expand/collapse the group.
            item(key = "header_${group.mangaId}") {
                MangaGroupHeader(
                    group = group,
                    onToggleExpand = { onToggleExpand(group.mangaId) },
                )
            }

            // Only show chapter / bookmark rows when the group is expanded.
            if (group.isExpanded) {
                group.chapters.forEach { chapterGroup ->
                    item(key = "chapter_${chapterGroup.chapterId}") {
                        ChapterSubHeader(chapterName = chapterGroup.chapterName)
                    }

                    items(
                        items = chapterGroup.bookmarks,
                        key = { bm -> "bookmark_${bm.id}" },
                    ) { bookmark ->
                        SwipeToDismissBookmarkRow(
                            bookmark = bookmark,
                            onOpen = { onOpenBookmark(bookmark.mangaId, bookmark.chapterId) },
                            onDelete = { onDeleteBookmark(bookmark) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }

            item(key = "divider_${group.mangaId}") {
                HorizontalDivider(thickness = 2.dp)
            }
        }
    }
}

// ─── Manga group header ───────────────────────────────────────────────────────

@Composable
private fun MangaGroupHeader(
    group: BookmarkGroup,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Manga cover thumbnail (40×56 dp, portrait aspect ratio).
        Surface(
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(width = 40.dp, height = 56.dp),
        ) {
            AsyncImage(
                model = group.mangaCoverUrl,
                contentDescription = group.mangaTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(5f / 7f)
                    .clip(MaterialTheme.shapes.small),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.mangaTitle.ifBlank {
                    stringResource(R.string.bookmarks_unknown_manga)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
            )
            val count = group.chapters.sumOf { it.bookmarks.size }
            Text(
                text = stringResource(R.string.bookmarks_count, count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = if (group.isExpanded) {
                Icons.Default.KeyboardArrowUp
            } else {
                Icons.Default.KeyboardArrowDown
            },
            contentDescription = if (group.isExpanded) {
                stringResource(R.string.bookmarks_collapse)
            } else {
                stringResource(R.string.bookmarks_expand)
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Chapter sub-header ───────────────────────────────────────────────────────

@Composable
private fun ChapterSubHeader(
    chapterName: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = chapterName.ifBlank { stringResource(R.string.bookmarks_chapter_fallback) },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
    )
}

// ─── Swipe-to-dismiss bookmark row ───────────────────────────────────────────

/**
 * Wraps [BookmarkRow] in a [SwipeToDismissBox] so the user can swipe left to delete.
 *
 * [SwipeToDismissBox] is the Material3 replacement for the old M2 SwipeToDismiss composable.
 * Swiping toward [SwipeToDismissBoxValue.EndToStart] (right-to-left) reveals a red delete
 * icon in the background. Once the drag exceeds the positional threshold the delete is
 * confirmed via [confirmValueChange] and [onDelete] is called.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissBookmarkRow(
    bookmark: BookmarkItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        // Require 40 % drag distance before confirming — reduces accidental swipes.
        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.bookmarks_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        BookmarkRow(
            bookmark = bookmark,
            onOpen = onOpen,
            onDelete = onDelete,
        )
    }
}

// ─── Bookmark row ─────────────────────────────────────────────────────────────

@Composable
private fun BookmarkRow(
    bookmark: BookmarkItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onOpen),
        headlineContent = {
            Text(stringResource(R.string.bookmarks_page, bookmark.pageIndex + 1))
        },
        supportingContent = if (!bookmark.note.isNullOrBlank()) {
            { Text(bookmark.note, maxLines = 2) }
        } else {
            null
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.bookmarks_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

// ─── Navigation extension ─────────────────────────────────────────────────────

fun NavGraphBuilder.bookmarksScreen(
    onNavigateBack: () -> Unit,
    onOpenBookmark: (mangaId: Long, chapterId: Long) -> Unit,
) {
    composable<Route.Bookmarks> {
        BookmarksScreen(onNavigateBack = onNavigateBack, onOpenBookmark = onOpenBookmark)
    }
}
