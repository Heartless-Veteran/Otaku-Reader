package app.otakureader.feature.updates

import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.domain.model.DownloadItem
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaUpdate
import app.otakureader.domain.model.UpdateRunSummary

/**
 * A group of chapters from the same manga, used by the manga-grouped display mode.
 * The chapters list is sorted newest-first within the group.
 */
data class MangaUpdateGroup(
    val manga: Manga,
    val chapters: List<MangaUpdate>,
)

/** Controls whether the Updates list is grouped by manga or by date bucket. */
enum class UpdatesDisplayMode { GROUPED_BY_MANGA, GROUPED_BY_DATE }

/**
 * Represents a failed update entry for the error screen.
 */
data class UpdateErrorEntry(
    val mangaId: Long,
    val mangaTitle: String,
    val thumbnailUrl: String?,
    val errorMessage: String,
    val timestamp: Long
)

/**
 * Represents a manga that will be checked during the next library update.
 */
data class PendingUpdateManga(
    val mangaId: Long,
    val title: String,
    val thumbnailUrl: String?,
    val sourceName: String,
    val lastChecked: Long
)

data class UpdatesState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val updates: List<MangaUpdate> = emptyList(),
    val error: String? = null,
    /** Selected chapter IDs for bulk operations. */
    val selectedItems: Set<Long> = emptySet(),
    /** List of failed updates for the Update Error Screen. */
    val updateErrors: List<UpdateErrorEntry> = emptyList(),
    /** Whether the update error screen is visible. */
    val showUpdateErrors: Boolean = false,
    /** List of manga that will be checked in the next update. */
    val pendingUpdates: List<PendingUpdateManga> = emptyList(),
    /** Whether the To-Be-Updated screen is visible. */
    val showPendingUpdates: Boolean = false,
    /** Diagnostics card: summary of the last completed library update run (#1041). */
    val lastRunSummary: UpdateRunSummary? = null,
    /** Active/queued downloads keyed by chapterId, for per-row progress indicators. */
    val activeDownloads: Map<Long, DownloadItem> = emptyMap(),
    /** Start of the active date filter (epoch-ms, inclusive), or null if no filter set. */
    val dateFilterStart: Long? = null,
    /** End of the active date filter (epoch-ms, inclusive), or null if no filter set. */
    val dateFilterEnd: Long? = null,
    /** Chapters grouped by manga for the GROUPED_BY_MANGA display mode. */
    val groupedByManga: List<MangaUpdateGroup> = emptyList(),
    /** Whether to render the list grouped by manga or by date bucket. Default is manga-grouped (Mihon style). */
    val displayMode: UpdatesDisplayMode = UpdatesDisplayMode.GROUPED_BY_MANGA,
) : UiState

sealed interface UpdatesEvent : UiEvent {
    data object Refresh : UpdatesEvent
    data class OnChapterClick(val mangaId: Long, val chapterId: Long) : UpdatesEvent
    data class OnChapterLongClick(val chapterId: Long) : UpdatesEvent
    data class OnDownloadChapter(val mangaId: Long, val chapterId: Long) : UpdatesEvent
    data object ClearSelection : UpdatesEvent
    data object SelectAll : UpdatesEvent
    data object DownloadSelected : UpdatesEvent
    data object MarkSelectedAsRead : UpdatesEvent
    data class MarkChapterAsRead(val chapterId: Long) : UpdatesEvent

    // Update Error Screen events
    data object ShowUpdateErrors : UpdatesEvent
    data object HideUpdateErrors : UpdatesEvent
    data class ClearUpdateError(val mangaId: Long) : UpdatesEvent
    data object ClearAllUpdateErrors : UpdatesEvent

    // To-Be-Updated Screen events
    data object ShowPendingUpdates : UpdatesEvent
    data object HidePendingUpdates : UpdatesEvent
    data object StartLibraryUpdate : UpdatesEvent
    /** Revert a swipe-to-mark-read action. */
    data class UnmarkChapterAsRead(val chapterId: Long) : UpdatesEvent
    /** Revert a bulk mark-as-read action. */
    data class UnmarkSelectedAsRead(val chapterIds: Set<Long>) : UpdatesEvent
    /** Apply a date range filter. Pass null for either bound to leave it open-ended. */
    data class SetDateFilter(val start: Long?, val end: Long?) : UpdatesEvent
    /** Remove the active date range filter and show all update entries. */
    data object ClearDateFilter : UpdatesEvent
    /** Toggle between GROUPED_BY_MANGA and GROUPED_BY_DATE display modes. */
    data object ToggleDisplayMode : UpdatesEvent
}

sealed interface UpdatesEffect : UiEffect {
    data class NavigateToReader(val mangaId: Long, val chapterId: Long) : UpdatesEffect
    data class ShowSnackbar(val message: String) : UpdatesEffect
    /** Snackbar with an Undo action after swipe-to-mark-read. Awaited inline by the screen. */
    data class ShowUndoSnackbar(val message: String, val chapterId: Long) : UpdatesEffect
    /** Snackbar with an Undo action after bulk mark-as-read. */
    data class ShowUndoBulkReadSnackbar(val message: String, val chapterIds: Set<Long>) : UpdatesEffect
}
