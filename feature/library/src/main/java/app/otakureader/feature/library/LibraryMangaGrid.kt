package app.otakureader.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.otakureader.core.ui.components.CompletedBadge
import app.otakureader.core.ui.components.DownloadBadge
import app.otakureader.core.ui.components.DroppedBadge
import app.otakureader.core.ui.components.MangaCard
import app.otakureader.core.ui.components.ManhwaCard
import app.otakureader.core.ui.theme.LocalOtakuColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width

/** Returns true when a manga item belongs to the manhwa/webtoon content type. */
internal fun isManhwa(manga: LibraryMangaItem): Boolean {
    val src = manga.sourceId.toString().lowercase()
    return src.contains("manhwa") || src.contains("webtoon") ||
        src.contains("korean") || src.contains("toon") || src.contains("naver")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MangaGrid(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
    adaptiveColumns: Int = state.gridSize,
    onMangaSelect: ((LibraryMangaItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedContentFilter by remember { mutableIntStateOf(0) }
    val contentTabs = listOf("All", "Manga", "Manhwa")

    val displayedManga by remember(state.mangaList, selectedContentFilter) {
        derivedStateOf {
            when (selectedContentFilter) {
                1 -> state.mangaList.filter { !isManhwa(it) }
                2 -> state.mangaList.filter { isManhwa(it) }
                else -> state.mangaList
            }
        }
    }

    // Header items shared by both grid variants
    val headerContent: @Composable () -> Unit = {
        // Daily reading goal banner
        if (state.readingGoal.dailyGoal > 0) {
            DailyGoalBanner(readingGoal = state.readingGoal)
        }

        // Continue Reading section
        if (state.continueReadingItems.isNotEmpty()) {
            ContinueReadingSection(
                items = state.continueReadingItems,
                onItemClick = { mangaId, chapterId ->
                    onEvent(LibraryEvent.ContinueReadingClick(mangaId, chapterId))
                }
            )
        }

        if (state.showRecommendations && state.recommendations.isNotEmpty()) {
            RecommendationsCarousel(
                items = state.recommendations,
                onMangaClick = { mangaId -> onEvent(LibraryEvent.OnMangaClick(mangaId)) },
                onDismiss = { mangaId -> onEvent(LibraryEvent.DismissRecommendation(mangaId)) },
            )
        }

        CategoryFilterChips(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            onCategorySelected = { onEvent(LibraryEvent.OnCategorySelected(it)) },
            modifier = Modifier.padding(vertical = 8.dp)
        )

        CategoryTabRow(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            onCategorySelected = { onEvent(LibraryEvent.OnCategorySelected(it)) },
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Reading list filter chips
        if (state.readingLists.isNotEmpty()) {
            ReadingListFilterChips(
                readingLists = state.readingLists,
                selectedListId = state.filterReadingListId,
                onListSelected = { onEvent(LibraryEvent.SetFilterReadingList(it)) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Content-type filter tabs
        TabRow(
            selectedTabIndex = selectedContentFilter,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (selectedContentFilter < tabPositions.size) {
                    val otakuColors = LocalOtakuColors.current
                    val indicatorColor = when (selectedContentFilter) {
                        1 -> otakuColors.contentFilterManga
                        2 -> otakuColors.contentFilterManhwa
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = tabPositions[selectedContentFilter].left)
                            .width(tabPositions[selectedContentFilter].width)
                            .height(3.dp)
                            .background(indicatorColor, RoundedCornerShape(2.dp))
                    )
                }
            }
        ) {
            contentTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedContentFilter == index,
                    onClick = { selectedContentFilter = index },
                    text = { Text(title) }
                )
            }
        }
    }

    if (state.isStaggeredGrid) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(130.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = modifier.fillMaxSize()
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                headerContent()
            }

            staggeredItems(
                items = displayedManga,
                key = { it.id },
                contentType = { "manga_card" }
            ) { manga ->
                val cardContent: @Composable () -> Unit = {
                    if (isManhwa(manga)) {
                        ManhwaCard(
                            coverUrl = manga.thumbnailUrl ?: "",
                            title = manga.title,
                            unreadCount = if (state.showBadges) manga.unreadCount else 0,
                            onClick = {
                                if (onMangaSelect != null) onMangaSelect(manga)
                                else onEvent(LibraryEvent.OnMangaClick(manga.id))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val readProgress = if (manga.totalChapterCount > 0) {
                            (manga.totalChapterCount - manga.unreadCount)
                                .coerceAtLeast(0)
                                .toFloat() / manga.totalChapterCount
                        } else null
                        val downloadCount = state.downloadCountByManga[manga.id] ?: 0
                        val continueReading = manga.lastRead != null && manga.unreadCount > 0
                        MangaCard(
                            title = manga.title,
                            coverUrl = manga.thumbnailUrl,
                            onClick = {
                                if (onMangaSelect != null) onMangaSelect(manga)
                                else onEvent(LibraryEvent.OnMangaClick(manga.id))
                            },
                            onLongClick = { onEvent(LibraryEvent.OnMangaLongClick(manga.id)) },
                            isSelected = manga.id in state.selectedManga,
                            readProgress = readProgress,
                            continueReading = continueReading,
                            isNew = manga.unreadCount > 0,
                            badge = when {
                                state.showBadges && manga.unreadCount > 0 && state.showDownloadBadge && downloadCount > 0 -> {
                                    {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            UnreadBadge(count = manga.unreadCount)
                                            DownloadBadge(count = downloadCount)
                                        }
                                    }
                                }
                                state.showBadges && manga.unreadCount > 0 -> {
                                    { UnreadBadge(count = manga.unreadCount) }
                                }
                                state.showDownloadBadge && downloadCount > 0 -> {
                                    { DownloadBadge(count = downloadCount) }
                                }
                                else -> null
                            },
                            statusBadge = when {
                                manga.userCompleted -> { { CompletedBadge() } }
                                manga.userDropped -> { { DroppedBadge() } }
                                else -> null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (state.visualEffectsEnabled) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        cardContent()
                    }
                } else {
                    cardContent()
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(adaptiveColumns),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                contentType = "header_section"
            ) {
                headerContent()
            }

            gridItems(
                items = displayedManga,
                key = { it.id },
                contentType = { "manga_card" }
            ) { manga ->
                val readProgress = if (manga.totalChapterCount > 0) {
                    (manga.totalChapterCount - manga.unreadCount)
                        .coerceAtLeast(0)
                        .toFloat() / manga.totalChapterCount
                } else null
                val downloadCount = state.downloadCountByManga[manga.id] ?: 0
                val continueReading = manga.lastRead != null && manga.unreadCount > 0
                val cardContent: @Composable () -> Unit = {
                    MangaCard(
                        title = manga.title,
                        coverUrl = manga.thumbnailUrl,
                        onClick = {
                            if (onMangaSelect != null) onMangaSelect(manga)
                            else onEvent(LibraryEvent.OnMangaClick(manga.id))
                        },
                        onLongClick = { onEvent(LibraryEvent.OnMangaLongClick(manga.id)) },
                        isSelected = manga.id in state.selectedManga,
                        readProgress = readProgress,
                        continueReading = continueReading,
                        isNew = manga.unreadCount > 0,
                        badge = when {
                            state.showBadges && manga.unreadCount > 0 && state.showDownloadBadge && downloadCount > 0 -> {
                                {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        UnreadBadge(count = manga.unreadCount)
                                        DownloadBadge(count = downloadCount)
                                    }
                                }
                            }
                            state.showBadges && manga.unreadCount > 0 -> {
                                { UnreadBadge(count = manga.unreadCount) }
                            }
                            state.showDownloadBadge && downloadCount > 0 -> {
                                { DownloadBadge(count = downloadCount) }
                            }
                            else -> null
                        },
                        statusBadge = when {
                            manga.userCompleted -> { { CompletedBadge() } }
                            manga.userDropped -> { { DroppedBadge() } }
                            else -> null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.visualEffectsEnabled) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        cardContent()
                    }
                } else {
                    cardContent()
                }
            }
        }
    }
}

@Composable
internal fun CategoryTabRow(
    categories: List<CategoryItem>,
    selectedCategory: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = categories.sumOf { it.count }
    val categoryTabs = listOf(
        CategoryItem(id = -1, name = stringResource(R.string.library_category_all), count = totalCount)
    ) + categories

    val selectedIndex = when (selectedCategory) {
        null -> 0
        else -> categoryTabs.indexOfFirst { it.id == selectedCategory }.coerceAtLeast(0)
    }

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 16.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        categoryTabs.forEachIndexed { index, category ->
            Tab(
                selected = selectedIndex == index,
                onClick = {
                    if (category.id == -1L) onCategorySelected(null)
                    else onCategorySelected(category.id)
                },
                text = {
                    Text(
                        text = "${category.name} ${category.count}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

@Composable
internal fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(24.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) stringResource(R.string.library_badge_overflow) else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
