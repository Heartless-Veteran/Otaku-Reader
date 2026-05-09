package app.otakureader.data.tracking

import app.otakureader.domain.tracking.Tracker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central registry for all enabled tracker services.
 *
 * Inject this singleton wherever tracker access is needed, rather than
 * injecting individual trackers directly.
 */
@Singleton
class TrackManager @Inject constructor(
    trackers: Set<@JvmSuppressWildcards Tracker>
) {
    private val registry: Map<Int, Tracker> = trackers.associateBy { it.id }

    /** All registered trackers. */
    val all: List<Tracker> get() = registry.values.toList()

    /** Returns the tracker with the given [id], or `null` if not registered. */
    fun get(id: Int): Tracker? = registry[id]

    /** Returns only the trackers that the user has authenticated with. */
    val loggedIn: List<Tracker>
        get() = all.filter { it.isLoggedIn }

    /**
     * Authenticate a tracker by its string identifier.
     *
     * @param trackerId Tracker string ID (e.g., "anilist", "mal", "kitsu", "shikimori").
     * @param code OAuth authorization code or token.
     * @param codeVerifier PKCE code verifier generated before the browser was opened.
     *   For MAL this is passed as [Tracker.login]'s `username` parameter per the PKCE spec.
     *   Must not be empty for PKCE-based trackers (MAL).
     * @return `true` on success.
     */
    suspend fun login(trackerId: String, code: String, codeVerifier: String): Boolean {
        val tracker = when (trackerId.lowercase()) {
            "anilist" -> get(app.otakureader.domain.model.TrackerType.ANILIST)
            "mal" -> get(app.otakureader.domain.model.TrackerType.MY_ANIME_LIST)
            "kitsu" -> get(app.otakureader.domain.model.TrackerType.KITSU)
            "shikimori" -> get(app.otakureader.domain.model.TrackerType.SHIKIMORI)
            "mangaupdates" -> get(app.otakureader.domain.model.TrackerType.MANGA_UPDATES)
            else -> null
        }
        return tracker?.login(codeVerifier, code) ?: false
    }
}
