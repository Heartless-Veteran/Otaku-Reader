package app.otakureader.domain.usecase.ai

import javax.inject.Inject

/**
 * Placeholder for AI-generated reading reminder notification text.
 *
 * Smart notification generation is planned for a future phase and is not yet implemented.
 * This stub always returns null, causing callers to fall back to static notification messages.
 */
class GenerateSmartNotificationUseCase @Inject constructor() {
    /**
     * Returns null — AI notification generation is not yet implemented.
     *
     * @param chaptersToday number of chapters read today
     * @param dailyGoal the user's daily chapter goal (0 = no goal)
     * @param currentStreak current reading streak in days
     */
    suspend operator fun invoke(chaptersToday: Int, dailyGoal: Int, currentStreak: Int): String? = null
}
