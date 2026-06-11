package app.otakureader.feature.library.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.scheduler.LibraryUpdateScheduler
import app.otakureader.domain.usecase.downloads.DeleteOrphanedDownloadsUseCase
import app.otakureader.domain.usecase.downloads.ReindexDownloadsUseCase
import app.otakureader.domain.usecase.downloads.ScanOrphanedDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Library Maintenance screen.
 *
 * Handles four maintenance tasks:
 * - Refresh Covers: triggers the library update scheduler (which re-downloads covers)
 * - Refresh Metadata: triggers the library update scheduler to pull fresh metadata
 * - Reindex Downloads: calls [ReindexDownloadsUseCase] to sync download status with disk
 * - Orphan Scan / Delete: backed by [ScanOrphanedDownloadsUseCase] /
 *   [DeleteOrphanedDownloadsUseCase], which compare the downloads directory against the
 *   manga/chapter database and remove stale directories
 *
 * Each task exposes a `Running` flag and a nullable `Result` string in [LibraryMaintenanceState].
 * One-shot feedback (e.g. errors) is delivered via [LibraryMaintenanceEffect].
 */
@HiltViewModel
class LibraryMaintenanceViewModel @Inject constructor(
    private val reindexDownloadsUseCase: ReindexDownloadsUseCase,
    private val scanOrphanedDownloadsUseCase: ScanOrphanedDownloadsUseCase,
    private val deleteOrphanedDownloadsUseCase: DeleteOrphanedDownloadsUseCase,
    private val libraryUpdateScheduler: LibraryUpdateScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryMaintenanceState())
    val state: StateFlow<LibraryMaintenanceState> = _state.asStateFlow()

    private val _effect = Channel<LibraryMaintenanceEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: LibraryMaintenanceEvent) {
        viewModelScope.launch {
            when (event) {
                LibraryMaintenanceEvent.RefreshCovers -> refreshCovers()
                LibraryMaintenanceEvent.RefreshMetadata -> refreshMetadata()
                LibraryMaintenanceEvent.ReindexDownloads -> reindexDownloads()
                LibraryMaintenanceEvent.ScanOrphans -> scanOrphans()
                LibraryMaintenanceEvent.DeleteOrphans -> deleteOrphans()
            }
        }
    }

    private suspend fun refreshCovers() {
        _state.update { it.copy(coverRefreshRunning = true, coverRefreshResult = null) }
        runCatching { libraryUpdateScheduler.enqueueNow() }
            .onSuccess {
                _state.update {
                    it.copy(
                        coverRefreshRunning = false,
                        coverRefreshResult = "Cover refresh queued",
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(coverRefreshRunning = false) }
                _effect.send(LibraryMaintenanceEffect.ShowSnackbar("Cover refresh failed: ${error.message}"))
            }
    }

    private suspend fun refreshMetadata() {
        _state.update { it.copy(metadataRefreshRunning = true, metadataRefreshResult = null) }
        runCatching { libraryUpdateScheduler.enqueueNow() }
            .onSuccess {
                _state.update {
                    it.copy(
                        metadataRefreshRunning = false,
                        metadataRefreshResult = "Metadata refresh queued",
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(metadataRefreshRunning = false) }
                _effect.send(LibraryMaintenanceEffect.ShowSnackbar("Metadata refresh failed: ${error.message}"))
            }
    }

    private suspend fun reindexDownloads() {
        _state.update { it.copy(reindexRunning = true, reindexResult = null) }
        runCatching { reindexDownloadsUseCase() }
            .onSuccess { result ->
                _state.update {
                    it.copy(
                        reindexRunning = false,
                        reindexResult = "Reindex complete: ${result.verifiedDownloads} chapters verified",
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(reindexRunning = false) }
                _effect.send(LibraryMaintenanceEffect.ShowSnackbar("Reindex failed: ${error.message}"))
            }
    }

    private suspend fun scanOrphans() {
        // Guard against repeated taps spawning concurrent disk scans.
        if (_state.value.orphanScanRunning) return
        _state.update { it.copy(orphanScanRunning = true, orphanScanResult = null) }
        runCatching { scanOrphanedDownloadsUseCase() }
            .onSuccess { result ->
                val resultText = if (result.count == 0) {
                    "No orphaned files found"
                } else {
                    "${result.count} orphaned folder(s) found (${result.sizeBytes.formatBytes()})"
                }
                _state.update {
                    it.copy(
                        orphanScanRunning = false,
                        orphanCount = result.count,
                        orphanedSizeBytes = result.sizeBytes,
                        orphanScanResult = resultText,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(orphanScanRunning = false) }
                _effect.send(LibraryMaintenanceEffect.ShowSnackbar("Scan failed: ${error.message}"))
            }
    }

    private suspend fun deleteOrphans() {
        // Guard against repeated taps spawning concurrent delete operations.
        if (_state.value.orphanScanRunning) return
        if (_state.value.orphanCount == 0) return
        _state.update { it.copy(orphanScanRunning = true, orphanScanResult = null) }
        runCatching { deleteOrphanedDownloadsUseCase() }
            .onSuccess { result ->
                val resultText = if (result.count == 0) {
                    "No orphaned files found"
                } else {
                    "Deleted ${result.count} folder(s) (${result.sizeBytes.formatBytes()} freed)"
                }
                _state.update {
                    it.copy(
                        orphanScanRunning = false,
                        orphanCount = 0,
                        orphanedSizeBytes = 0L,
                        orphanScanResult = resultText,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(orphanScanRunning = false) }
                _effect.send(LibraryMaintenanceEffect.ShowSnackbar("Delete failed: ${error.message}"))
            }
    }

    private fun Long.formatBytes(): String = when {
        this < 1_024L -> "$this B"
        this < 1_048_576L -> "%.1f KB".format(this / 1_024.0)
        this < 1_073_741_824L -> "%.1f MB".format(this / 1_048_576.0)
        else -> "%.2f GB".format(this / 1_073_741_824.0)
    }
}
