package app.otakureader.data.sync.remote

import app.otakureader.core.preferences.SyncPreferences
import app.otakureader.data.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating SelfHostedSyncApi instances with dynamic base URLs.
 * Caches instances to avoid recreating OkHttpClient/Retrofit on every call.
 */
@Singleton
class SelfHostedSyncApiFactory @Inject constructor(
    private val syncPreferences: SyncPreferences
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Cache for Retrofit instances keyed by base URL
    private val apiCache = mutableMapOf<String, SelfHostedSyncApi>()

    // Cached OkHttpClient (shared across all Retrofit instances)
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            // Only enable logging in debug builds
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }
        }.build()
    }

    suspend fun create(): SelfHostedSyncApi {
        val baseUrl = syncPreferences.getSelfHostedServerUrl().takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Server URL not configured")

        // Ensure URL ends with /
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        // Return cached instance if available
        return apiCache.getOrPut(normalizedUrl) {
            Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(SelfHostedSyncApi::class.java)
        }
    }

    /**
     * Creates an API instance with the current URL from preferences,
     * or returns null if URL is not configured.
     */
    suspend fun createOrNull(): SelfHostedSyncApi? {
        return try {
            create()
        } catch (e: IllegalStateException) {
            null
        }
    }

    /**
     * Clears the cache when the server URL changes.
     * Call this when the user updates their server configuration.
     */
    fun clearCache() {
        apiCache.clear()
    }
}
