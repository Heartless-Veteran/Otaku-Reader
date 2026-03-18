package app.otakureader.domain.usecase

import app.otakureader.domain.model.MangaRecommendation
import app.otakureader.domain.model.RecommendationResult
import app.otakureader.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting personalized manga recommendations.
 */
class GetRecommendationsUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository
) {
    /**
     * Get recommendations for the current user.
     *
     * @param forceRefresh If true, bypass cache and fetch fresh recommendations
     * @return Result containing recommendations or error
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<RecommendationResult> {
        return recommendationRepository.getRecommendations(forceRefresh)
    }
}

/**
 * Use case for observing recommendations as a Flow.
 */
class ObserveRecommendationsUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository
) {
    /**
     * Observe recommendations as a Flow.
     */
    operator fun invoke(): Flow<RecommendationResult?> {
        return recommendationRepository.observeRecommendations()
    }
}

/**
 * Use case for getting the "For You" section recommendations.
 */
class GetForYouRecommendationsUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository
) {
    /**
     * Get recommendations for the "For You" section.
     *
     * @param limit Maximum number of recommendations
     * @return Result containing recommendations or error
     */
    suspend operator fun invoke(limit: Int = 10): Result<List<MangaRecommendation>> {
        return recommendationRepository.getForYouRecommendations(limit)
    }
}

/**
 * Use case for refreshing recommendations.
 */
class RefreshRecommendationsUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository
) {
    /**
     * Force refresh recommendations from AI.
     *
     * @return Result containing fresh recommendations or error
     */
    suspend operator fun invoke(): Result<RecommendationResult> {
        return recommendationRepository.getRecommendations(forceRefresh = true)
    }
}

/**
 * Use case for dismissing a recommendation.
 */
class DismissRecommendationUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository
) {
    /**
     * Dismiss a recommendation.
     *
     * @param recommendationId The ID of the recommendation to dismiss
     */
    suspend operator fun invoke(recommendationId: String) {
        recommendationRepository.dismissRecommendation(recommendationId)
    }
}

/**
 * Use case for getting similar manga recommendations.
 */
class GetSimilarMangaUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository
) {
    /**
     * Get manga similar to the specified manga.
     *
     * @param mangaId The manga ID to find similar titles for
     * @param limit Maximum number of similar manga
     * @return Result containing similar manga or error
     */
    suspend operator fun invoke(mangaId: Long, limit: Int = 5): Result<List<MangaRecommendation>> {
        return recommendationRepository.getSimilarManga(mangaId, limit)
    }
}

/**
 * Use case for checking if recommendations need a refresh.
 */
class CheckRecommendationsRefreshNeededUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository
) {
    /**
     * Check if recommendations cache has expired and needs refresh.
     *
     * @return true if refresh is needed
     */
    suspend operator fun invoke(): Boolean {
        return recommendationRepository.needsRefresh()
    }
}
