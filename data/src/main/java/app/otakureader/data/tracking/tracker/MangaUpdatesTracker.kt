package app.otakureader.data.tracking.tracker

import app.otakureader.core.preferences.TrackerTokenStore
import kotlinx.coroutines.CancellationException
import app.otakureader.data.tracking.api.MangaUpdatesApi
import app.otakureader.data.tracking.api.MangaUpdatesListRequest
import app.otakureader.data.tracking.api.MangaUpdatesLoginRequest
import app.otakureader.data.tracking.api.MangaUpdatesSearchRequest
import app.otakureader.data.tracking.api.MangaUpdatesSeriesRef
import app.otakureader.domain.model.TrackEntry
import app.otakureader.domain.model.TrackStatus
import app.otakureader.domain.model.TrackerType
import app.otakureader.domain.tracking.Tracker

/**
 * Tracker implementation for [MangaUpdates (BakaUpdates)](https://www.mangaupdates.com/).
 *
 * Authentication uses session-based login (username + password → session token).
 * The session token and user ID are persisted via [TrackerTokenStore] so they
 * survive app restarts.
 *
 * MangaUpdates list IDs map to internal [TrackStatus] as follows:
 *  - 0 → READING
 *  - 1 → COMPLETED
 *  - 2 → ON_HOLD
 *  - 3 → DROPPED
 *  - 4 → PLAN_TO_READ
 *  - 5 → RE_READING
 */
class MangaUpdatesTracker(
    private val api: MangaUpdatesApi,
    private val tokenStore: TrackerTokenStore,
) : Tracker {

    override val id: Int = TrackerType.MANGA_UPDATES
    override val name: String = "MangaUpdates"

    private var sessionToken: String?
    private var userId: Long?

    init {
        val tokens = tokenStore.getTokens(TrackerType.MANGA_UPDATES)
        sessionToken = tokens?.accessToken
        userId = tokens?.userId
    }

    override val isLoggedIn: Boolean
        get() = sessionToken?.isNotBlank() == true

    override suspend fun login(username: String, password: String): Boolean {
        return try {
            val response = api.login(MangaUpdatesLoginRequest(login = username, password = password))
            val token = response.context?.sessionToken
            if (token != null && token.isNotBlank()) {
                sessionToken = token
                userId = response.context?.uid
                tokenStore.saveTokens(trackerId = id, accessToken = token, userId = userId)
                true
            } else {
                sessionToken = null
                userId = null
                tokenStore.clearTokens(id)
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    override fun logout() {
        sessionToken = null
        userId = null
        tokenStore.clearTokens(id)
    }

    override suspend fun search(query: String): List<TrackEntry> {
        return api.searchSeries(MangaUpdatesSearchRequest(search = query)).results.map { result ->
            TrackEntry(
                remoteId = result.record.seriesId,
                mangaId = 0L,
                trackerId = id,
                title = result.record.title
            )
        }
    }

    override suspend fun find(remoteId: Long): TrackEntry? {
        return try {
            val entry = api.getListEntry(remoteId)
            TrackEntry(
                remoteId = remoteId,
                mangaId = 0L,
                trackerId = id,
                title = entry.series?.title ?: "",
                status = toTrackStatus(entry.listId),
                lastChapterRead = entry.chapter.toFloat(),
                score = entry.score?.toFloat() ?: 0f
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun update(entry: TrackEntry): TrackEntry {
        val listId = toRemoteStatus(entry.status)
        val request = MangaUpdatesListRequest(
            series = MangaUpdatesSeriesRef(seriesId = entry.remoteId),
            listId = listId,
            chapter = entry.lastChapterRead.toInt()
        )
        return try {
            api.addToList(request)
            entry
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            try {
                api.updateListEntry(request)
                entry
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: Exception) {
                entry
            }
        }
    }

    override fun toTrackStatus(remoteStatus: Int): TrackStatus = when (remoteStatus) {
        0 -> TrackStatus.READING
        1 -> TrackStatus.COMPLETED
        2 -> TrackStatus.ON_HOLD
        3 -> TrackStatus.DROPPED
        4 -> TrackStatus.PLAN_TO_READ
        5 -> TrackStatus.RE_READING
        else -> TrackStatus.PLAN_TO_READ
    }

    override fun toRemoteStatus(status: TrackStatus): Int = when (status) {
        TrackStatus.READING -> 0
        TrackStatus.COMPLETED -> 1
        TrackStatus.ON_HOLD -> 2
        TrackStatus.DROPPED -> 3
        TrackStatus.PLAN_TO_READ -> 4
        TrackStatus.RE_READING -> 5
    }
}
