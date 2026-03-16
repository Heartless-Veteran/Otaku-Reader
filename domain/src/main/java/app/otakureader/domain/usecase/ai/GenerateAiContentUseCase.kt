package app.otakureader.domain.usecase.ai

import app.otakureader.domain.ai.AiFeature
import app.otakureader.domain.ai.AiFeatureGate
import app.otakureader.domain.repository.AiRepository

/**
 * Use case for generating AI-powered content.
 *
 * This use case provides a clean interface for features to interact
 * with AI capabilities, such as generating manga summaries, recommendations,
 * or other text-based content.
 *
 * Before calling the AI backend the gate is checked so that callers degrade
 * gracefully when AI is disabled or not configured.
 *
 * @property aiRepository Repository for AI operations
 * @property aiFeatureGate Gate that determines whether AI features are available
 */
class GenerateAiContentUseCase(
    private val aiRepository: AiRepository,
    private val aiFeatureGate: AiFeatureGate,
) {
    /**
     * Generate content based on the provided prompt.
     *
     * @param prompt The text prompt to send to the AI
     * @param feature The specific AI feature making this request, or `null` to
     *   check only the global AI availability (master toggle + API key).
     * @return Result containing the generated text on success, or an error.
     *   Returns [Result.failure] with [IllegalStateException] when AI is not
     *   available instead of propagating to the AI backend.
     */
    suspend operator fun invoke(
        prompt: String,
        feature: AiFeature? = null,
    ): Result<String> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be blank"))
        }

        val available = if (feature != null) {
            aiFeatureGate.isFeatureAvailable(feature)
        } else {
            aiFeatureGate.isAiAvailable()
        }

        if (!available) {
            return Result.failure(
                IllegalStateException("AI feature is not available. Ensure AI is enabled and an API key is configured.")
            )
        }

        return aiRepository.generateContent(prompt)
    }
}
