package app.otakureader.domain.usecase

import app.otakureader.domain.repository.TrackerSyncRepository
import javax.inject.Inject

/**
 * Pushes all pending local tracker states to their respective remote trackers (#1025).
 *
 * Uses the existing [TrackerSyncRepository.syncAllPending] logic which staggers requests
 * at ~350 ms per entry (well within MAL's 5 req/s limit) and handles conflict resolution
 * per the per-tracker configuration.
 *
 * This use case must not be invoked while incognito mode is active — callers are
 * responsible for checking that guard before invoking.
 */
class SyncLibraryUseCase @Inject constructor(
    private val trackerSyncRepository: TrackerSyncRepository,
) {
    suspend operator fun invoke(): TrackerSyncRepository.SyncSummary =
        trackerSyncRepository.syncAllPending()
}
