package app.otakureader.domain.usecase.ai

import app.otakureader.core.ai.model.AiConfig
import app.otakureader.core.ai.model.AiException
import app.otakureader.core.ai.model.AiRequest
import app.otakureader.core.common.result.Result
import app.otakureader.domain.repository.AiRepository

/**
 * Use case for generating AI-powered content with advanced configuration.
 *
 * This use case provides a clean interface for features to interact
 * with AI capabilities, supporting:
 * - Custom generation parameters (temperature, max tokens, etc.)
 * - Timeout configuration
 * - Safety filter controls
 *
 * @property aiRepository Repository for AI operations
 */
class GenerateAiContentWithConfigUseCase(
    private val aiRepository: app.otakureader.data.repository.AiRepositoryImpl
) {
    /**
     * Generate content with full configuration control.
     *
     * @param request The AI request containing prompt and parameters
     * @return [Result.Success] with generated text, or [Result.Error]
     */
    suspend operator fun invoke(request: AiRequest): Result<String> {
        return when (val result = aiRepository.generateContentWithConfig(request)) {
            is Result.Success -> Result.Success(result.data.content)
            is Result.Error -> Result.Error(result.exception)
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * Generate content with a simple prompt and custom config.
     *
     * @param prompt The text prompt
     * @param config Generation configuration
     * @return [Result.Success] with generated text, or [Result.Error]
     */
    suspend operator fun invoke(
        prompt: String,
        config: AiConfig
    ): Result<String> {
        aiRepository.updateConfig(config)
        return aiRepository.generateContent(prompt)
    }
}
