package app.otakureader.core.webview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mediates between extension sources that need a WebView challenge solved and the UI
 * layer that can display a [WebView].
 *
 * ### Flow
 * 1. A source calls [requestChallenge], which emits a [ChallengeRequest] on
 *    [challengeRequests] and suspends until the challenge is resolved.
 * 2. The UI observes [challengeRequests], opens the embedded WebView for the given URL,
 *    and waits for the user to complete the challenge.
 * 3. After the page finishes loading (or the user taps "Done"), the UI calls
 *    [completeChallenge] which resumes the suspended source coroutine with the result.
 * 4. Before calling [completeChallenge], the UI should call
 *    [WebViewCookieBridge.syncCookiesToOkHttp] so that the acquired cookies are
 *    forwarded to OkHttp automatically.
 *
 * All challenge requests are buffered with a capacity of 8 and DROP_OLDEST overflow
 * behaviour. In the extremely unlikely event of simultaneous challenges from different
 * sources, the oldest is dropped to prevent unbounded accumulation.
 */
@Singleton
class WebViewChallengeManager @Inject constructor(
    private val bridge: WebViewCookieBridge,
) {

    private val _challengeRequests = MutableSharedFlow<ChallengeRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Hot flow of pending [ChallengeRequest]s.
     *
     * Collect this in a screen-level composable (or the activity) and open the
     * WebView challenge screen for each emission.
     */
    val challengeRequests: SharedFlow<ChallengeRequest> = _challengeRequests.asSharedFlow()

    /**
     * Requests that a WebView challenge be displayed for [url] on behalf of [sourceId].
     *
     * Suspends until [completeChallenge] is called by the UI with this request.
     *
     * @param sourceId Stable identifier for the extension source requesting the challenge.
     * @param url      The URL that should be opened in the WebView (e.g. a Cloudflare
     *                 challenge page or CAPTCHA page).
     * @return `true` when the challenge was completed successfully, `false` when it was
     *         cancelled or failed.
     */
    suspend fun requestChallenge(sourceId: String, url: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        val request = ChallengeRequest(sourceId = sourceId, url = url, deferred = deferred)
        _challengeRequests.emit(request)
        return deferred.await()
    }

    /**
     * Signals to the suspended [requestChallenge] call that the challenge at [request]
     * has finished.
     *
     * Call this from the UI after [WebViewCookieBridge.syncCookiesToOkHttp] so that
     * the source's coroutine resumes only after cookies are already in OkHttp's jar.
     *
     * @param request The [ChallengeRequest] that was completed.
     * @param cookieJar The OkHttp cookie jar to sync cookies into. Pass `null` to skip
     *                  cookie syncing (e.g. if the user cancelled without loading the page).
     * @param success  Whether the challenge was solved successfully.
     */
    fun completeChallenge(
        request: ChallengeRequest,
        cookieJar: okhttp3.CookieJar?,
        success: Boolean,
    ) {
        if (success && cookieJar != null) {
            bridge.syncCookiesToOkHttp(request.url, cookieJar)
        }
        request.deferred.complete(success)
    }
}

/**
 * Represents a single WebView challenge request emitted by [WebViewChallengeManager].
 *
 * @property sourceId  The extension source that triggered this challenge.
 * @property url       The URL to load in the WebView.
 * @property deferred  Internal deferred; the UI resolves this via
 *                     [WebViewChallengeManager.completeChallenge].
 */
data class ChallengeRequest(
    val sourceId: String,
    val url: String,
    val deferred: CompletableDeferred<Boolean>,
)
