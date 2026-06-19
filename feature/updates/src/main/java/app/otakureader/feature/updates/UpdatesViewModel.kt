package app.otakureader.feature.updates

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.model.DownloadBlockedException
import app.otakureader.domain.repository.DownloadRepository
import app.otakureader.domain.scheduler.LibraryUpdateScheduler
import app.otakureader.domain.usecase.GetLastUpdateRunSummaryUseCase
import app.otakureader.domain.usecase.GetLibraryMangaUseCase
import app.otakureader.domain.usecase.GetRecentUpdatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getRecentUpdatesUseCase: GetRecentUpdatesUseCase,
    private val getLibraryMangaUseCase: GetLibraryMangaUseCase,
    private val getLastUpdateRunSummaryUseCase: GetLastUpdateRunSummaryUseCase,
    private val generalPreferences: GeneralPreferences,
    private val downloadRepository: DownloadRepository,
    private val chapterRepository: ChapterRepository,
    private val libraryUpdateScheduler: LibraryUpdateScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdatesState())
    val state: StateFlow<UpdatesState> = _state.asStateFlow()

    private val _effect = Channel<UpdatesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadUpdates()
        loadLastRunSummary()
        markUpdatesViewed()
        observeActiveDownloads()
    }

    fun onEvent(event: UpdatesEvent) {
        when (event) {
            UpdatesEvent.Refresh -> loadUpdates()
            is UpdatesEvent.OnChapterClick -> handleChapterClick(event.mangaId, event.chapterId)
            is UpdatesEvent.OnChapterLongClick -> toggleSelection(event.chapterId)
            is UpdatesEvent.OnDownloadChapter -> downloadChapter(event.mangaId, event.chapterId)
            UpdatesEvent.ClearSelection -> _state.update { it.copy(selectedItems = emptySet()) }
            UpdatesEvent.SelectAll -> selectAll()
            UpdatesEvent.DownloadSelected -> downloadSelected()
            UpdatesEvent.MarkSelectedAsRead -> markSelectedAsRead()
            is UpdatesEvent.MarkChapterAsRead -> markSingleChapterAsRead(event.chapterId)
            is UpdatesEvent.UnmarkChapterAsRead -> unmarkChapterAsRead(event.chapterId)

            // Update Error Screen events
            UpdatesEvent.ShowUpdateErrors -> _state.update { it.copy(showUpdateErrors = true) }
            UpdatesEvent.HideUpdateErrors -> _state.update { it.copy(showUpdateErrors = false) }
            is UpdatesEvent.ClearUpdateError -> _state.update { state ->
                state.copy(updateErrors = state.updateErrors.filter { it.mangaId != event.mangaId })
            }
            UpdatesEvent.ClearAllUpdateErrors -> _state.update { it.copy(updateErrors = emptyList()) }

            // To-Be-Updated Screen events
            UpdatesEvent.ShowPendingUpdates -> {
                _state.update { it.copy(showPendingUpdates = true) }
                loadPendingUpdates()
            }
            UpdatesEvent.HidePendingUpdates -> _state.update { it.copy(showPendingUpdates = false) }
            UpdatesEvent.StartLibraryUpdate -> startLibraryUpdate()
            is UpdatesEvent.SetDateFilter -> _state.update {
                it.copy(dateFilterStart = event.start, dateFilterEnd = event.end)
            }
            UpdatesEvent.ClearDateFilter -> _state.update {
                it.copy(dateFilterStart = null, dateFilterEnd = null)
            }
        }
    }

    private fun handleChapterClick(mangaId: Long, chapterId: Long) {
        if (_state.value.selectedItems.isNotEmpty()) {
            toggleSelection(chapterId)
        } else {
            viewModelScope.launch {
                _effect.send(UpdatesEffect.NavigateToReader(mangaId, chapterId))
            }
        }
    }

    private fun toggleSelection(chapterId: Long) {
        _state.update { state ->
            val sel = state.selectedItems
            state.copy(
                selectedItems = if (chapterId in sel) sel - chapterId else sel + chapterId
            )
        }
    }

    private fun selectAll() {
        _state.update { state ->
            state.copy(selectedItems = state.updates.map { it.chapter.id }.toSet())
        }
    }

    private fun downloadChapter(mangaId: Long, chapterId: Long) {
        val update = _state.value.updates.find { it.chapter.id == chapterId } ?: return
        viewModelScope.launch {
            runCatching {
                downloadRepository.enqueueChapter(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    mangaTitle = update.manga.title,
                    chapterTitle = update.chapter.name,
                    sourceName = update.manga.sourceId.toString()
                )
            }.onSuccess {
                _effect.send(UpdatesEffect.ShowSnackbar(
                    context.getString(R.string.updates_download_queued, update.chapter.name)
                ))
            }.onFailure { e ->
                val message = if (e is DownloadBlockedException) {
                    context.getString(R.string.updates_download_blocked_data_saver)
                } else {
                    context.getString(R.string.updates_download_failed, update.chapter.name)
                }
                _effect.send(UpdatesEffect.ShowSnackbar(message))
            }
        }
    }

    private fun downloadSelected() {
        val selected = _state.value.selectedItems
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val updates = _state.value.updates.filter { it.chapter.id in selected }
            var successCount = 0
            var failCount = 0
            var blockedByDataSaver = false
            updates.forEach { update ->
                runCatching {
                    downloadRepository.enqueueChapter(
                        mangaId = update.manga.id,
                        chapterId = update.chapter.id,
                        mangaTitle = update.manga.title,
                        chapterTitle = update.chapter.name,
                        sourceName = update.manga.sourceId.toString()
                    )
                }.onSuccess { successCount++ }.onFailure { e ->
                    if (e is DownloadBlockedException) blockedByDataSaver = true
                    failCount++
                }
            }
            _state.update { it.copy(selectedItems = emptySet()) }
            val message = when {
                blockedByDataSaver -> context.getString(R.string.updates_bulk_download_data_saver_blocked)
                failCount == 0 -> context.getString(R.string.updates_bulk_download_queued, successCount)
                else -> context.getString(R.string.updates_bulk_download_partial, successCount, failCount)
            }
            _effect.send(UpdatesEffect.ShowSnackbar(message))
        }
    }

    private fun markSelectedAsRead() {
        val selected = _state.value.selectedItems
        if (selected.isEmpty()) return
        val count = selected.size
        viewModelScope.launch {
            // try/catch with explicit CancellationException rethrow — coroutine cancellation
            // (e.g. ViewModel cleared mid-update) shouldn't surface as "Failed to mark as read".
            try {
                chapterRepository.updateChapterProgress(selected, read = true, lastPageRead = 0)
                // Subtract only the IDs we actually processed from the current selection.
                // Clearing wholesale would also wipe any new selections the user made while
                // the DB write was in flight — losing UI state they expect to keep.
                _state.update { it.copy(selectedItems = it.selectedItems - selected) }
                _effect.send(
                    UpdatesEffect.ShowSnackbar(
                        context.resources.getQuantityString(R.plurals.updates_marked_as_read, count, count)
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _effect.send(UpdatesEffect.ShowSnackbar(context.getString(R.string.updates_mark_as_read_failed)))
            }
        }
    }

    private fun markSingleChapterAsRead(chapterId: Long) {
        viewModelScope.launch {
            try {
                chapterRepository.updateChapterProgress(setOf(chapterId), read = true, lastPageRead = 0)
                _effect.send(
                    UpdatesEffect.ShowUndoSnackbar(
                        message = context.getString(R.string.updates_marked_as_read_single),
                        chapterId = chapterId,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _effect.send(UpdatesEffect.ShowSnackbar(context.getString(R.string.updates_mark_as_read_failed)))
            }
        }
    }

    private fun unmarkChapterAsRead(chapterId: Long) {
        viewModelScope.launch {
            try {
                chapterRepository.updateChapterProgress(setOf(chapterId), read = false, lastPageRead = 0)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _effect.send(UpdatesEffect.ShowSnackbar(context.getString(R.string.updates_unmark_as_read_failed)))
            }
        }
    }

    private fun loadUpdates() {
        _state.update { it.copy(isLoading = true, error = null) }
        getRecentUpdatesUseCase()
            .onEach { updates ->
                _state.update { it.copy(isLoading = false, updates = updates) }
            }
            .catch { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
            .launchIn(viewModelScope)
    }

    /** Observe the last library update run summary for the diagnostics card. */
    private fun loadLastRunSummary() {
        getLastUpdateRunSummaryUseCase()
            .onEach { summary ->
                _state.update { it.copy(lastRunSummary = summary) }
            }
            .catch { /* diagnostics failure should not affect the main updates list */ }
            .launchIn(viewModelScope)
    }

    /** Observe the download queue and surface active/queued items for per-row progress UI. */
    private fun observeActiveDownloads() {
        downloadRepository.observeDownloads()
            .onEach { items ->
                _state.update { it.copy(activeDownloads = items.filter { d -> d.isActive }.associateBy { d -> d.chapterId }) }
            }
            .catch { /* download progress failure should not affect the main updates list */ }
            .launchIn(viewModelScope)
    }

    /** Record the current time so the Library badge counter resets to zero. */
    private fun markUpdatesViewed() {
        viewModelScope.launch {
            generalPreferences.setLastUpdatesViewedAt(System.currentTimeMillis())
        }
    }

    /** Load manga that will be checked during the next library update. */
    private fun loadPendingUpdates() {
        viewModelScope.launch {
            try {
                val libraryManga = getLibraryMangaUseCase().first()
                val pendingManga = libraryManga.map { manga ->
                    PendingUpdateManga(
                        mangaId = manga.id,
                        title = manga.title,
                        thumbnailUrl = manga.thumbnailUrl,
                        sourceName = manga.sourceId.toString(),
                        lastChecked = 0L
                    )
                }
                _state.update { state -> state.copy(pendingUpdates = pendingManga) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { state -> state.copy(pendingUpdates = emptyList()) }
            }
        }
    }

    /** Start a manual library update; shows a brief pull-to-refresh indicator. */
    private fun startLibraryUpdate() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true, showPendingUpdates = false) }
        viewModelScope.launch {
            try {
                libraryUpdateScheduler.enqueueNow()
                _effect.send(UpdatesEffect.ShowSnackbar(context.getString(R.string.updates_library_update_started)))
                delay(REFRESH_INDICATOR_DURATION_MS)
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    companion object {
        const val REFRESH_INDICATOR_DURATION_MS = 1_500L
    }
}
