package app.otakureader.domain.repository

/**
 * Loader for AI prompts from external sources (assets, remote config, etc).
 *
 * This abstraction allows prompts to be externalized from code (H-7),
 * enabling updates without app releases.
 */
interface PromptLoader {
    /**
     * Load a prompt template by name.
     *
     * @param name The prompt identifier (e.g., "smart_search")
     * @return The prompt template string, or null if not found
     */
    suspend fun loadPrompt(name: String): String?
}
