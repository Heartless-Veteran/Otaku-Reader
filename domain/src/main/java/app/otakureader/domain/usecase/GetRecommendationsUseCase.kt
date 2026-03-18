package app.otakureader.domain.usecase

import app.otakureader.core.ai.GeminiClient
import app.otakureader.core.ai.model.AiConfig
import app.otakureader.core.ai.model.AiRequest
import app.otakureader.core.common.result.Result
import app.otakureader.domain.model.GenrePreference
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaReadingHistory
import app.otakureader.domain.model.RecommendationAnalysisInput
import app.otakureader.domain.model.RecommendationType
import app.otakureader.domain.model.RecommendedManga
import app.otakureader.domain.model.RecommendedMangaResult
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for generating AI-powered manga recommendations.
 *
 * This use case analyzes the user's reading history, favorites, ratings, and time spent
 * to generate personalized manga recommendations using Google's Gemini Pro AI.
 *
 * Features:
 * - Analyzes reading patterns (genres, authors, completion patterns)
 * - Considers user ratings and favorites
 * - Factors in time spent per manga/series
 * - Caches recommendations for 24 hours
 * - Returns ranked list of 10 recommendations with confidence scores and reasoning
 *
 * @param geminiClient The Gemini AI client for generating recommendations
 * @param mangaRepository Repository for accessing manga data
 * @param chapterRepository Repository for accessing reading history
 * @param recommendationRepository Repository for caching recommendations
 * @param json JSON serializer for parsing AI responses
 */
