package app.otakureader.domain.usecase

import app.otakureader.domain.model.Recommendation
import app.otakureader.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecommendationsUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
) {
    operator fun invoke(): Flow<List<Recommendation>> =
        recommendationRepository.getRecommendations()
}
