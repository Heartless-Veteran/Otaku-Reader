package app.otakureader.domain.usecase.ai

import app.otakureader.domain.repository.AiRepository

/**
 * Use case for generating AI-powered content.
 *
 * This use case wraps [AiRepository.generateContent] and validates the prompt
 * before sending it to the AI. Configuration parameters (temperature, max tokens)
 * are handled by the repository layer.
 *
 * @property aiRepository Repository for AI operations
 */
class GenerateAiContentWithConfigUseCase(
    private val aiRepository: AiRepository
) {
    /**
     * Generate content from a text prompt.
     *
     * @param prompt The text prompt to send to the AI
     * @return [Result] containing the generated text on success, or an error
     */
    suspend operator fun invoke(prompt: String): Result<String> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be blank"))
        }
        return aiRepository.generateContent(prompt)
    }
}
