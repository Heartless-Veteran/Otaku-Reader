package app.otakureader.data.repository

import app.otakureader.core.database.dao.RecommendationDao
import app.otakureader.core.database.entity.RecommendationEntity
import app.otakureader.domain.model.MangaRecommendation
import app.otakureader.domain.model.RecommendationInput
import app.otakureader.domain.model.RecommendationResult
import app.otakureader.domain.model.RecommendationType
import app.otakureader.domain.model.UserReadingPattern
import app.otakureader.domain.repository.AiRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [RecommendationRepository] using Gemini AI.
 *
 * This repository:
 * 1. Analyzes user's reading patterns from library and history
 * 2. Calls Gemini AI to generate personalized recommendations
 * 3. Caches results locally for offline access
 * 4. Provides fresh recommendations on demand
 */
@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val aiRepository: AiRepository,
    private val mangaRepository: MangaRepository,
    private val recommendationDao: RecommendationDao,
    private val settings: app.otakureader.core.preferences.AiPreferences
) : RecommendationRepository {

    private val _recommendationsFlow = MutableStateFlow<RecommendationResult?>(null)
    private var lastRefreshTime: Long? = null

    companion object {
        private const val CACHE_DURATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
        private const val MIN_LIBRARY_SIZE_FOR_AI = 3
        private const val MAX_RECOMMENDATIONS = 10
    }

    override suspend fun getRecommendations(forceRefresh: Boolean): Result<RecommendationResult> {
        // Check cache first if not forcing refresh
        if (!forceRefresh) {
            val cached = getCachedRecommendations()
            if (cached != null) {
                _recommendationsFlow.value = cached
                return Result.success(cached)
            }
        }

        // Check if AI is available
        if (!aiRepository.isAvailable()) {
            return Result.failure(IllegalStateException("AI service not available. Please configure API key."))
        }

        return try {
            // Analyze reading patterns
            val patternResult = analyzeReadingPatterns()
            if (patternResult.isFailure) {
                return Result.failure(patternResult.exceptionOrNull()!!)
            }
            val pattern = patternResult.getOrNull()!!

            // Build recommendation input
            val input = buildRecommendationInput(pattern)

            // Generate recommendations via AI
            val result = generateRecommendations(input).getOrThrow()

            // Cache and emit
            cacheRecommendations(result)
            lastRefreshTime = System.currentTimeMillis()
            _recommendationsFlow.value = result

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeRecommendations(): Flow<RecommendationResult?> {
        return _recommendationsFlow.asStateFlow()
    }

    override suspend fun analyzeReadingPatterns(): Result<UserReadingPattern> {
        return try {
            // Get user's library
            val libraryManga = mangaRepository.getLibraryManga().first()

            if (libraryManga.size < MIN_LIBRARY_SIZE_FOR_AI) {
                return Result.failure(
                    IllegalStateException(
                        "Need at least $MIN_LIBRARY_SIZE_FOR_AI manga in library for recommendations. " +
                        "Current: ${libraryManga.size}"
                    )
                )
            }

            // Analyze genres
            val genreCounts = mutableMapOf<String, Int>()
            val authorCounts = mutableMapOf<String, Int>()
            var totalChapters = 0
            var completedCount = 0

            libraryManga.forEach { manga ->
                // Count genres
                manga.genre.forEach { genre ->
                    genreCounts[genre] = genreCounts.getOrDefault(genre, 0) + 1
                }

                // Count authors
                manga.author?.let { author ->
                    authorCounts[author] = authorCounts.getOrDefault(author, 0) + 1
                }

                totalChapters += manga.totalChapters
                if (manga.status == app.otakureader.domain.model.MangaStatus.COMPLETED) {
                    completedCount++
                }
            }

            // Sort and take top genres/authors
            val favoriteGenres = genreCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { (genre, count) ->
                    app.otakureader.domain.model.GenrePreference(
                        genre = genre,
                        frequency = count,
                        averageTimeSpentMs = 0L, // Would need reading time tracking
                        completionRate = completedCount.toFloat() / libraryManga.size
                    )
                }

            val favoriteAuthors = authorCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { (author, count) ->
                    app.otakureader.domain.model.AuthorPreference(
                        author = author,
                        mangaCount = count,
                        totalTimeSpentMs = 0L
                    )
                }

            val pattern = UserReadingPattern(
                favoriteGenres = favoriteGenres,
                favoriteAuthors = favoriteAuthors,
                preferredStatus = listOf(app.otakureader.domain.model.MangaStatus.COMPLETED),
                averageReadingTimeMs = 0L,
                preferredChapterCountMin = null,
                preferredChapterCountMax = null,
                commonThemes = emptyList(),
                readingVelocity = app.otakureader.domain.model.ReadingVelocity.MODERATE,
                favoriteTropes = emptyList()
            )

            Result.success(pattern)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateRecommendations(input: RecommendationInput): Result<RecommendationResult> {
        return try {
            // Build prompt for Gemini
            val prompt = buildGeminiPrompt(input)

            // Call AI
            val aiResult = aiRepository.generateContent(prompt)
            if (aiResult.isFailure) {
                return Result.failure(aiResult.exceptionOrNull()!!)
            }

            val aiResponse = aiResult.getOrNull()!!

            // Parse AI response into recommendations
            val recommendations = parseAiRecommendations(aiResponse, input)

            val result = RecommendationResult(
                recommendations = recommendations,
                pattern = null, // Would need to store pattern separately
                refreshedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + CACHE_DURATION_MS,
                isCached = false
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearCache() {
        recommendationDao.deleteAllRecommendations()
        lastRefreshTime = null
        _recommendationsFlow.value = null
    }

    override suspend fun getLastRefreshTime(): Long? {
        return lastRefreshTime ?: recommendationDao.getLastSuccessfulRefresh()?.timestamp
    }

    override suspend fun needsRefresh(): Boolean {
        val lastRefresh = getLastRefreshTime() ?: return true
        return System.currentTimeMillis() - lastRefresh > CACHE_DURATION_MS
    }

    override suspend fun refreshIfNeeded(): Result<RecommendationResult> {
        return if (needsRefresh()) {
            getRecommendations(forceRefresh = true)
        } else {
            getRecommendations(forceRefresh = false)
        }
    }

    override suspend fun markRecommendationViewed(recommendationId: String) {
        recommendationDao.markAsViewed(recommendationId)
    }

    override suspend fun markRecommendationActioned(recommendationId: String, mangaId: Long) {
        recommendationDao.markAsActioned(recommendationId, mangaId)
    }

    override suspend fun dismissRecommendation(recommendationId: String) {
        recommendationDao.dismissRecommendation(recommendationId)
    }

    override suspend fun getSimilarManga(mangaId: Long, limit: Int): Result<List<MangaRecommendation>> {
        val manga = mangaRepository.getMangaById(mangaId) ?: return Result.failure(
            IllegalArgumentException("Manga not found: $mangaId")
        )

        val prompt = """
            Based on this manga, suggest $limit similar manga:
            
            Title: ${manga.title}
            Author: ${manga.author ?: "Unknown"}
            Genres: ${manga.genre.joinToString(", ")}
            Description: ${manga.description ?: "No description"}
            
            Provide $limit recommendations in this exact format:
            
            TITLE: [manga title]
            AUTHOR: [author name]
            GENRES: [genre1, genre2, genre3]
            DESCRIPTION: [brief description]
            REASON: [why it's similar to ${manga.title}]
            
            (Repeat for each recommendation)
            
            Be specific and accurate. Only recommend real manga titles.
        """.trimIndent()

        return try {
            val aiResult = aiRepository.generateContent(prompt)
            if (aiResult.isFailure) {
                return Result.failure(aiResult.exceptionOrNull()!!)
            }

            val recommendations = parseAiRecommendations(aiResult.getOrNull()!!, null)
                .take(limit)

            Result.success(recommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getForYouRecommendations(limit: Int): Result<List<MangaRecommendation>> {
        val result = getRecommendations(forceRefresh = false)
        return result.map { it.recommendations.take(limit) }
    }

    // Private helper methods

    private suspend fun getCachedRecommendations(): RecommendationResult? {
        if (needsRefresh()) return null

        val entities = recommendationDao.getRecommendations()
        if (entities.isEmpty()) return null

        val recommendations = entities.map { it.toDomainModel() }

        return RecommendationResult(
            recommendations = recommendations,
            pattern = null,
            refreshedAt = entities.firstOrNull()?.generatedAt ?: System.currentTimeMillis(),
            expiresAt = entities.firstOrNull()?.generatedAt?.plus(CACHE_DURATION_MS) ?: System.currentTimeMillis(),
            isCached = true
        )
    }

    private suspend fun cacheRecommendations(result: RecommendationResult) {
        // Clear old cache
        recommendationDao.deleteAllRecommendations()

        // Insert new recommendations
        val entities = result.recommendations.map { it.toEntity() }
        recommendationDao.insertRecommendations(entities)
    }

    private fun buildRecommendationInput(pattern: UserReadingPattern): RecommendationInput {
        // This would normally aggregate actual reading history
        // For now, using pattern-derived data
        return RecommendationInput(
            libraryManga = emptyList(), // Would need actual library data with engagement
            readingHistory = emptyList(), // Would need chapter history
            totalReadingTimeMs = 0L
        )
    }

    private fun buildGeminiPrompt(input: RecommendationInput): String {
        val genreList = input.libraryManga.flatMap { it.manga.genre }.distinct().take(10)

        return """
            You are a manga recommendation engine. Analyze the user's preferences and suggest $MAX_RECOMMENDATIONS manga they would enjoy.

            User Profile:
            - Genres in library: ${genreList.joinToString(", ")}
            - Total manga in library: ${input.libraryManga.size}
            - Total reading time: ${input.totalReadingTimeMs / 1000 / 60} minutes
            
            Provide $MAX_RECOMMENDATIONS recommendations in this EXACT format:
            
            ---
            TITLE: [Exact manga title]
            AUTHOR: [Author name or "Unknown"]
            GENRES: [Primary genre, Secondary genre]
            DESCRIPTION: [2-3 sentence description, no spoilers]
            REASON: [Brief explanation why this matches user's taste - be specific about genre/trope connections]
            TYPE: [SIMILAR/DISCOVERY/TRENDING/HIDDEN_GEM]
            ---
            (Repeat for each recommendation)
            
            Guidelines:
            - Mix of popular and lesser-known titles
            - Prioritize genres the user already likes
            - Include one "DISCOVERY" recommendation outside their usual genres
            - Be accurate - only recommend real, published manga
            - Keep descriptions engaging but spoiler-free
            
            Respond ONLY with the formatted recommendations, no other text.
        """.trimIndent()
    }

    private fun parseAiRecommendations(response: String, input: RecommendationInput?): List<MangaRecommendation> {
        val recommendations = mutableListOf<MangaRecommendation>()

        // Split by delimiter
        val sections = response.split("---").filter { it.trim().isNotEmpty() }

        for (section in sections) {
            try {
                val title = extractField(section, "TITLE") ?: continue
                val author = extractField(section, "AUTHOR")
                val genres = extractField(section, "GENRES")?.split(",")?.map { it.trim() } ?: emptyList()
                val description = extractField(section, "DESCRIPTION")
                val reason = extractField(section, "REASON") ?: "Recommended based on your reading history"
                val typeStr = extractField(section, "TYPE") ?: "SIMILAR"

                val type = try {
                    RecommendationType.valueOf(typeStr.uppercase())
                } catch (e: IllegalArgumentException) {
                    RecommendationType.SIMILAR
                }

                val recommendation = MangaRecommendation(
                    mangaId = null, // Not in local DB yet
                    title = title,
                    author = author,
                    thumbnailUrl = null, // Would need to fetch from source
                    description = description,
                    genres = genres,
                    sourceId = "", // Would need source lookup
                    sourceUrl = "",
                    reasonExplanation = reason,
                    confidenceScore = 0.8f,
                    basedOnMangaIds = input?.libraryManga?.map { it.manga.id } ?: emptyList(),
                    basedOnGenres = genres,
                    recommendationType = type,
                    generatedAt = System.currentTimeMillis()
                )

                recommendations.add(recommendation)
            } catch (e: Exception) {
                // Skip malformed entries
                continue
            }
        }

        return recommendations
    }

    private fun extractField(text: String, fieldName: String): String? {
        val regex = Regex("$fieldName:\\s*(.+?)(?=\\n[A-Z]+:|\\z)", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.trim()
    }

    // Extension functions for conversion

    private fun RecommendationEntity.toDomainModel(): MangaRecommendation {
        return MangaRecommendation(
            mangaId = mangaId,
            title = title,
            author = author,
            thumbnailUrl = thumbnailUrl,
            description = description,
            genres = genres.split(",").filter { it.isNotBlank() },
            sourceId = sourceId,
            sourceUrl = sourceUrl,
            reasonExplanation = reasonExplanation,
            confidenceScore = confidenceScore,
            basedOnMangaIds = basedOnMangaIds.split(",").mapNotNull { it.toLongOrNull() },
            basedOnGenres = basedOnGenres.split(",").filter { it.isNotBlank() },
            recommendationType = RecommendationType.valueOf(recommendationType),
            generatedAt = generatedAt
        )
    }

    private fun MangaRecommendation.toEntity(): RecommendationEntity {
        return RecommendationEntity(
            id = UUID.randomUUID().toString(),
            mangaId = mangaId,
            title = title,
            author = author,
            thumbnailUrl = thumbnailUrl,
            description = description,
            genres = genres.joinToString(","),
            sourceId = sourceId,
            sourceUrl = sourceUrl,
            reasonExplanation = reasonExplanation,
            confidenceScore = confidenceScore,
            basedOnMangaIds = basedOnMangaIds.joinToString(","),
            basedOnGenres = basedOnGenres.joinToString(","),
            recommendationType = recommendationType.name,
            viewed = false,
            actioned = false,
            dismissed = false,
            generatedAt = generatedAt,
            expiresAt = generatedAt + CACHE_DURATION_MS
        )
    }
}