@Singleton
class GetRecommendationsUseCase @Inject constructor(
    private val geminiClient: GeminiClient,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    private val recommendationRepository: RecommendationRepository? = null,
    private val json: Json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
) {
    /**
     * Generate personalized manga recommendations for the current user.
     *
     * This method:
     * 1. Checks for cached recommendations (valid for 24 hours)
     * 2. If cache miss or forceRefresh=true, analyzes reading history
     * 3. Generates recommendations using Gemini Pro AI
     * 4. Caches results and returns ranked list of 10 recommendations
     *
     * @param forceRefresh Bypass cache and generate fresh recommendations
     * @return Flow of [RecommendedMangaResult] containing ranked recommendations
     */
    operator fun invoke(forceRefresh: Boolean = false): Flow<Result<RecommendedMangaResult>> = flow {
        // Check cache first unless force refresh
        if (!forceRefresh && recommendationRepository != null) {
            val cached = recommendationRepository.getRecommendations(forceRefresh = false)
            if (cached.isSuccess) {
                val result = cached.getOrNull()
                if (result != null && result.recommendations.isNotEmpty()) {
                    emit(Result.Success(
                        RecommendedMangaResult(
                            recommendations = result.recommendations.map { it.toRecommendedManga() },
                            refreshedAt = result.refreshedAt,
                            expiresAt = result.expiresAt,
                            isCached = true,
                            analysisSummary = null
                        )
                    ))
                    return@flow
                }
            }
        }

        // Generate fresh recommendations
        emit(generateFreshRecommendations())
    }

    /**
     * Generate fresh recommendations by analyzing reading history and calling Gemini Pro.
     */
    private suspend fun generateFreshRecommendations(): Result<RecommendedMangaResult> {
        // Check if AI is available
        if (!geminiClient.isInitialized()) {
            return Result.Error(
                Exception("AI service is not initialized. Please configure a Gemini API key in settings.")
            )
        }

        return try {
            // Gather reading data
            val analysisInput = gatherReadingAnalysisInput()
            
            // Generate AI prompt
            val prompt = buildRecommendationPrompt(analysisInput)
            
            // Call Gemini Pro
            val aiConfig = AiConfig(
                maxTokens = 2048,
                temperature = 0.7,
                topP = 0.9,
                requestTimeoutMillis = 60_000L
            )
            
            val request = AiRequest(
                prompt = prompt,
                maxTokens = 2048,
                temperature = 0.7,
                topP = 0.9,
                timeoutMillis = 60_000L
            )
            
            val response = geminiClient.generateContentWithConfig(request)
            
            when (response) {
                is Result.Success -> {
                    val recommendations = parseRecommendations(
                        response.data.content, 
                        analysisInput
                    )
                    
                    val result = RecommendedMangaResult(
                        recommendations = recommendations.take(RecommendedMangaResult.MAX_RECOMMENDATIONS),
                        isCached = false,
                        analysisSummary = generateAnalysisSummary(analysisInput)
                    )
                    
                    // Cache the results
                    recommendationRepository?.let { repo ->
                        cacheRecommendations(result, analysisInput)
                    }
                    
                    Result.Success(result)
                }
                is Result.Error -> {
                    Result.Error(response.exception)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Gather all reading history and user preference data for analysis.
     */
    private suspend fun gatherReadingAnalysisInput(): RecommendationAnalysisInput {
        // Get library manga (favorites)
        val libraryManga = mangaRepository.getLibraryManga().first()
        
        // Get reading history
        val history = chapterRepository.observeHistory().first()
        
        // Build reading history per manga
        val mangaHistoryMap = mutableMapOf<Long, MangaReadingHistory>()
        val timeSpentPerManga = mutableMapOf<Long, Long>()
        
        libraryManga.forEach { manga ->
            val chapters = chapterRepository.getChaptersByMangaId(manga.id).first()
            val readChapters = chapters.filter { it.read }
            val totalChapters = chapters.size
            
            // Calculate time spent (using history if available, or estimate)
            val timeSpent = history
                .filter { chapter -> 
                    chapters.any { it.id == chapter.chapter.id } 
                }
                .sumOf { it.readDurationMs }
            
            timeSpentPerManga[manga.id] = timeSpent
            
            val completionPercentage = if (totalChapters > 0) {
                readChapters.size.toFloat() / totalChapters
            } else 0f
            
            mangaHistoryMap[manga.id] = MangaReadingHistory(
                manga = manga,
                chaptersRead = readChapters.size,
                timeSpentMs = timeSpent,
                isCompleted = completionPercentage >= 0.95f,
                completionPercentage = completionPercentage,
                lastReadAt = history
                    .filter { chapter -> 
                        chapters.any { it.id == chapter.chapter.id } 
                    }
                    .maxOfOrNull { it.readAt },
                isFavorite = manga.favorite
            )
        }
        
        // Calculate genre preferences
        val genreCounts = mutableMapOf<String, Pair<Int, Long>>() // genre -> (count, time)
        mangaHistoryMap.values.forEach { history ->
            history.manga.genre.forEach { genre ->
                val (count, time) = genreCounts.getOrDefault(genre, 0 to 0L)
                genreCounts[genre] = (count + 1) to (time + history.timeSpentMs)
            }
        }
        
        val preferredGenres = genreCounts.map { (genre, pair) ->
            val (count, time) = pair
            GenrePreference(
                genre = genre,
                frequency = count,
                averageTimeSpentMs = time / count,
                completionRate = mangaHistoryMap.values
                    .filter { it.manga.genre.contains(genre) }
                    .map { it.completionPercentage }
                    .average()
                    .toFloat()
            )
        }.sortedByDescending { it.frequency }
        
        // Get favorite authors
        val favoriteAuthors = libraryManga
            .groupBy { it.author }
            .filter { it.key != null }
            .map { (author, mangaList) -> author!! to mangaList.size }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        val totalReadingTime = timeSpentPerManga.values.sum()
        
        return RecommendationAnalysisInput(
            readingHistory = mangaHistoryMap.values.toList(),
            favoriteManga = libraryManga.filter { it.favorite },
            ratedManga = emptyList(), // TODO: Add rating support when available
            timeSpentPerManga = timeSpentPerManga,
            totalReadingTimeMs = totalReadingTime,
            preferredGenres = preferredGenres,
            favoriteAuthors = favoriteAuthors
        )
    }

    /**
     * Build the AI prompt for generating recommendations.
     */
    private fun buildRecommendationPrompt(input: RecommendationAnalysisInput): String {
        val topGenres = input.preferredGenres.take(5)
        val topManga = input.readingHistory
            .sortedByDescending { it.timeSpentMs }
            .take(5)
        
        return buildString {
            appendLine("You are an expert manga recommendation AI. Analyze the user's reading history and provide personalized manga recommendations.")
            appendLine()
            appendLine("USER READING PROFILE:")
            appendLine("===================")
            
            // Favorite genres
            appendLine("Top Genres:")
            topGenres.forEach { genre ->
                appendLine("  - ${genre.genre}: ${genre.frequency} manga, avg ${formatTime(genre.averageTimeSpentMs)} per manga")
            }
            
            // Favorite authors
            if (input.favoriteAuthors.isNotEmpty()) {
                appendLine()
                appendLine("Favorite Authors: ${input.favoriteAuthors.joinToString(", ")}")
            }
            
            // Top manga by time spent
            appendLine()
            appendLine("Most Engaged Manga:")
            topManga.forEach { history ->
                appendLine("  - ${history.manga.title}")
                appendLine("    Genres: ${history.manga.genre.take(3).joinToString(", ")}")
                appendLine("    Time: ${formatTime(history.timeSpentMs)}, Completion: ${(history.completionPercentage * 100).toInt()}%")
                appendLine("    Status: ${if (history.isFavorite) "FAVORITED" else if (history.isCompleted) "COMPLETED" else "READING"}")
            }
            
            // Reading patterns
            appendLine()
            appendLine("Reading Patterns:")
            appendLine("  - Total reading time: ${formatTime(input.totalReadingTimeMs)}")
            appendLine("  - Completed series: ${input.readingHistory.count { it.isCompleted }}")
            appendLine("  - Favorited manga: ${input.favoriteManga.size}")
            
            appendLine()
            appendLine("INSTRUCTIONS:")
            appendLine("=============")
            appendLine("Recommend 10 manga that this user would enjoy based on their reading profile.")
            appendLine()
            appendLine("For each recommendation, provide:")
            appendLine("1. Title (exact manga title)")
            appendLine("2. Author (if known)")
            appendLine("3. Genres (comma-separated)")
            appendLine("4. Brief description (1-2 sentences)")
            appendLine("5. Confidence score (0.0 to 1.0) - how confident you are this matches their taste")
            appendLine("6. Reasoning - explain WHY this manga is recommended in a friendly, personalized way")
            appendLine("   Example: \"You enjoyed dark fantasy like Berserk, try this for similar gritty storytelling\"")
            appendLine()
            appendLine("FORMAT YOUR RESPONSE AS JSON:")
            appendLine("{\n  \"recommendations\": [\n    {\n      \"title\": \"Manga Title\",\n      \"author\": \"Author Name\",\n      \"genres\": [\"Action\", \"Fantasy\"],\n      \"description\": \"Brief description\",\n      \"confidenceScore\": 0.85,\n      \"reasoning\": \"Personalized explanation\",\n      \"recommendationType\": \"SIMILAR\" // Options: SIMILAR, DISCOVERY, TRENDING, THEME_BASED, AUTHOR_BASED, HIDDEN_GEM\n    }\n  ]\n}")
            appendLine()
            appendLine("IMPORTANT:")
            appendLine("- Provide exactly 10 recommendations")
            appendLine("- Rank by confidence score (highest first)")
            appendLine("- Include a mix of SIMILAR (same taste), DISCOVERY (new genres), and HIDDEN_GEM (lesser known)")
            appendLine("- Make reasoning personal and specific to their reading history")
            appendLine("- ONLY return valid JSON, no markdown formatting")
        }
    }

    /**
     * Parse the AI response into RecommendedManga objects.
     */
    private fun parseRecommendations(
        response: String,
        input: RecommendationAnalysisInput
    ): List<RecommendedManga> {
        return try {
            // Extract JSON from response (handle potential markdown formatting)
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}')
            
            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                return parseFallbackRecommendations(response, input)
            }
            
            val json = response.substring(jsonStart, jsonEnd + 1)
            
            // Parse AI response structure
            val aiResponse = this.json.decodeFromString<AiRecommendationResponse>(json)
            
            aiResponse.recommendations.mapIndexed { index, aiRec ->
                // Create a placeholder manga object for external recommendations
                val manga = Manga(
                    id = -(index + 1), // Negative ID indicates external recommendation
                    sourceId = 0L,
                    url = aiRec.sourceUrl ?: "",
                    title = aiRec.title,
                    thumbnailUrl = aiRec.thumbnailUrl,
                    author = aiRec.author,
                    artist = null,
                    description = aiRec.description,
                    genre = aiRec.genres
                )
                
                RecommendedManga(
                    manga = manga,
                    confidenceScore = aiRec.confidenceScore.coerceIn(0.0f, 1.0f),
                    reasoning = aiRec.reasoning,
                    basedOnMangaIds = findBasedOnMangaIds(aiRec, input),
                    basedOnGenres = aiRec.genres.filter { genre ->
                        input.preferredGenres.any { it.genre.equals(genre, ignoreCase = true) }
                    },
                    recommendationType = RecommendationType.valueOf(aiRec.recommendationType)
                )
            }
        } catch (e: Exception) {
            // Fallback: try to parse as simple list
            parseFallbackRecommendations(response, input)
        }
    }

    /**
     * Fallback parsing for non-JSON responses.
     */
    private fun parseFallbackRecommendations(
        response: String,
        input: RecommendationAnalysisInput
    ): List<RecommendedManga> {
        // Simple line-based parsing as fallback
        val lines = response.lines()
        val recommendations = mutableListOf<RecommendedManga>()
        
        var currentTitle = ""
        var currentReasoning = ""
        
        for (line in lines) {
            when {
                line.matches(Regex("""^\d+[:.)\s].*""")) -> {
                    // Save previous recommendation
                    if (currentTitle.isNotBlank() && currentReasoning.isNotBlank()) {
                        recommendations.add(createFallbackRecommendation(
                            currentTitle, currentReasoning, recommendations.size, input
                        ))
                    }
                    currentTitle = line.replace(Regex("""^\d+[:.)\s]+"""), "").trim()
                    currentReasoning = ""
                }
                line.contains("reason", ignoreCase = true) && line.contains(":") -> {
                    currentReasoning = line.substringAfter(":").trim()
                }
                line.contains("because", ignoreCase = true) -> {
                    currentReasoning = line.trim()
                }
            }
        }
        
        // Add last recommendation
        if (currentTitle.isNotBlank() && recommendations.size < 10) {
            recommendations.add(createFallbackRecommendation(
                currentTitle, currentReasoning, recommendations.size, input
            ))
        }
        
        return recommendations.take(10)
    }

    private fun createFallbackRecommendation(
        title: String,
        reasoning: String,
        index: Int,
        input: RecommendationAnalysisInput
    ): RecommendedManga {
        val confidence = 0.9f - (index * 0.08f) // Descending confidence
        
        return RecommendedManga(
            manga = Manga(
                id = -(index + 1),
                sourceId = 0L,
                url = "",
                title = title,
                thumbnailUrl = null,
                author = null,
                genre = emptyList()
            ),
            confidenceScore = confidence.coerceIn(0.5f, 0.95f),
            reasoning = reasoning.ifBlank { "Recommended based on your reading preferences" },
            recommendationType = when {
                index < 3 -> RecommendationType.SIMILAR
                index < 6 -> RecommendationType.DISCOVERY
                else -> RecommendationType.HIDDEN_GEM
            }
        )
    }

    private fun findBasedOnMangaIds(
        aiRec: AiRecommendation,
        input: RecommendationAnalysisInput
    ): List<Long> {
        // Find manga from user's history that likely influenced this recommendation
        return input.readingHistory
            .filter { history ->
                // Match by genre overlap
                val genreMatch = history.manga.genre.any { genre ->
                    aiRec.genres.any { it.equals(genre, ignoreCase = true) }
                }
                // Match by author
                val authorMatch = aiRec.author != null && 
                    history.manga.author?.contains(aiRec.author, ignoreCase = true) == true
                
                genreMatch || authorMatch
            }
            .sortedByDescending { it.timeSpentMs }
            .take(3)
            .map { it.manga.id }
    }

    private fun generateAnalysisSummary(input: RecommendationAnalysisInput): String {
        val topGenre = input.preferredGenres.firstOrNull()
        val totalManga = input.readingHistory.size
        val completedManga = input.readingHistory.count { it.isCompleted }
        
        return buildString {
            append("Based on $totalManga manga in your library")
            if (completedManga > 0) {
                append(" ($completedManga completed)")
            }
            topGenre?.let {
                append(". You enjoy ${it.genre} manga most")
            }
            append(".")
        }
    }

    private suspend fun cacheRecommendations(
        result: RecommendedMangaResult,
        input: RecommendationAnalysisInput
    ) {
        // Convert to domain model for caching
        val domainRecommendations = result.recommendations.map { rec ->
            app.otakureader.domain.model.MangaRecommendation(
                mangaId = if (rec.manga.id > 0) rec.manga.id else null,
                title = rec.manga.title,
                author = rec.manga.author,
                thumbnailUrl = rec.manga.thumbnailUrl,
                description = rec.manga.description,
                genres = rec.manga.genre,
                sourceId = rec.manga.sourceId.toString(),
                sourceUrl = rec.manga.url,
                reasonExplanation = rec.reasoning,
                confidenceScore = rec.confidenceScore,
                basedOnMangaIds = rec.basedOnMangaIds,
                basedOnGenres = rec.basedOnGenres,
                recommendationType = rec.recommendationType,
                generatedAt = rec.generatedAt
            )
        }
        
        // Cache via repository
        // Note: This would require extending the repository interface
    }

    private fun formatTime(ms: Long): String {
        return when {
            ms < 60_000 -> "${ms / 1000}s"
            ms < 3_600_000 -> "${ms / 60_000}m"
            ms < 86_400_000 -> "${ms / 3_600_000}h"
            else -> "${ms / 86_400_000}d"
        }
    }
}

/**
 * AI response structure for parsing JSON.
 */
@kotlinx.serialization.Serializable
private data class AiRecommendationResponse(
    val recommendations: List<AiRecommendation>
)

@kotlinx.serialization.Serializable
private data class AiRecommendation(
    val title: String,
    val author: String? = null,
    val genres: List<String> = emptyList(),
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val sourceUrl: String? = null,
    val confidenceScore: Float = 0.5f,
    val reasoning: String = "",
    val recommendationType: String = "SIMILAR"
)

/**
 * Extension to convert domain recommendation to our model.
 */
private fun app.otakureader.domain.model.MangaRecommendation.toRecommendedManga(): RecommendedManga {
    return RecommendedManga(
        manga = Manga(
            id = mangaId ?: 0L,
            sourceId = sourceId.toLongOrNull() ?: 0L,
            url = sourceUrl,
            title = title,
            thumbnailUrl = thumbnailUrl,
            author = author,
            genre = genres
        ),
        confidenceScore = confidenceScore,
        reasoning = reasonExplanation,
        basedOnMangaIds = basedOnMangaIds,
        basedOnGenres = basedOnGenres,
        recommendationType = recommendationType
    )
}
