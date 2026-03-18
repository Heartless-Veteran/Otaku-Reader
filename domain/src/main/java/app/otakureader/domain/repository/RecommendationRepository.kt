package app.otakureader.domain.repository

import app.otakureader.domain.model.MangaRecommendation
import app.otakureader.domain.model.RecommendationInput
import app.otakureader.domain.model.RecommendationResult
import app.otakureader.domain.model.UserReadingPattern
import kotlinx.coroutines.flow.Flow

/**
 * Repository for AI-powered manga recommendations.
 *
 * This repository provides personalized manga recommendations based on the user's
 * reading history, library, and engagement patterns.
 */
interface RecommendationRepository {

    /**
     * Get personalized recommendations for the current user.
     *
     * This method will:
     * 1. Check for cached recommendations that are still valid
     * 2. If no valid cache exists, analyze reading patterns and generate new recommendations
     * 3. Store new recommendations in cache
     *
     * @param forceRefresh If true, ignore cache and generate fresh recommendations
     * @return Result containing recommendations or error
     */
    suspend fun getRecommendations(forceRefresh: Boolean = false): Result<RecommendationResult>

    /**
     * Observe recommendations as a Flow.
     *
     * Emits whenever recommendations change (initial load, cache updates, etc.)
     */
    fun observeRecommendations(): Flow<RecommendationResult?>

    /**
     * Analyze user's reading patterns from their history.
     *
     * @return User reading pattern analysis
     */
    suspend fun analyzeReadingPatterns(): Result<UserReadingPattern>

    /**
     * Generate recommendations based on provided input data.
     *
     * @param input The recommendation input data
     * @return Generated recommendations
     */
    suspend fun generateRecommendations(input: RecommendationInput): Result<RecommendationResult>

    /**
     * Clear the recommendation cache.
     */
    suspend fun clearCache()

    /**
     * Get the timestamp of the last successful recommendation refresh.
     *
     * @return Timestamp in milliseconds, or null if never refreshed
     */
    suspend fun getLastRefreshTime(): Long?

    /**
     * Check if recommendations need to be refreshed.
     *
     * @return true if cache is expired or empty
     */
    suspend fun needsRefresh(): Boolean

    /**
     * Refresh recommendations if needed (cache expired).
     *
     * @return Result of the refresh operation
     */
    suspend fun refreshIfNeeded(): Result<RecommendationResult>

    /**
     * Mark a recommendation as viewed by the user.
     *
     * @param recommendationId The ID of the viewed recommendation
     */
    suspend fun markRecommendationViewed(recommendationId: String)

    /**
     * Mark a recommendation as acted upon (e.g., manga added to library).
     *
     * @param recommendationId The ID of the recommendation
     * @param mangaId The ID of the manga that was added
     */
    suspend fun markRecommendationActioned(recommendationId: String, mangaId: Long)

    /**
     * Dismiss a recommendation so it won't appear again.
     *
     * @param recommendationId The ID of the recommendation to dismiss
     */
    suspend fun dismissRecommendation(recommendationId: String)

    /**
     * Get similar manga recommendations for a specific manga.
     *
     * @param mangaId The manga ID to find similar titles for
     * @param limit Maximum number of recommendations to return
     * @return Similar manga recommendations
     */
    suspend fun getSimilarManga(mangaId: Long, limit: Int = 5): Result<List<MangaRecommendation>>

    /**
     * Get "For You" recommendations for display in the library/browse section.
     *
     * @param limit Maximum number of recommendations to return
     * @return Curated recommendations for the "For You" section
     */
    suspend fun getForYouRecommendations(limit: Int = 10): Result<List<MangaRecommendation>>
}
