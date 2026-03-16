package app.otakureader.data.ai

import app.otakureader.core.preferences.AiPreferences
import app.otakureader.domain.ai.AiFeature
import app.otakureader.domain.ai.AiFeatureGate
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [AiFeatureGate].
 *
 * Feature availability is determined by checking (in order):
 * 1. The master AI toggle ([AiPreferences.aiEnabled]).
 * 2. The presence of a non-blank Gemini API key ([AiPreferences.getGeminiApiKey]).
 * 3. The per-feature toggle for the requested [AiFeature].
 *
 * This class reads preference flows on each call so that toggling a setting in the
 * UI takes effect immediately without requiring an app restart.
 */
@Singleton
class AiFeatureGateImpl @Inject constructor(
    private val aiPreferences: AiPreferences,
) : AiFeatureGate {

    /**
     * Returns `true` when the master AI toggle is on **and** an API key is configured.
     */
    override suspend fun isAiAvailable(): Boolean {
        val masterEnabled = aiPreferences.aiEnabled.first()
        if (!masterEnabled) return false

        val apiKey = aiPreferences.getGeminiApiKey()
        return apiKey.isNotBlank()
    }

    /**
     * Returns `true` when [isAiAvailable] is satisfied and the per-feature toggle for
     * [feature] is also on.
     */
    override suspend fun isFeatureAvailable(feature: AiFeature): Boolean {
        if (!isAiAvailable()) return false

        return when (feature) {
            AiFeature.READING_INSIGHTS -> aiPreferences.aiReadingInsights.first()
            AiFeature.SMART_SEARCH -> aiPreferences.aiSmartSearch.first()
            AiFeature.RECOMMENDATIONS -> aiPreferences.aiRecommendations.first()
            AiFeature.PANEL_READER -> aiPreferences.aiPanelReader.first()
            AiFeature.SFX_TRANSLATION -> aiPreferences.aiSfxTranslation.first()
            AiFeature.SUMMARY_TRANSLATION -> aiPreferences.aiSummaryTranslation.first()
            AiFeature.SOURCE_INTELLIGENCE -> aiPreferences.aiSourceIntelligence.first()
            AiFeature.SMART_NOTIFICATIONS -> aiPreferences.aiSmartNotifications.first()
            AiFeature.AUTO_CATEGORIZATION -> aiPreferences.aiAutoCategorization.first()
        }
    }
}
