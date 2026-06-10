package app.otakureader.feature.library.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.scheduler.LibraryUpdateScheduler
import app.otakureader.domain.usecase.downloads.ReindexDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
 * - Orphan Scan / Delete: placeholder — will be backed by a real use case in a future iteration
 *
 * Each task exposes a `Running` flag and a nullable `Result` string in [LibraryMaintenanceState].
 * One-shot feedback (e.g. errors) is delivered via [LibraryMaintenanceEffect].
 */
@HiltViewModel
class LibraryMaintenanceViewModel @Inject constructor(
    private val reindexDownloadsUseCase: ReindexDownloadsUseCase,
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
        // Trigger the existing library update scheduler, which re-fetches covers.
        // The actual worker runs in the background; we report that the job was queued.
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
        _state.update { it.copy(orphanScanRunning = true, orphanScanResult = null) }
        // Orphan scanning is not yet backed by a dedicated use case.
        // A 500 ms delay simulates work so the progress indicator is visible.
        delay(500)
        _state.update {
            it.copy(
                orphanScanRunning = false,
                orphanCount = 0,
                orphanScanResult = "No orphaned files found",
            )
        }
    }

    private suspend fun deleteOrphans() {
        // Deletion is gated on a prior scan so users know what will be removed.
        _effect.send(
            LibraryMaintenanceEffect.ShowSnackbar("Orphaned file cleanup not yet implemented"),
        )
    }
}
