package app.otakureader.domain.usecase.search

import app.otakureader.core.ai.GeminiClient
import app.otakureader.core.common.result.Result
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.model.search.CachedSmartSearch
import app.otakureader.domain.model.search.ParsedSearchQuery
import app.otakureader.domain.model.search.SearchIntent
import app.otakureader.domain.repository.SmartSearchCacheRepository
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for converting natural language queries into structured search intents.
 * Uses Gemini AI for NLP processing and caches results locally.
 */
@Singleton
class SmartSearchUseCase @Inject constructor(
    private val geminiClient: GeminiClient,
    private val cacheRepository: SmartSearchCacheRepository
) {
    /**
     * Convert a natural language query to structured search intents.
     *
     * @param query The natural language search query
     * @param skipCache Whether to skip cache and force AI processing
     * @return Result containing the parsed search query or an error
     */
    suspend operator fun invoke(
        query: String,
        skipCache: Boolean = false
    ): Result<ParsedSearchQuery> {
        if (query.isBlank()) {
            return Result.Error(IllegalArgumentException("Query cannot be blank"))
        }

        val queryHash = hashQuery(query)

        // Check cache first
        if (!skipCache) {
            val cached = cacheRepository.getCachedSearch(queryHash).firstOrNull()
            if (cached != null && isCacheValid(cached)) {
                return Result.Success(cached.parsedQuery)
            }
        }

        // Check if Gemini is initialized
        if (!geminiClient.isInitialized()) {
            return Result.Error(SmartSearchException.AiNotInitialized)
        }

        // Process with AI
        return when (val aiResult = processWithAi(query)) {
            is Result.Success -> {
                val parsedQuery = aiResult.data
                // Cache the result
                cacheRepository.cacheSearch(
                    CachedSmartSearch(
                        queryHash = queryHash,
                        originalQuery = query,
                        parsedQuery = parsedQuery
                    )
                )
                Result.Success(parsedQuery)
            }
            is Result.Error -> Result.Error(aiResult.exception)
        }
    }

    /**
     * Get recent cached smart searches.
     */
    suspend fun getRecentSearches(limit: Int = 10): List<CachedSmartSearch> {
        return cacheRepository.getRecentSearches(limit)
    }

    /**
     * Clear all cached searches.
     */
    suspend fun clearCache() {
        cacheRepository.clearAllCache()
    }

    /**
     * Process the query with Gemini AI.
     */
    private suspend fun processWithAi(query: String): Result<ParsedSearchQuery> {
        val prompt = buildPrompt(query)

        return when (val result = geminiClient.generateContent(prompt)) {
            is Result.Success -> {
                try {
                    val parsedQuery = parseAiResponse(query, result.data)
                    Result.Success(parsedQuery)
                } catch (e: Exception) {
                    Result.Error(SmartSearchException.ParsingError(e.message ?: "Failed to parse AI response"))
                }
            }
            is Result.Error -> Result.Error(
                SmartSearchException.AiProcessingError(
                    result.exception.message ?: "AI processing failed"
                )
            )
        }
    }

    /**
     * Build the prompt for Gemini AI.
     */
    private fun buildPrompt(query: String): String {
        return """
            Analyze this manga search query and extract search intents. Return ONLY a JSON object.

            Query: "$query"

            Available intent types:
            - TitleSearch: {"type": "title", "title": "...", "fuzzyMatch": true/false}
            - GenreSearch: {"type": "genre", "genres": ["..."], "matchMode": "ANY" or "ALL"}
            - AuthorSearch: {"type": "author", "author": "...", "includeArtist": true/false}
            - DescriptionSearch: {"type": "description", "keywords": ["..."], "matchMode": "ANY" or "ALL"}
            - MoodSearch: {"type": "mood", "mood": "DARK|LIGHTHEARTED|ACTION_PACKED|ROMANTIC|MYSTERIOUS|COMEDY|DRAMATIC|HORROR|ADVENTURE|SLICE_OF_LIFE|PSYCHOLOGICAL|THRILLING|HEARTWARMING|EPIC|TRAGIC"}
            - StatusSearch: {"type": "status", "status": "ONGOING|COMPLETED|LICENSED|PUBLISHING_FINISHED|CANCELLED|ON_HIATUS|UNKNOWN"}
            - PopularitySearch: {"type": "popularity", "minRating": number/null, "minPopularity": number/null}

            Genre examples: ACTION, ADVENTURE, COMEDY, DRAMA, FANTASY, HORROR, ISEKAI, MECHA, MYSTERY, PSYCHOLOGICAL, ROMANCE, SCI_FI, SLICE_OF_LIFE, SPORTS, SUPERNATURAL, THRILLER

            Return format:
            {
                "intents": [...],
                "confidence": 0.0-1.0,
                "isAmbiguous": true/false,
                "clarificationPrompt": "..." or null
            }

            Example:
            Query: "dark fantasy with magic schools"
            Response: {
                "intents": [
                    {"type": "mood", "mood": "DARK"},
                    {"type": "genre", "genres": ["FANTASY"], "matchMode": "ANY"},
                    {"type": "description", "keywords": ["magic school"], "matchMode": "ANY"}
                ],
                "confidence": 0.92,
                "isAmbiguous": false,
                "clarificationPrompt": null
            }

            Example:
            Query: "something like Berserk but finished"
            Response: {
                "intents": [
                    {"type": "title", "title": "Berserk", "fuzzyMatch": true},
                    {"type": "mood", "mood": "DARK"},
                    {"type": "status", "status": "COMPLETED"}
                ],
                "confidence": 0.88,
                "isAmbiguous": false,
                "clarificationPrompt": null
            }

            Return ONLY the JSON object, no markdown formatting, no explanations.
        """.trimIndent()
    }

    /**
     * Parse the AI response into a ParsedSearchQuery.
     */
    private fun parseAiResponse(originalQuery: String, response: String): ParsedSearchQuery {
        val cleanResponse = response.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = org.json.JSONObject(cleanResponse)
        val intentsArray = json.getJSONArray("intents")
        val intents = mutableListOf<SearchIntent>()

        for (i in 0 until intentsArray.length()) {
            val intentObj = intentsArray.getJSONObject(i)
            val type = intentObj.getString("type")

            val intent = when (type) {
                "title" -> SearchIntent.TitleSearch(
                    title = intentObj.getString("title"),
                    fuzzyMatch = intentObj.optBoolean("fuzzyMatch", true)
                )
                "genre" -> {
                    val genresArray = intentObj.getJSONArray("genres")
                    val genres = mutableListOf<String>()
                    for (j in 0 until genresArray.length()) {
                        genres.add(genresArray.getString(j))
                    }
                    SearchIntent.GenreSearch(
                        genres = genres,
                        matchMode = SearchIntent.GenreSearch.MatchMode.valueOf(
                            intentObj.optString("matchMode", "ANY").uppercase()
                        )
                    )
                }
                "author" -> SearchIntent.AuthorSearch(
                    author = intentObj.getString("author"),
                    includeArtist = intentObj.optBoolean("includeArtist", true)
                )
                "description" -> {
                    val keywordsArray = intentObj.getJSONArray("keywords")
                    val keywords = mutableListOf<String>()
                    for (j in 0 until keywordsArray.length()) {
                        keywords.add(keywordsArray.getString(j))
                    }
                    SearchIntent.DescriptionSearch(
                        keywords = keywords,
                        matchMode = SearchIntent.DescriptionSearch.MatchMode.valueOf(
                            intentObj.optString("matchMode", "ANY").uppercase()
                        )
                    )
                }
                "mood" -> SearchIntent.MoodSearch(
                    mood = SearchIntent.MoodSearch.Mood.valueOf(intentObj.getString("mood").uppercase())
                )
                "status" -> SearchIntent.StatusSearch(
                    status = MangaStatus.valueOf(intentObj.getString("status").uppercase())
                )
                "popularity" -> SearchIntent.PopularitySearch(
                    minRating = intentObj.optDouble("minRating", -1.0).takeIf { it >= 0 }?.toFloat(),
                    minPopularity = intentObj.optInt("minPopularity", -1).takeIf { it >= 0 }
                )
                else -> null
            }

            intent?.let { intents.add(it) }
        }

        return ParsedSearchQuery(
            originalQuery = originalQuery,
            intents = intents,
            confidence = json.optDouble("confidence", 0.5).toFloat(),
            isAmbiguous = json.optBoolean("isAmbiguous", false),
            clarificationPrompt = json.optString("clarificationPrompt").takeIf { it.isNotBlank() }
        )
    }

    /**
     * Hash the query for caching purposes.
     */
    private fun hashQuery(query: String): String {
        val normalized = query.lowercase().trim()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(normalized.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Check if a cached search is still valid (less than 7 days old).
     */
    private fun isCacheValid(cached: CachedSmartSearch): Boolean {
        val maxAgeMs = 7 * 24 * 60 * 60 * 1000L // 7 days
        return System.currentTimeMillis() - cached.timestamp < maxAgeMs
    }
}

/**
 * Exceptions for Smart Search.
 */
sealed class SmartSearchException(message: String) : Exception(message) {
    data object AiNotInitialized : SmartSearchException("AI client not initialized. Please configure API key in settings.")
    data class ParsingError(val details: String) : SmartSearchException("Failed to parse AI response: $details")
    data class AiProcessingError(val details: String) : SmartSearchException("AI processing failed: $details")
}