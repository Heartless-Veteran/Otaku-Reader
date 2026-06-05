package app.otakureader.domain.scheduler

/**
 * Scheduler interface for reader-progress sync work.
 *
 * Implementations are expected to delegate to [WorkManager] and live in the
 * data layer.  The interface lives in the domain layer so that feature modules
 * can trigger sync without importing WorkManager or any data-layer class.
 */
interface SyncScheduler {
    /** Start (or keep) the periodic 15-minute sync job. */
    fun schedulePeriodicSync()

    /** Enqueue an immediate one-shot sync (replaces any pending one-shot). */
    fun enqueueSingleSync()
}
