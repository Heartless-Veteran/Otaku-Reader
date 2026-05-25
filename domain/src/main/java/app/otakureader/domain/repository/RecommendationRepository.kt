package app.otakureader.domain.repository

import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.Recommendation
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {
    fun getRecommendations(): Flow<List<Recommendation>>
    suspend fun refreshRecommendations(libraryManga: List<Manga>)
    suspend fun dismissRecommendation(mangaId: Long)
}
