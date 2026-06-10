package app.otakureader.core.webview.di

import app.otakureader.core.webview.WebViewCookieBridge
import app.otakureader.core.webview.WebViewChallengeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the WebView subsystem.
 *
 * Both [WebViewCookieBridge] and [WebViewChallengeManager] are annotated with
 * `@Singleton @Inject constructor`, so Hilt can create them automatically without
 * explicit `@Provides` bindings. This module documents their intent and makes the
 * dependency graph explicit for readers and for Hilt's validation.
 */
@Module
@InstallIn(SingletonComponent::class)
object WebViewModule {

    /**
     * Provides the [WebViewCookieBridge] singleton.
     *
     * The bridge copies cookies from Android's WebView [android.webkit.CookieManager]
     * into OkHttp's [okhttp3.CookieJar] after a source completes a WebView challenge.
     */
    @Provides
    @Singleton
    fun provideWebViewCookieBridge(): WebViewCookieBridge = WebViewCookieBridge()

    /**
     * Provides the [WebViewChallengeManager] singleton.
     *
     * Sources suspend on [WebViewChallengeManager.requestChallenge]; the UI observes
     * [WebViewChallengeManager.challengeRequests] and calls
     * [WebViewChallengeManager.completeChallenge] once the user has solved the CAPTCHA
     * or passed the anti-bot check.
     */
    @Provides
    @Singleton
    fun provideWebViewChallengeManager(
        bridge: WebViewCookieBridge,
    ): WebViewChallengeManager = WebViewChallengeManager(bridge)
}
