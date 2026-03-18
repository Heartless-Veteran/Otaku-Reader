package app.otakureader.data.sync.remote

import app.otakureader.core.preferences.SyncPreferences
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating SelfHostedSyncApi instances with dynamic base URLs.
 */
@Singleton
class SelfHostedSyncApiFactory @Inject constructor(
    private val syncPreferences: SyncPreferences
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    fun create(): SelfHostedSyncApi {
        val baseUrl = syncPreferences.selfHostedServerUrl.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Server URL not configured")
        
        // Ensure URL ends with /
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SelfHostedSyncApi::class.java)
    }
    
    /**
     * Creates an API instance with the current URL from preferences,
     * or returns null if URL is not configured.
     */
    fun createOrNull(): SelfHostedSyncApi? {
        return try {
            create()
        } catch (e: IllegalStateException) {
            null
        }
    }
}
