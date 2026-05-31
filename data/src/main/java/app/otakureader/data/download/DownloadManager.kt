package app.otakureader.data.download

import android.content.Context
import app.otakureader.core.common.network.NetworkMonitor
import app.otakureader.core.common.network.NetworkType
import app.otakureader.core.database.dao.DownloadQueueDao
import app.otakureader.core.database.entity.DownloadQueueEntity
import app.otakureader.core.preferences.DownloadPreferences
import app.otakureader.domain.model.DownloadItem
import app.otakureader.domain.model.DownloadPriority
import app.otakureader.domain.model.DownloadStatus
import app.otakureader.core.common.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Holds everything needed to download a chapter.
 *
 * @param pageUrls ordered list of remote image URLs; may be empty when pages have not
 *                 been resolved from the source yet.
 * @param priority queue priority for this request; lower values are processed first.
 *                 Defaults to [DownloadPriority.NORMAL].
 */
data class ChapterDownloadRequest(
    val mangaId: Long,
    val chapterId: Long,
    val sourceName: String,
    val mangaTitle: String,
    val chapterTitle: String,
    val pageUrls: List<String>,
    val priority: Int = DownloadPriority.NORMAL
)

/**
 * Manages the chapter download queue and coordinates actual file downloads via [Downloader].
 *
 * Chapters are added to an in-memory queue backed by a [StateFlow]. Pages for a single chapter
 * are downloaded sequentially, but multiple chapters may be downloaded concurrently (typically
 * one coroutine per chapter, and callers may enqueue many at once).
 *
 * The queue is sorted by [DownloadItem.priority] (ascending – lower value = higher priority)
 * before being emitted.  Items with equal priority are ordered by insertion time (FIFO).
 *
 * Already-downloaded pages are skipped automatically (provided the file is non-empty),
 * making the process idempotent. Partial files from interrupted downloads are re-downloaded.
 *
 * All mutations to [jobs] and [requests] are performed under [mutex] to prevent races.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: Downloader,
    private val downloadPreferences: DownloadPreferences,
    private val networkMonitor: NetworkMonitor,
    private val downloadQueueDao: DownloadQueueDao,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val mutex = Mutex()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    /** Active coroutine jobs keyed by chapterId. */
    private val jobs = mutableMapOf<Long, Job>()

    /** Stored requests keyed by chapterId so that paused/failed/completed downloads can be resumed. */
    private val requests = mutableMapOf<Long, ChapterDownloadRequest>()

    /** Internal map for O(1) lookup and updates of download items by chapterId. */
    private val downloadMap = mutableMapOf<Long, DownloadItem>()

    /** Maximum concurrent downloads from user preference (default: 2) */
    private var maxConcurrentDownloads: Int = 2

    init {
        scope.launch {
            downloadPreferences.concurrentDownloads.collect { max ->
                maxConcurrentDownloads = max.coerceIn(1, 5)
            }
        }
        scope.launch { restoreQueueFromDb() }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    suspend fun enqueue(request: ChapterDownloadRequest) {
        // Block downloads on mobile data when Data Saver is enabled.
        if (downloadPreferences.dataSaverEnabled.first() &&
            networkMonitor.currentNetwork() == NetworkType.MOBILE
        ) {
            return
        }

        val shouldStart = mutex.withLock {
            val existing = downloadMap[request.chapterId]
            // Allow re-enqueueing for terminal states (COMPLETED, FAILED) or when the item
            // is not present at all (i.e., never queued, or previously canceled via cancel()
            // which removes the item). Active (QUEUED, DOWNLOADING) and PAUSED downloads
            // are not re-enqueued; use resume() for PAUSED.
            if (existing != null &&
                existing.status != DownloadStatus.COMPLETED &&
                existing.status != DownloadStatus.FAILED
            ) return

            requests[request.chapterId] = request
            val newItem = DownloadItem(
                id = request.chapterId,
                mangaId = request.mangaId,
                chapterId = request.chapterId,
                mangaTitle = request.mangaTitle,
                chapterTitle = request.chapterTitle,
                status = DownloadStatus.QUEUED,
                priority = request.priority
            )
            downloadMap[request.chapterId] = newItem
            refreshDownloadsList()

            // Check if we can start immediately or need to wait for a slot
            jobs.size < maxConcurrentDownloads
        }

        downloadQueueDao.insert(
            DownloadQueueEntity(
                chapterId = request.chapterId,
                mangaId = request.mangaId,
                mangaTitle = request.mangaTitle,
                chapterTitle = request.chapterTitle,
                sourceName = request.sourceName,
                pageUrlsJson = Json.encodeToString(request.pageUrls),
                priority = request.priority,
                status = DownloadStatus.QUEUED.name,
                addedAt = System.currentTimeMillis()
            )
        )

        // Start download only if under the concurrent limit
        if (shouldStart) {
            launchDownloadJob(request.chapterId, request)
        }
    }

    suspend fun pause(chapterId: Long) {
        mutex.withLock {
            jobs.remove(chapterId)?.cancel()
            updateStatus(chapterId, DownloadStatus.PAUSED)
        }
        downloadQueueDao.updateStatus(chapterId, DownloadStatus.PAUSED.name)
    }

    suspend fun resume(chapterId: Long) {
        val canStart = mutex.withLock {
            val item = downloadMap[chapterId]
            if (item?.status != DownloadStatus.PAUSED) return
            // Only start immediately if under the limit
            jobs.size < maxConcurrentDownloads
        }
        
        val request = requests[chapterId] ?: return
        
        if (canStart) {
            launchDownloadJob(chapterId, request)
        } else {
            // Just mark as queued, will be picked up when a slot opens
            mutex.withLock { updateStatus(chapterId, DownloadStatus.QUEUED) }
            downloadQueueDao.updateStatus(chapterId, DownloadStatus.QUEUED.name)
        }
    }

    /**
     * Re-queues a FAILED download so it is attempted again.
     *
     * [resume] only un-pauses PAUSED items, so it can't recover a FAILED one; retrying instead
     * re-enqueues the originally stored request (enqueue already permits re-queueing terminal
     * states). No-op if the chapter isn't FAILED or its request is no longer available.
     */
    suspend fun retry(chapterId: Long) {
        val request = mutex.withLock {
            if (downloadMap[chapterId]?.status != DownloadStatus.FAILED) return
            requests[chapterId]
        } ?: return
        enqueue(request)
    }

    suspend fun cancel(chapterId: Long) {
        mutex.withLock {
            jobs.remove(chapterId)?.cancel()
            requests.remove(chapterId)
            downloadMap.remove(chapterId)
            refreshDownloadsList()
        }
        downloadQueueDao.delete(chapterId)
    }

    /**
     * Removes a completed or paused download and its in-memory metadata.
     * This is used when the on-disk chapter is deleted.
     */
    suspend fun remove(chapterId: Long) {
        mutex.withLock {
            jobs.remove(chapterId)?.cancel()
            requests.remove(chapterId)
            downloadMap.remove(chapterId)
            refreshDownloadsList()
        }
        downloadQueueDao.delete(chapterId)
    }

    suspend fun clearAll() {
        mutex.withLock {
            jobs.values.forEach { it.cancel() }
            jobs.clear()
            requests.clear()
            downloadMap.clear()
            _downloads.value = emptyList()
        }
        downloadQueueDao.deleteAll()
    }

    /**
     * Moves the given chapter to the front of the in-memory queue by assigning it a
     * priority value lower than all currently queued items.
     *
     * This affects the ordering of the emitted [downloads] list, but does not interrupt
     * or preempt already-running downloads in the current implementation.
     *
     * If the chapter is not in the queue this is a no-op.
     */
    suspend fun prioritize(chapterId: Long) {
        val newPriority = mutex.withLock {
            val item = downloadMap[chapterId] ?: return
            val minPriority = downloadMap.values
                .filter { it.chapterId != chapterId }
                .minOfOrNull { it.priority } ?: DownloadPriority.NORMAL
            val p = if (minPriority > Int.MIN_VALUE) minPriority - 1 else Int.MIN_VALUE
            downloadMap[chapterId] = item.copy(priority = p)
            requests[chapterId]?.let { requests[chapterId] = it.copy(priority = p) }
            refreshDownloadsList()
            p
        }
        downloadQueueDao.updatePriority(chapterId, newPriority)
    }

    /**
     * Sets an explicit [newPriority] value for the given chapter.
     *
     * Lower values appear earlier in the queue (higher urgency).  Use the constants in
     * [DownloadPriority] for common presets.  If the chapter is not in the queue this
     * is a no-op.
     */
    suspend fun reorder(chapterId: Long, newPriority: Int) {
        val updated = mutex.withLock {
            val item = downloadMap[chapterId] ?: return
            downloadMap[chapterId] = item.copy(priority = newPriority)
            requests[chapterId]?.let { requests[chapterId] = it.copy(priority = newPriority) }
            refreshDownloadsList()
            true
        }
        if (updated) downloadQueueDao.updatePriority(chapterId, newPriority)
    }

    /**
     * Moves a list of chapters to the front of the queue in a single transaction.
     *
     * Each chapter in [chapterIds] receives a priority value lower than every chapter
     * that is not in the list. Chapters within the list retain their relative order from
     * the current queue. This is more efficient than calling [prioritize] repeatedly
     * because only one mutex acquisition and one list rebuild are required.
     *
     * IDs that are not currently in the queue are silently ignored.
     *
     * Duplicates in [chapterIds] are treated by first occurrence only.
     *
     * @param chapterIds Ordered list of chapter IDs to prioritize (defensive copy is made)
     */
    suspend fun prioritizeAll(chapterIds: List<Long>) {
        // Short-circuit: empty input is a no-op
        if (chapterIds.isEmpty()) return

        // Defensive copy to prevent concurrent mutation by caller
        val chapterIdsCopy = chapterIds.toList()
        // Collect all final (chapterId → priority) pairs so we can persist after releasing the lock.
        val priorityUpdates = mutableMapOf<Long, Int>()
        mutex.withLock {
            val chapterIdSet = chapterIdsCopy.toHashSet()
            // Determine the targets in their current queue order.
            val orderedTargets = downloadMap.values
                .filter { it.chapterId in chapterIdSet }
                .sortedBy { it.priority }
            if (orderedTargets.isEmpty()) {
                // Nothing to prioritize; all IDs were absent from the queue.
                return@withLock
            }

            val outsideMin = downloadMap.values
                .filter { it.chapterId !in chapterIdSet }
                .minOfOrNull { it.priority } ?: DownloadPriority.NORMAL

            // How many distinct Int slots exist below outsideMin, using Long math to avoid overflow.
            val availableBelow = outsideMin.toLong() - Int.MIN_VALUE.toLong()

            if (availableBelow >= orderedTargets.size.toLong()) {
                // Enough room: assign contiguous priorities just below outsideMin.
                val base = outsideMin.toLong() - orderedTargets.size.toLong()
                orderedTargets.forEachIndexed { index, item ->
                    val newPriority = (base + index.toLong()).toInt()
                    downloadMap[item.chapterId] = item.copy(priority = newPriority)
                    requests[item.chapterId]?.let {
                        requests[item.chapterId] = it.copy(priority = newPriority)
                    }
                    priorityUpdates[item.chapterId] = newPriority
                }
            } else {
                // Not enough room below outsideMin. Renormalize non-targets to non-negative
                // priorities, then place all targets in the Int.MIN_VALUE.. range so they
                // are strictly lower than every non-target.
                val nonTargets = downloadMap.values
                    .filter { it.chapterId !in chapterIdSet }
                    .sortedBy { it.priority }

                nonTargets.forEachIndexed { index, item ->
                    // Index is non-negative and well within Int range for realistic queues.
                    val normalizedPriority = index
                    downloadMap[item.chapterId] = item.copy(priority = normalizedPriority)
                    requests[item.chapterId]?.let {
                        requests[item.chapterId] = it.copy(priority = normalizedPriority)
                    }
                    priorityUpdates[item.chapterId] = normalizedPriority
                }

                orderedTargets.forEachIndexed { index, item ->
                    val newPriority = Int.MIN_VALUE + index
                    downloadMap[item.chapterId] = item.copy(priority = newPriority)
                    requests[item.chapterId]?.let {
                        requests[item.chapterId] = it.copy(priority = newPriority)
                    }
                    priorityUpdates[item.chapterId] = newPriority
                }
            }

            refreshDownloadsList()
        }
        for ((id, priority) in priorityUpdates) {
            downloadQueueDao.updatePriority(id, priority)
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun updateStatus(chapterId: Long, status: DownloadStatus) {
        updateDownloadInPlace(chapterId) { item ->
            item.copy(status = status)
        }
    }

    private fun updateProgress(chapterId: Long, progress: Int) {
        updateDownloadInPlace(chapterId) { item ->
            item.copy(progress = progress)
        }
    }

    /**
     * Updates a single [DownloadItem] in both [downloadMap] and [_downloads] without
     * re-sorting the entire list.  This preserves the existing ordering, which is
     * already sorted by [DownloadItem.priority] when items are enqueued or their
     * priority changes.
     *
     * Must be called while holding [mutex].
     */
    private fun updateDownloadInPlace(
        chapterId: Long,
        transform: (DownloadItem) -> DownloadItem
    ) {
        val current = downloadMap[chapterId] ?: return
        val updated = transform(current)
        downloadMap[chapterId] = updated

        _downloads.update { list ->
            list.map { item ->
                if (item.chapterId == chapterId) updated else item
            }
        }
    }

    /**
     * Rebuilds the public [downloads] list, sorted by [DownloadItem.priority] ascending
     * (lower value = higher priority).  Items with equal priority retain their original
     * insertion order (stable sort).
     *
     * Must be called while holding [mutex].
     */
    private fun refreshDownloadsList() {
        _downloads.value = downloadMap.values.sortedBy { it.priority }
    }

    private suspend fun restoreQueueFromDb() {
        val entities = downloadQueueDao.getAllOnce()
        if (entities.isEmpty()) return

        mutex.withLock {
            for (entity in entities) {
                val pageUrls = try {
                    Json.decodeFromString<List<String>>(entity.pageUrlsJson)
                } catch (_: Exception) {
                    emptyList()
                }
                val request = ChapterDownloadRequest(
                    mangaId = entity.mangaId,
                    chapterId = entity.chapterId,
                    sourceName = entity.sourceName,
                    mangaTitle = entity.mangaTitle,
                    chapterTitle = entity.chapterTitle,
                    pageUrls = pageUrls,
                    priority = entity.priority
                )
                requests[entity.chapterId] = request
                // DOWNLOADING in DB means the job was interrupted — restore as QUEUED.
                val restoredStatus = when (entity.status) {
                    DownloadStatus.PAUSED.name -> DownloadStatus.PAUSED
                    DownloadStatus.FAILED.name -> DownloadStatus.FAILED
                    else -> DownloadStatus.QUEUED
                }
                downloadMap[entity.chapterId] = DownloadItem(
                    id = entity.chapterId,
                    mangaId = entity.mangaId,
                    chapterId = entity.chapterId,
                    mangaTitle = entity.mangaTitle,
                    chapterTitle = entity.chapterTitle,
                    status = restoredStatus,
                    priority = entity.priority
                )
            }
            refreshDownloadsList()
        }
        // Fill all available concurrent slots, not just one.
        repeat(maxConcurrentDownloads) { processPendingQueue() }
    }

    /**
     * Processes the pending download queue when slots become available.
     * Finds the highest priority queued download and starts it if under the limit.
     * Must NOT be called while holding [mutex].
     *
     * The pending item and its request are resolved under the lock, then the lock is
     * released before calling [launchDownloadJob].  This avoids a re-entrant deadlock:
     * [launchDownloadJob] is a suspend function that also acquires [mutex], and the
     * coroutine mutex is NOT reentrant.
     */
    private fun processPendingQueue() {
        scope.launch {
            // Resolve the next candidate under the lock, then release before launching.
            val toStart = mutex.withLock {
                if (jobs.size >= maxConcurrentDownloads) return@launch

                val pendingItem = downloadMap.values
                    .filter { it.status == DownloadStatus.QUEUED && !jobs.containsKey(it.chapterId) }
                    .minByOrNull { it.priority }
                    ?: return@launch

                val request = requests[pendingItem.chapterId] ?: return@launch
                pendingItem.chapterId to request
            }

            // Call launchDownloadJob *outside* the lock to avoid re-entrant deadlock.
            launchDownloadJob(toStart.first, toStart.second)
        }
    }

    /**
     * Launches a download job for the given chapter. Must be called outside [mutex] lock.
     */
    @Suppress("LongMethod", "CognitiveComplexMethod")
    private suspend fun launchDownloadJob(chapterId: Long, request: ChapterDownloadRequest) {
        mutex.withLock {
            // Double-check we haven't exceeded the limit
            if (jobs.size >= maxConcurrentDownloads) return@withLock

            updateStatus(chapterId, DownloadStatus.DOWNLOADING)

            jobs[chapterId] = scope.launch {
                // Track whether actual page downloads were attempted.  When pageUrls is empty
                // the item is re-queued and waits for page URLs to be resolved; in that case
                // processPendingQueue must NOT be called to avoid an infinite retry loop.
                var downloadedPages = false
                try {
                    val pageUrls = request.pageUrls
                    val totalPages = pageUrls.size

                    if (totalPages == 0) {
                        // Pages not yet resolved – park the item back as QUEUED and stop.
                        mutex.withLock { updateStatus(chapterId, DownloadStatus.QUEUED) }
                        return@launch
                    }

                    downloadedPages = true
                    val packAsCbz = downloadPreferences.saveAsCbz.first()

                    pageUrls.forEachIndexed { index, url ->
                        if (!isActive) return@launch

                        val destFile = DownloadProvider.getPageFile(
                            context,
                            request.sourceName,
                            request.mangaTitle,
                            request.chapterTitle,
                            index
                        )

                        if (!destFile.exists() || destFile.length() == 0L) {
                            destFile.delete()
                            val result = downloader.downloadPage(url, destFile)
                            if (result.isFailure) {
                                destFile.delete()
                                mutex.withLock { updateStatus(chapterId, DownloadStatus.FAILED) }
                                return@launch
                            }
                        }

                        mutex.withLock { updateProgress(chapterId, ((index + 1) * 100) / totalPages) }
                    }

                    if (isActive) {
                        if (packAsCbz) {
                            val chapterDir = DownloadProvider.getChapterDir(
                                context,
                                request.sourceName,
                                request.mangaTitle,
                                request.chapterTitle
                            )
                            CbzCreator.createCbz(chapterDir).onSuccess {
                                chapterDir.listFiles()
                                    ?.filter { file ->
                                        file.isFile &&
                                            file.extension.lowercase() in DownloadProvider.PAGE_EXTENSIONS
                                    }
                                    ?.forEach { it.delete() }
                            }
                        }
                        mutex.withLock { updateStatus(chapterId, DownloadStatus.COMPLETED) }
                    }
                } finally {
                    val finalStatus = mutex.withLock {
                        jobs.remove(chapterId)
                        downloadMap[chapterId]?.status
                    }
                    when (finalStatus) {
                        DownloadStatus.COMPLETED -> downloadQueueDao.delete(chapterId)
                        DownloadStatus.FAILED -> downloadQueueDao.updateStatus(chapterId, DownloadStatus.FAILED.name)
                        else -> Unit
                    }
                    // Only look for the next queued item when real download work was done.
                    // Skipping this when pages were empty prevents an infinite re-launch loop.
                    if (downloadedPages) {
                        processPendingQueue()
                    }
                }
            }
        }
    }
}
