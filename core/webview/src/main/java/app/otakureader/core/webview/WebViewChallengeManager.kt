package app.otakureader.core.webview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.CookieJar
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mediates WebView challenge requests between extension sources and the app's navigation layer.
 *
 * Usage flow:
 * 1. An extension source detects a Cloudflare/CAPTCHA barrier and calls [requestChallenge].
 * 2. This class emits a [ChallengeRequest] on [pendingChallenge] and suspends.
 * 3. The app's navigation layer observes [pendingChallenge] and navigates to the WebView screen.
 * 4. When the user finishes and closes the WebView, the nav layer calls [completeChallenge].
 * 5. [requestChallenge] resumes and returns `true` if cookies were obtained, `false` if cancelled.
 */
@Singleton
class WebViewChallengeManager @Inject constructor(
    private val cookieBridge: WebViewCookieBridge,
) {

    data class ChallengeRequest(val sourceId: Long, val url: String)

    private val _pendingChallenge = MutableSharedFlow<ChallengeRequest>(extraBufferCapacity = 1)

    /** Emits whenever a source requests a WebView challenge. Observe in the navigation host. */
    val pendingChallenge: SharedFlow<ChallengeRequest> = _pendingChallenge.asSharedFlow()

    private val pendingCompletions = ConcurrentHashMap<Long, CompletableDeferred<Boolean>>()

    /**
     * Requests a WebView challenge for [url] on behalf of source [sourceId].
     * Suspends until [completeChallenge] is called with a matching [sourceId].
     *
     * @return `true` if the challenge completed with cookies, `false` if the user cancelled.
     */
    suspend fun requestChallenge(sourceId: Long, url: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        // Complete any existing deferred for this source so it doesn't hang indefinitely.
        // Use remove(key, value) in finally so we only remove OUR deferred, not a subsequent one.
        pendingCompletions.put(sourceId, deferred)?.complete(false)
        _pendingChallenge.emit(ChallengeRequest(sourceId, url))
        return try {
            deferred.await()
        } finally {
            pendingCompletions.remove(sourceId, deferred)
        }
    }

    /**
     * Signals that the WebView challenge for source [sourceId] is done.
     *
     * If [cookieJar] is provided and [cookieString] is non-empty, the cookies are
     * synced from Android's [android.webkit.CookieManager] into OkHttp so subsequent
     * requests from the same source carry the freshly acquired session cookies.
     *
     * @param sourceId   The source that initiated the challenge.
     * @param url        The URL that was opened in the WebView.
     * @param cookieString Raw cookie string from [android.webkit.CookieManager]; null if none.
     * @param cookieJar  Optional OkHttp jar to receive the synced cookies.
     */
    fun completeChallenge(
        sourceId: Long,
        url: String,
        cookieString: String?,
        cookieJar: CookieJar? = null,
    ) {
        if (cookieJar != null && !cookieString.isNullOrEmpty()) {
            cookieBridge.syncCookiesToOkHttp(url, cookieJar)
        }
        val success = !cookieString.isNullOrEmpty()
        pendingCompletions[sourceId]?.complete(success)
    }
}
