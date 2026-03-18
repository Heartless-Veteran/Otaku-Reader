package app.otakureader.domain.usecase.ai

import app.otakureader.core.ai.model.AiException
import app.otakureader.core.ai.model.AiRequest
import app.otakureader.core.common.result.Result
import app.otakureader.domain.repository.AiRepository

/**
 * Use case for generating manga summaries using AI.
 *
 * This use case provides specialized prompting for manga summary generation,
 * with appropriate token limits and temperature settings.
 *
 * @property aiRepository Repository for AI operations
 */
class GenerateMangaSummaryUseCase(
    private val aiRepository: AiRepository
) {
    /**
     * Generate a summary for manga description or chapter content.
     *
     * @param title The manga title
     * @param description The full description text to summarize
     * @param maxLength Maximum word count for the summary (default: 100)
     * @return [Result.Success] with the generated summary, or [Result.Error]
     */
    suspend operator fun invoke(
        title: String,
        description: String,
        maxLength: Int = 100
    ): Result<String> {
        if (description.isBlank()) {
            return Result.Error(AiException.InvalidRequest("Description cannot be blank"))
        }

        val prompt = buildString {
            append("Summarize the following manga in $maxLength words or less. ")
            append("Focus on the main plot and genre without spoilers:\n\n")
            append("Title: $title\n")
            append("Description: $description")
        }

        return aiRepository.generateContent(prompt)
    }

    /**
     * Generate a summary for multiple chapters.
     *
     * @param title The manga title
     * @param chapterTitles List of recent chapter titles
     * @param maxLength Maximum word count for the summary
     * @return [Result.Success] with the arc summary, or [Result.Error]
     */
    suspend fun summarizeArc(
        title: String,
        chapterTitles: List<String>,
        maxLength: Int = 150
    ): Result<String> {
        if (chapterTitles.isEmpty()) {
            return Result.Error(AiException.InvalidRequest("Chapter titles cannot be empty"))
        }

        val prompt = buildString {
            append("Based on these chapter titles from '$title', ")
            append("summarize the current story arc in $maxLength words or less:\n\n")
            chapterTitles.forEach { append("- $it\n") }
        }

        return aiRepository.generateContent(prompt)
    }
}
