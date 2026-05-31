package app.otakureader.data.repository

import app.otakureader.core.database.dao.MangaDao
import app.otakureader.core.database.dao.RecommendationDao
import app.otakureader.core.database.entity.RecommendationEntity
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.Recommendation
import app.otakureader.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val recommendationDao: RecommendationDao,
    private val mangaDao: MangaDao,
) : RecommendationRepository {

    override fun getRecommendations(): Flow<List<Recommendation>> =
        recommendationDao.getRecommendations().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun refreshRecommendations(libraryManga: List<Manga>) {
        if (libraryManga.size < MIN_LIBRARY_SIZE) return

        val profile = buildGenreProfile(libraryManga)
        if (profile.isEmpty()) return

        // Candidates are browsed-but-not-favorited manga from the local manga table —
        // these are sources the user has at least glanced at, which keeps recommendations
        // grounded in their actual extension ecosystem rather than blind catalog crawls.
        val libraryIds = libraryManga.mapTo(mutableSetOf()) { it.id }
        val now = System.currentTimeMillis()

        val scored = mangaDao.getRecommendationCandidates(limit = CANDIDATE_POOL_LIMIT)
            .asSequence()
            .filter { it.id !in libraryIds }
            .mapNotNull { entity ->
                val genres = entity.genre?.split('|', ',', ';')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: return@mapNotNull null
                if (genres.isEmpty()) return@mapNotNull null
                val genreVector = genres.associateWith { 1f }
                val score = cosineSimilarity(profile, genreVector)
                if (score <= 0f) return@mapNotNull null
                RecommendationEntity(
                    mangaId = entity.id,
                    title = entity.title,
                    thumbnailUrl = entity.thumbnailUrl,
                    sourceId = entity.sourceId,
                    genresJson = Json.encodeToString(genres),
                    score = score,
                    lastComputed = now,
                )
            }
            .sortedByDescending { it.score }
            .take(MAX_RECOMMENDATIONS)
            .toList()

        recommendationDao.deleteAll()
        if (scored.isNotEmpty()) recommendationDao.upsertAll(scored)
    }

    override suspend fun dismissRecommendation(mangaId: Long) {
        recommendationDao.deleteById(mangaId)
    }

    private fun buildGenreProfile(manga: List<Manga>): Map<String, Float> {
        val profile = mutableMapOf<String, Float>()
        manga.forEach { m ->
            m.genre.forEach { genre -> profile[genre] = (profile[genre] ?: 0f) + 1f }
        }
        val magnitude = sqrt(profile.values.sumOf { (it * it).toDouble() }.toFloat())
        return if (magnitude == 0f) emptyMap()
        else profile.mapValues { it.value / magnitude }
    }

    private fun cosineSimilarity(a: Map<String, Float>, b: Map<String, Float>): Float {
        var dot = 0f
        var magB = 0f
        b.forEach { (genre, weight) ->
            dot += (a[genre] ?: 0f) * weight
            magB += weight * weight
        }
        val denominator = sqrt(magB)
        return if (denominator == 0f) 0f else dot / denominator
    }

    private fun RecommendationEntity.toDomain() = Recommendation(
        mangaId = mangaId,
        title = title,
        thumbnailUrl = thumbnailUrl,
        sourceId = sourceId,
        score = score,
    )

    companion object {
        private const val MIN_LIBRARY_SIZE = 5
        private const val MAX_RECOMMENDATIONS = 20
        private const val CANDIDATE_POOL_LIMIT = 500
    }
}
