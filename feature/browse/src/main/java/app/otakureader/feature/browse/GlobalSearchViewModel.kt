package app.otakureader.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.core.preferences.SearchHistoryPreferences
import app.otakureader.domain.usecase.source.GetSourcesUseCase
import app.otakureader.domain.usecase.source.GlobalSearchUseCase
import app.otakureader.sourceapi.MangaSource
import app.otakureader.sourceapi.toSourceId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val getSourcesUseCase: GetSourcesUseCase,
    private val globalSearchUseCase: GlobalSearchUseCase,
    private val generalPreferences: GeneralPreferences,
    private val searchHistoryPreferences: SearchHistoryPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(GlobalSearchState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GlobalSearchState()
    )

    private val _effect = Channel<GlobalSearchEffect>()
    val effect = _effect.receiveAsFlow()

    /** Tracks the currently active search so it can be cancelled on a new search. */
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            searchHistoryPreferences.recentSearches.collect { history ->
                _state.update { it.copy(recentSearches = history) }
            }
        }
        viewModelScope.launch {
            generalPreferences.pinnedSourceIds.collect { ids ->
                _state.update { it.copy(hasPinnedSources = ids.isNotEmpty()) }
            }
        }
    }

    fun initQuery(query: String) {
        if (query.isNotBlank() && _state.value.query.isBlank()) {
            _state.update { it.copy(query = query) }
            performSearch(query)
        }
    }

    fun onEvent(event: GlobalSearchEvent) {
        when (event) {
            is GlobalSearchEvent.OnQueryChange -> {
                _state.update { it.copy(query = event.query) }
            }
            is GlobalSearchEvent.Search -> {
                val query = _state.value.query
                if (query.isNotBlank()) {
                    performSearch(query)
                    viewModelScope.launch {
                        searchHistoryPreferences.addSearchQuery(query)
                    }
                }
            }
            is GlobalSearchEvent.OnMangaClick -> {
                viewModelScope.launch {
                    _effect.send(
                        GlobalSearchEffect.NavigateToMangaDetail(event.sourceId, event.manga.url)
                    )
                }
            }
            is GlobalSearchEvent.OnHistoryItemClick -> {
                _state.update { it.copy(query = event.query) }
                performSearch(event.query)
                viewModelScope.launch {
                    searchHistoryPreferences.addSearchQuery(event.query)
                }
            }
            is GlobalSearchEvent.OnClearHistory -> {
                viewModelScope.launch {
                    searchHistoryPreferences.clearHistory()
                }
            }
            is GlobalSearchEvent.OnRemoveHistoryItem -> {
                viewModelScope.launch {
                    searchHistoryPreferences.removeSearchQuery(event.query)
                }
            }
            is GlobalSearchEvent.OnToggleOnlyResults -> {
                _state.update { it.copy(onlyShowHasResults = !it.onlyShowHasResults) }
            }
            is GlobalSearchEvent.SetSourceFilter -> {
                _state.update { it.copy(sourceFilter = event.filter) }
            }
            is GlobalSearchEvent.ClickSource -> {
                viewModelScope.launch {
                    _effect.send(
                        GlobalSearchEffect.NavigateToSource(event.sourceId, _state.value.query),
                    )
                }
            }
        }
    }

    private fun performSearch(query: String) {
        // Cancel any in-flight search so stale results can't overwrite the new query's results
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val allSources: List<MangaSource> = getSourcesUseCase().first()
            val showNsfw = generalPreferences.showNsfwContent.first()
            val nsfwFiltered = if (showNsfw) allSources else allSources.filter { !it.isNsfw }

            // When PinnedOnly is selected and there are pinned sources, restrict to those
            val sourceFilter = _state.value.sourceFilter
            val pinnedIds = generalPreferences.pinnedSourceIds.first()
            val sources = if (
                sourceFilter == GlobalSearchSourceFilter.PinnedOnly && pinnedIds.isNotEmpty()
            ) {
                nsfwFiltered.filter { it.id.toSourceId() in pinnedIds }
            } else {
                nsfwFiltered
            }

            if (sources.isEmpty()) {
                _state.update { it.copy(isSearching = false, sourceResults = emptyList()) }
                return@launch
            }

            // Initialise each source with a loading state
            _state.update {
                it.copy(
                    isSearching = true,
                    sourceResults = sources.map { source ->
                        SourceSearchResult(
                            sourceId = source.id,
                            sourceName = source.name,
                            sourceLanguage = source.lang,
                            isLoading = true
                        )
                    }
                )
            }

            // Search each source concurrently; failures are captured per-source
            sources.forEach { source ->
                launch {
                    val result = globalSearchUseCase(source.id, query)
                    _state.update { state ->
                        val updatedResults = state.sourceResults.map { sr ->
                            if (sr.sourceId == source.id) {
                                result.fold(
                                    onSuccess = { page ->
                                        sr.copy(results = page.mangas, isLoading = false)
                                    },
                                    onFailure = { error ->
                                        sr.copy(
                                            isLoading = false,
                                            error = error.message ?: "Search failed"
                                        )
                                    }
                                )
                            } else {
                                sr
                            }
                        }
                        // Mark overall search as done once all sources have responded
                        val allDone = updatedResults.none { it.isLoading }
                        state.copy(
                            isSearching = !allDone,
                            sourceResults = updatedResults
                        )
                    }
                }
            }
        }
    }
}
