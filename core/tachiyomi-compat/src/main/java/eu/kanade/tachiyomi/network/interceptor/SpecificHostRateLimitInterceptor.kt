package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toDuration
import kotlin.time.toDurationUnit

/**
 * An OkHttp interceptor that handles given url host's rate limiting.
 *
 * This uses Java Time APIs and is the legacy method, kept
 * for compatibility reasons with existing extensions.
 *
 * @since extension-lib 1.3
 */
@Deprecated("Use the version with kotlin.time APIs instead.")
fun OkHttpClient.Builder.rateLimitHost(
    httpUrl: HttpUrl,
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) = addInterceptor(
    RateLimitInterceptor(httpUrl.host, permits, period.toDuration(unit.toDurationUnit())),
)

/**
 * An OkHttp interceptor that handles given url host's rate limiting.
 *
 * @since extension-lib 1.5
 */
fun OkHttpClient.Builder.rateLimitHost(
    httpUrl: HttpUrl,
    permits: Int,
    period: Duration = 1.seconds,
) = addInterceptor(RateLimitInterceptor(httpUrl.host, permits, period))

/**
 * An OkHttp interceptor that handles given url host's rate limiting.
 *
 * @since extension-lib 1.5
 */
@Suppress("UNUSED")
fun OkHttpClient.Builder.rateLimitHost(url: String, permits: Int, period: Duration = 1.seconds) =
    addInterceptor(RateLimitInterceptor(url.toHttpUrlOrNull()?.host, permits, period))
