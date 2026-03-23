package app.otakureader.domain.repository

/**
 * Repository for AI-powered features.
 *
 * Provides access to AI functionality such as content generation,
 * manga recommendations, and text analysis.
 */
interface AiRepository {

    /**
     * Generate content based on a text prompt.
     *
     * @param prompt The text prompt to send to the AI
     * @return The generated text response
     */
    suspend fun generateContent(prompt: String): Result<String>

    /**
     * Check if the AI service is available and properly configured.
     *
     * @return true if the AI service is ready to use, false otherwise
     */
    suspend fun isAvailable(): Boolean

    /**
     * Initialize the AI service.
     *
     * The API key is read internally from secure storage rather than passed as a
     * parameter (H-8: prevents key exposure in logs/memory dumps).
     *
     * @throws IllegalStateException if no API key is configured
     */
    suspend fun initialize()

    /**
     * Clear the active API key and reset the AI client to an uninitialized state.
     *
     * After this call, [isAvailable] returns false until a new key is configured
     * and [initialize] is called again.
     */
    suspend fun clearApiKey()
}
