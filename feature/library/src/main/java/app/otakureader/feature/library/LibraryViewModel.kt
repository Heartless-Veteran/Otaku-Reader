package app.otakureader.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.core.preferences.LibraryPreferences
import app.otakureader.core.preferences.ReadingGoalPreferences
import app.otakureader.core.ui.selection.SelectionManager
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.DownloadRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.ReaderSettingsRepository
import app.otakureader.domain.repository.ReadingListRepository
import app.otakureader.domain.repository.StatisticsRepository
import app.otakureader.domain.scheduler.LibraryUpdateScheduler
import app.otakureader.domain.tracking.TrackRepository
import app.otakureader.domain.usecase.GetCategoriesUseCase
import app.otakureader.domain.usecase.GetContinueReadingUseCase
import app.otakureader.domain.usecase.GetLibraryMangaUseCase
import app.otakureader.domain.usecase.GetRecommendationsUseCase
import app.otakureader.domain.usecase.SearchLibraryMangaUseCase
import app.otakureader.domain.usecase.ToggleFavoriteMangaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryManga: GetLibraryMangaUseCase,
    private val searchLibraryManga: SearchLibraryMangaUseCase,
    private val toggleFavoriteManga: ToggleFavoriteMangaUseCase,
    private val libraryPreferences: LibraryPreferences,
    private val generalPreferences: GeneralPreferences,
    private val chapterRepository: ChapterRepository,
    private val mangaRepository: MangaRepository,
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: ReaderSettingsRepository,
    private val trackRepository: TrackRepository,
    private val getCategories: GetCategoriesUseCase,
    private val getContinueReading: GetContinueReadingUseCase,
    private val readingGoalPreferences: ReadingGoalPreferences,
    private val statisticsRepository: StatisticsRepository,
    private val readingListRepository: ReadingListRepository,
    private val getRecommendations: GetRecommendationsUseCase,
    private val libraryUpdateScheduler: LibraryUpdateScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    /** Atomic selection manager — keys are manga IDs. */
    private val selection = SelectionManager<Long>()

    private val _effect = Channel<LibraryEffect>(Channel.BUFFERED)
    val effect: Flow<LibraryEffect> = _effect.receiveAsFlow()

    /** Holds the full, unfiltered library items for reactive filtering. */
    private val _allItems = MutableStateFlow<List<LibraryMangaItem>>(emptyList())

    /** IDs of manga matching the current search query; null when no search is active. */
    private val _searchMatchingIds = MutableStateFlow<Set<Long>?>(null)
    private var searchJob: Job? = null

    init {
        loadLibrary()
        loadCategories()
        observeLibraryPreferences()
        observeFilteredItems()
        observeNewUpdatesCount()
        observeContinueReading()
        observeGoalProgress()
        observeReadingLists()
        observeDownloadCounts()
        observeIncognitoMode()
        // Sync selection manager into state so UI recomposes
        selection.selected
            .onEach { ids -> _state.update { it.copy(selectedManga = ids) } }
            .launchIn(viewModelScope)
        observeRecommendations()
    }

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.Refresh, is LibraryEvent.OnMangaClick,
            is LibraryEvent.OnMangaLongClick, is LibraryEvent.ContinueReadingClick -> handleNavEvent(event)
            is LibraryEvent.OnSearchQueryChange, is LibraryEvent.ToggleSearchBar,
            is LibraryEvent.OnCategorySelected, is LibraryEvent.ClearSelection -> handleUiEvent(event)
            is LibraryEvent.FilterHasNotes, is LibraryEvent.SetSortMode, is LibraryEvent.SetFilterMode,
            is LibraryEvent.SetFilterSource, is LibraryEvent.ToggleNsfw, is LibraryEvent.SetFilterReadingList,
            is LibraryEvent.SetGenreFilter, is LibraryEvent.SetSortAscending,
            is LibraryEvent.ClearAllFilters -> handleFilterSortEvent(event)
            is LibraryEvent.ToggleFilterSheet -> _state.update { it.copy(showBottomSheet = !it.showBottomSheet) }
            is LibraryEvent.ToggleBottomSheet -> _state.update { it.copy(showBottomSheet = !it.showBottomSheet) }
            is LibraryEvent.SetBottomSheetTab -> _state.update { it.copy(bottomSheetTab = event.tab) }
            is LibraryEvent.SetGroupByCategory -> viewModelScope.launch { libraryPreferences.setGroupByCategory(event.enabled) }
            is LibraryEvent.SetGridSize -> viewModelScope.launch { libraryPreferences.setGridSize(event.size) }
            is LibraryEvent.SetShowBadges -> viewModelScope.launch { libraryPreferences.setShowBadges(event.enabled) }
            is LibraryEvent.SetShowDownloadBadge ->
                viewModelScope.launch { libraryPreferences.setShowDownloadBadge(event.enabled) }
            is LibraryEvent.SetStaggeredGrid ->
                viewModelScope.launch { libraryPreferences.setStaggeredGrid(event.enabled) }
            is LibraryEvent.ToggleIncognito -> toggleIncognitoMode()
            is LibraryEvent.DismissRecommendation -> dismissRecommendation(event.mangaId)
            is LibraryEvent.ToggleAdvancedSearch -> _state.update { it.copy(showAdvancedSearch = !it.showAdvancedSearch) }
            is LibraryEvent.ApplyAdvancedSearch -> applyAdvancedSearch(event.authorQuery, event.tagQuery)
            is LibraryEvent.ToggleFavorite, is LibraryEvent.MarkSelectedAsRead,
            is LibraryEvent.MarkSelectedAsUnread, is LibraryEvent.RemoveSelectedFromLibrary,
            is LibraryEvent.DownloadSelected, is LibraryEvent.MarkSelectedAsCompleted,
            is LibraryEvent.MarkSelectedAsDropped, is LibraryEvent.ShareSelectedManga,
            is LibraryEvent.ViewSelectedManga -> handleActionEvent(event)
            is LibraryEvent.UpdateLibrary, is LibraryEvent.UpdateCategory,
            is LibraryEvent.OpenRandomEntry, is LibraryEvent.ReindexDownloads -> handleNewEvent(event)
        }
    }

    private fun handleNavEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.Refresh -> loadLibrary()
            is LibraryEvent.OnMangaClick -> onMangaClick(event.mangaId)
            is LibraryEvent.OnMangaLongClick -> onMangaLongClick(event.mangaId)
            is LibraryEvent.ContinueReadingClick -> onContinueReadingClick(event.mangaId, event.chapterId)
            else -> Unit
        }
    }

    private fun handleUiEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.OnSearchQueryChange -> onSearchQueryChange(event.query)
            is LibraryEvent.ToggleSearchBar -> toggleSearchBar()
            is LibraryEvent.OnCategorySelected -> onCategorySelected(event.categoryId)
            is LibraryEvent.ClearSelection -> clearSelection()
            else -> Unit
        }
    }

    private fun handleFilterSortEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.FilterHasNotes -> onFilterHasNotes(event.enabled)
            is LibraryEvent.SetSortMode -> onSetSortMode(event.mode)
            is LibraryEvent.SetFilterMode -> onSetFilterMode(event.mode)
            is LibraryEvent.SetFilterSource -> onSetFilterSource(event.sourceId)
            is LibraryEvent.ToggleNsfw -> onToggleNsfw(event.show)
            is LibraryEvent.SetFilterReadingList -> onSetFilterReadingList(event.listId)
            is LibraryEvent.SetGenreFilter -> _state.update { it.copy(filterGenres = event.genres) }
            is LibraryEvent.SetSortAscending -> _state.update { it.copy(sortAscending = event.ascending) }
            is LibraryEvent.ClearAllFilters -> _state.update {
                it.copy(
                    filterMode = LibraryFilterMode.ALL,
                    filterGenres = emptySet(),
                    filterHasNotes = false,
                    filterSourceId = null,
                    filterReadingListId = null,
                    sortAscending = true,
                )
            }
            else -> Unit
        }
    }

    private fun handleActionEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.ToggleFavorite -> toggleFavorite(event.mangaId)
            is LibraryEvent.MarkSelectedAsRead -> markSelectedAsRead()
            is LibraryEvent.MarkSelectedAsUnread -> markSelectedAsUnread()
            is LibraryEvent.RemoveSelectedFromLibrary -> removeSelectedFromLibrary()
            is LibraryEvent.DownloadSelected -> downloadSelected()
            is LibraryEvent.MarkSelectedAsCompleted -> markSelectedAsCompleted()
            is LibraryEvent.MarkSelectedAsDropped -> markSelectedAsDropped()
            is LibraryEvent.ShareSelectedManga -> shareSelectedManga()
            is LibraryEvent.ViewSelectedManga -> viewSelectedManga()
            else -> Unit
        }
    }

    private fun handleNewEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.UpdateLibrary -> viewModelScope.launch {
                libraryUpdateScheduler.enqueueNow()
                _effect.send(LibraryEffect.ShowSnackbar("Library update started"))
            }
            is LibraryEvent.UpdateCategory -> viewModelScope.launch {
                libraryUpdateScheduler.enqueueNow()
                _effect.send(LibraryEffect.ShowSnackbar("Category update started — per-category filtering applied by worker"))
            }
            is LibraryEvent.OpenRandomEntry -> {
                val currentList = _state.value.mangaList
                if (currentList.isNotEmpty()) {
                    val random = currentList.random()
                    viewModelScope.launch { _effect.send(LibraryEffect.NavigateToManga(random.id)) }
                }
            }
            is LibraryEvent.ReindexDownloads -> viewModelScope.launch {
                _effect.send(LibraryEffect.ShowSnackbar("Download reindex not yet available"))
            }
            else -> Unit
        }
    }

    private fun observeLibraryPreferences() {
        // Observe each preference independently to avoid 6-flow combine type-inference limitation
        libraryPreferences.gridSize
            .onEach { gridSize -> _state.update { it.copy(gridSize = gridSize) } }
            .launchIn(viewModelScope)
        libraryPreferences.showBadges
            .onEach { showBadges -> _state.update { it.copy(showBadges = showBadges) } }
            .launchIn(viewModelScope)
        libraryPreferences.showDownloadBadge
            .onEach { show -> _state.update { it.copy(showDownloadBadge = show) } }
            .launchIn(viewModelScope)
        libraryPreferences.librarySortMode
            .onEach { sortModeInt ->
                _state.update {
                    it.copy(sortMode = LibrarySortMode.entries.getOrElse(sortModeInt) { LibrarySortMode.ALPHABETICAL })
                }
            }
            .launchIn(viewModelScope)
        libraryPreferences.libraryFilterMode
            .onEach { filterModeInt ->
                _state.update {
                    it.copy(filterMode = LibraryFilterMode.entries.getOrElse(filterModeInt) { LibraryFilterMode.ALL })
                }
            }
            .launchIn(viewModelScope)
        libraryPreferences.libraryFilterSourceId
            .onEach { filterSourceId -> _state.update { it.copy(filterSourceId = filterSourceId) } }
            .launchIn(viewModelScope)
        generalPreferences.showNsfwContent
            .onEach { showNsfw -> _state.update { it.copy(showNsfw = showNsfw) } }
            .launchIn(viewModelScope)
        libraryPreferences.isStaggeredGrid
            .onEach { staggered -> _state.update { it.copy(isStaggeredGrid = staggered) } }
            .launchIn(viewModelScope)
        generalPreferences.visualEffectsEnabled
            .onEach { enabled -> _state.update { it.copy(visualEffectsEnabled = enabled) } }
            .launchIn(viewModelScope)
        libraryPreferences.groupByCategory
            .onEach { groupByCategory -> _state.update { it.copy(groupByCategory = groupByCategory) } }
            .launchIn(viewModelScope)
    }

    private fun observeNewUpdatesCount() {
        generalPreferences.lastUpdatesViewedAt
            .flatMapLatest { since -> chapterRepository.countNewUpdatesSince(since) }
            .onEach { count -> _state.update { it.copy(newUpdatesCount = count) } }
            .launchIn(viewModelScope)
    }

    private fun loadLibrary() {
        val isRefreshing = _state.value.mangaList.isNotEmpty()
        _state.update { it.copy(isLoading = !isRefreshing, isRefreshing = isRefreshing) }

        getLibraryManga()
            .map { mangaList ->
                coroutineScope {
                    // Build tracking lookup in parallel
                    val trackingDeferred = mangaList.map { manga ->
                        async {
                            val hasEntries = trackRepository.observeEntriesForManga(manga.id)
                                .first().isNotEmpty()
                            if (hasEntries) manga.id else null
                        }
                    }

                    // Build download lookup in parallel
                    val downloadDeferred = mangaList.map { manga ->
                        async {
                            val hasDownloads = downloadRepository.hasMangaDownloads(
                                sourceName = manga.sourceId.toString(),
                                mangaTitle = manga.title
                            )
                            if (hasDownloads) manga.id else null
                        }
                    }

                    val trackedMangaIds = trackingDeferred.awaitAll().filterNotNull().toSet()
                    val downloadedMangaIds = downloadDeferred.awaitAll().filterNotNull().toSet()

                    mangaList.map { manga ->
                        manga.toLibraryItem(
                            isDownloaded = manga.id in downloadedMangaIds,
                            hasTracking = manga.id in trackedMangaIds
                        )
                    }
                }
            }
            .onEach { items ->
                _allItems.value = items
                val genres = items.flatMap { it.genres }.distinct().sorted()
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = null, availableGenres = genres) }
            }
            .catch { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategories()
                .collect { categories ->
                    val items = categories.map { category ->
                        CategoryItem(
                            id = category.id,
                            name = category.name,
                            count = category.mangaCount
                        )
                    }
                    _state.update { it.copy(categories = items) }
                }
        }
    }

    private fun observeFilteredItems() {
        // When category selection changes, fetch the manga IDs in that category
        viewModelScope.launch {
            _state.map { it.selectedCategory }
                .distinctUntilChanged()
                .collect { categoryId ->
                    if (categoryId != null) {
                        val mangaIds = getCategories.getMangaIdsForCategory(categoryId).first()
                        _state.update { it.copy(categoryFilterMangaIds = mangaIds.toSet()) }
                    } else {
                        _state.update { it.copy(categoryFilterMangaIds = emptySet()) }
                    }
                }
        }

        // When reading list filter changes, reactively track the manga IDs in that list
        _state.map { it.filterReadingListId }
            .distinctUntilChanged()
            .flatMapLatest { listId ->
                if (listId != null) {
                    readingListRepository.getListWithManga(listId)
                        .map { (_, items) -> items.map { it.manga.id }.toSet() }
                } else {
                    flowOf(emptySet())
                }
            }
            .onEach { ids -> _state.update { it.copy(readingListMangaIds = ids) } }
            .launchIn(viewModelScope)

        // Combine all items with filter params (now including category, reading list, and search result IDs from state)
        combine(
            _allItems,
            _searchMatchingIds,
            _state.map {
                FilterSortParams(
                    query = it.searchQuery,
                    searchMatchingIds = null, // populated by combine below
                    filterHasNotes = it.filterHasNotes,
                    sortMode = it.sortMode,
                    filterMode = it.filterMode,
                    filterSourceId = it.filterSourceId,
                    showNsfw = it.showNsfw,
                    selectedCategory = it.selectedCategory,
                    categoryMangaIds = it.categoryFilterMangaIds,
                    filterReadingListId = it.filterReadingListId,
                    readingListMangaIds = it.readingListMangaIds,
                    filterGenres = it.filterGenres,
                    sortAscending = it.sortAscending,
                )
            }.distinctUntilChanged()
        ) { items, matchingIds, params ->
            applyFiltersAndSort(items, params.copy(searchMatchingIds = matchingIds))
        }
            .onEach { filtered ->
                _state.update { it.copy(mangaList = filtered) }
            }
            .launchIn(viewModelScope)
    }

    private fun onMangaClick(mangaId: Long) {
        // Atomic check-and-toggle: if selection is active, toggle inside update block
        // to prevent race between read and write.
        _state.update { state ->
            if (state.selectedManga.isNotEmpty()) {
                // Selection mode is active — toggle this manga and stay in selection mode
                val newSelection = if (state.selectedManga.contains(mangaId)) {
                    state.selectedManga - mangaId
                } else {
                    state.selectedManga + mangaId
                }
                state.copy(selectedManga = newSelection)
            } else {
                // No selection active — navigate (effect is fired after update)
                state
            }
        }
        // Only navigate if we did NOT toggle (selection was empty before click)
        if (_state.value.selectedManga.isEmpty()) {
            viewModelScope.launch {
                _effect.send(LibraryEffect.NavigateToManga(mangaId))
            }
        }
    }

    private fun onMangaLongClick(mangaId: Long) {
        selection.toggle(mangaId)
    }

    private fun onSearchQueryChange(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(searchQuery = query, isSearching = false) }
            _searchMatchingIds.value = null
            return
        }
        // Mark searching so the UI shows progress instead of a false "no results" while the
        // debounced query is in flight (the matching-id set is empty until results land).
        _state.update { it.copy(searchQuery = query, isSearching = true) }
        _searchMatchingIds.value = emptySet()
        searchJob = viewModelScope.launch {
            delay(300L)
            searchLibraryManga(query).collect { mangas ->
                _searchMatchingIds.value = mangas.map { it.id }.toSet()
                _state.update { it.copy(isSearching = false) }
            }
        }
    }

    private fun toggleSearchBar() {
        _state.update { state ->
            if (state.showSearchBar) {
                // Closing: also clear the query and dismiss any open advanced search sheet
                searchJob?.cancel()
                _searchMatchingIds.value = null
                state.copy(showSearchBar = false, searchQuery = "", showAdvancedSearch = false)
            } else {
                state.copy(showSearchBar = true)
            }
        }
    }

    private fun onCategorySelected(categoryId: Long?) {
        _state.update { it.copy(selectedCategory = categoryId) }
    }

    private fun clearSelection() {
        selection.clear()
    }

    private fun markSelectedAsRead() = markChaptersForSelectedManga(read = true)

    private fun markSelectedAsUnread() = markChaptersForSelectedManga(read = false)

    private fun markChaptersForSelectedManga(read: Boolean) {
        val mangaIds = selection.snapshotAndClear()
        if (mangaIds.isEmpty()) return
        viewModelScope.launch {
            val chapterIds = mangaIds.flatMap { mangaId ->
                chapterRepository.getChaptersByMangaIdSync(mangaId).map { it.id }
            }
            if (chapterIds.isNotEmpty()) {
                chapterRepository.updateChapterProgress(chapterIds, read = read, lastPageRead = 0)
            }
        }
    }

    private fun removeSelectedFromLibrary() {
        val ids = selection.snapshotAndClear()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { mangaId -> toggleFavoriteManga(mangaId) }
        }
    }

    private fun downloadSelected() {
        val mangaIds = selection.snapshotAndClear()
        if (mangaIds.isEmpty()) return
        viewModelScope.launch {
            val mangaById = _allItems.value.associateBy { it.id }
            mangaIds.forEach { mangaId ->
                val manga = mangaById[mangaId] ?: return@forEach
                val chapters = chapterRepository.getChaptersByMangaId(mangaId).first()
                chapters.filter { !it.read }.forEach { chapter ->
                    downloadRepository.enqueueChapter(
                        mangaId = mangaId,
                        chapterId = chapter.id,
                        mangaTitle = manga.title,
                        chapterTitle = chapter.name
                    )
                }
            }
        }
    }

    private fun markSelectedAsCompleted() {
        val mangaIds = selection.snapshotAndClear()
        if (mangaIds.isEmpty()) return
        viewModelScope.launch {
            mangaIds.forEach { mangaRepository.markUserCompleted(it, completed = true) }
        }
    }

    private fun markSelectedAsDropped() {
        val mangaIds = selection.snapshotAndClear()
        if (mangaIds.isEmpty()) return
        viewModelScope.launch {
            mangaIds.forEach { mangaRepository.markUserDropped(it, dropped = true) }
        }
    }

    private fun shareSelectedManga() {
        val mangaId = _state.value.selectedManga.singleOrNull() ?: return
        selection.clear()
        viewModelScope.launch {
            val manga = mangaRepository.getMangaById(mangaId) ?: return@launch
            val url = manga.url.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: ""
            _effect.send(LibraryEffect.ShareManga(title = manga.title, url = url))
        }
    }

    private fun viewSelectedManga() {
        val mangaId = _state.value.selectedManga.singleOrNull() ?: return
        selection.clear()
        viewModelScope.launch { _effect.send(LibraryEffect.NavigateToManga(mangaId)) }
    }

    private fun toggleFavorite(mangaId: Long) {
        viewModelScope.launch {
            toggleFavoriteManga(mangaId)
        }
    }

    private fun onFilterHasNotes(enabled: Boolean) {
        _state.update { it.copy(filterHasNotes = enabled) }
    }

    private fun onSetSortMode(mode: LibrarySortMode) {
        viewModelScope.launch {
            libraryPreferences.setLibrarySortMode(mode.ordinal)
        }
    }

    private fun onSetFilterMode(mode: LibraryFilterMode) {
        viewModelScope.launch {
            libraryPreferences.setLibraryFilterMode(mode.ordinal)
        }
    }

    private fun onSetFilterSource(sourceId: Long?) {
        viewModelScope.launch {
            libraryPreferences.setLibraryFilterSourceId(sourceId)
        }
    }

    private fun onToggleNsfw(show: Boolean) {
        viewModelScope.launch {
            generalPreferences.setShowNsfwContent(show)
        }
    }

    private fun observeContinueReading() {
        getContinueReading()
            .onEach { items -> _state.update { it.copy(continueReadingItems = items) } }
            .catch { e -> android.util.Log.w("LibraryViewModel", "observeContinueReading failed", e) }
            .launchIn(viewModelScope)
    }

    private fun observeGoalProgress() {
        combine(
            readingGoalPreferences.dailyChapterGoal,
            readingGoalPreferences.weeklyChapterGoal
        ) { daily, weekly -> Pair(daily, weekly) }
            .flatMapLatest { (dailyGoal, weeklyGoal) ->
                statisticsRepository.getReadingGoalProgress(dailyGoal, weeklyGoal)
            }
            .onEach { goal -> _state.update { it.copy(readingGoal = goal) } }
            .catch { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("LibraryViewModel", "observeGoalProgress failed", e)
            }
            .launchIn(viewModelScope)
    }

    private fun onContinueReadingClick(mangaId: Long, chapterId: Long) {
        viewModelScope.launch {
            _effect.send(LibraryEffect.NavigateToReader(mangaId, chapterId))
        }
    }

    private fun observeReadingLists() {
        readingListRepository.getAllLists()
            .map { lists ->
                lists.map { list ->
                    ReadingListFilterItem(
                        id = list.id,
                        name = list.name,
                        count = list.itemCount
                    )
                }
            }
            .onEach { items ->
                _state.update { state ->
                    val currentListId = state.filterReadingListId
                    val stillExists = currentListId == null || items.any { it.id == currentListId }
                    state.copy(
                        readingLists = items,
                        filterReadingListId = if (stillExists) currentListId else null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onSetFilterReadingList(listId: Long?) {
        _state.update { it.copy(filterReadingListId = listId) }
    }

    private fun observeDownloadCounts() {
        downloadRepository.observeDownloads()
            .map { downloads ->
                downloads.groupingBy { it.mangaId }.eachCount()
            }
            .distinctUntilChanged()
            .onEach { counts -> _state.update { it.copy(downloadCountByManga = counts) } }
            .catch { e -> android.util.Log.w("LibraryViewModel", "observeDownloadCounts failed", e) }
            .launchIn(viewModelScope)
    }

    private fun observeIncognitoMode() {
        settingsRepository.incognitoMode
            .onEach { enabled -> _state.update { it.copy(incognitoMode = enabled) } }
            .launchIn(viewModelScope)
    }

    private fun toggleIncognitoMode() {
        viewModelScope.launch {
            val current = _state.value.incognitoMode
            settingsRepository.setIncognitoMode(!current)
        }
    }

    private fun observeRecommendations() {
        libraryPreferences.showRecommendations
            .onEach { show -> _state.update { it.copy(showRecommendations = show) } }
            .launchIn(viewModelScope)

        combine(
            getRecommendations(),
            libraryPreferences.dismissedRecommendations,
        ) { recs, dismissed ->
            recs.filter { it.mangaId.toString() !in dismissed }
                .map { rec ->
                    LibraryMangaItem(
                        id = rec.mangaId,
                        title = rec.title,
                        thumbnailUrl = rec.thumbnailUrl,
                        unreadCount = 0,
                        isFavorite = false,
                        sourceId = rec.sourceId,
                    )
                }
        }
            .onEach { items -> _state.update { it.copy(recommendations = items) } }
            .launchIn(viewModelScope)
    }

    private fun dismissRecommendation(mangaId: Long) {
        viewModelScope.launch {
            libraryPreferences.dismissRecommendation(mangaId)
        }
    }

    private fun applyAdvancedSearch(authorQuery: String, tagQuery: String) {
        // Strip any existing author:/tag: operators from the current query before appending new ones
        val baseQuery = _state.value.searchQuery
            .replace(Regex("""author:"[^"]*""""), "")
            .replace(Regex("""tag:"[^"]*""""), "")
            .replace(Regex("""author:\S+"""), "")
            .replace(Regex("""tag:\S+"""), "")
            .trim()

        val parts = buildList {
            if (authorQuery.isNotBlank()) {
                val value = authorQuery.trim()
                add(if (' ' in value) """author:"$value"""" else "author:$value")
            }
            if (tagQuery.isNotBlank()) {
                val value = tagQuery.trim()
                add(if (' ' in value) """tag:"$value"""" else "tag:$value")
            }
        }
        val newQuery = listOf(baseQuery).plus(parts).filter { it.isNotBlank() }.joinToString(" ").trim()
        _state.update { it.copy(showAdvancedSearch = false, searchQuery = newQuery) }
        onSearchQueryChange(newQuery)
    }
}
