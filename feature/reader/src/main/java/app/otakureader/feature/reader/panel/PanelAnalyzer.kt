package app.otakureader.feature.reader.panel

import android.content.Context
import app.otakureader.feature.reader.model.PanelAnalysisException
import app.otakureader.feature.reader.model.PanelAnalysisRequest
import app.otakureader.feature.reader.model.PanelAnalysisResultWrapper
import app.otakureader.domain.model.ReadingDirection
import coil3.ImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op implementation of PanelAnalyzer.
 *
 * Panel-aware reading requires the Gemini Vision API. The Gemini-backed
 * implementation lives in a separate companion repo (see #708); this stub
 * keeps the reader feature compiling and gracefully returns "not available"
 * errors for all panel analysis requests.
 */
@Singleton
class PanelAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val cacheService: PanelCacheService
) {
    /**
     * No-op initialization — the Gemini client is not available.
     */
    fun initialize(apiKey: String) {
        // No-op: Gemini client is not available in this build (see #708).
    }

    /**
     * Always returns false — AI is not available.
     */
    fun isInitialized(): Boolean = false

    /**
     * Always returns error indicating AI is not available.
     */
    suspend fun analyzePage(
        request: PanelAnalysisRequest,
        useCache: Boolean = true,
        timeoutMillis: Long = 45_000L
    ): PanelAnalysisResultWrapper {
        return PanelAnalysisResultWrapper.Error(
            PanelAnalysisException.NotAvailable()
        )
    }

    /**
     * Simplified API - always returns error.
     */
    suspend fun analyzePage(
        imageUrl: String,
        readingDirection: ReadingDirection = ReadingDirection.RTL
    ): PanelAnalysisResultWrapper {
        return PanelAnalysisResultWrapper.Error(
            PanelAnalysisException.NotAvailable()
        )
    }
}
