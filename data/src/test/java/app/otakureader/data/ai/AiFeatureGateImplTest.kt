package app.otakureader.data.ai

import app.otakureader.core.preferences.AiPreferences
import app.otakureader.domain.ai.AiFeature
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiFeatureGateImplTest {

    private lateinit var aiPreferences: AiPreferences
    private lateinit var gate: AiFeatureGateImpl

    @Before
    fun setUp() {
        aiPreferences = mockk()
        gate = AiFeatureGateImpl(aiPreferences)
    }

    // ---- isAiAvailable ----

    @Test
    fun `isAiAvailable returns false when master toggle is off`() = runTest {
        every { aiPreferences.aiEnabled } returns flowOf(false)

        assertFalse(gate.isAiAvailable())
    }

    @Test
    fun `isAiAvailable returns false when toggle is on but API key is blank`() = runTest {
        every { aiPreferences.aiEnabled } returns flowOf(true)
        coEvery { aiPreferences.getGeminiApiKey() } returns ""

        assertFalse(gate.isAiAvailable())
    }

    @Test
    fun `isAiAvailable returns true when toggle is on and API key is set`() = runTest {
        every { aiPreferences.aiEnabled } returns flowOf(true)
        coEvery { aiPreferences.getGeminiApiKey() } returns "AIzaSomeFakeKey"

        assertTrue(gate.isAiAvailable())
    }

    // ---- isFeatureAvailable ----

    @Test
    fun `isFeatureAvailable returns false when global AI is unavailable`() = runTest {
        every { aiPreferences.aiEnabled } returns flowOf(false)

        assertFalse(gate.isFeatureAvailable(AiFeature.READING_INSIGHTS))
    }

    @Test
    fun `isFeatureAvailable returns false when feature toggle is off`() = runTest {
        every { aiPreferences.aiEnabled } returns flowOf(true)
        coEvery { aiPreferences.getGeminiApiKey() } returns "AIzaSomeFakeKey"
        every { aiPreferences.aiReadingInsights } returns flowOf(false)

        assertFalse(gate.isFeatureAvailable(AiFeature.READING_INSIGHTS))
    }

    @Test
    fun `isFeatureAvailable returns true when global AI available and feature toggle on`() = runTest {
        every { aiPreferences.aiEnabled } returns flowOf(true)
        coEvery { aiPreferences.getGeminiApiKey() } returns "AIzaSomeFakeKey"
        every { aiPreferences.aiSmartSearch } returns flowOf(true)

        assertTrue(gate.isFeatureAvailable(AiFeature.SMART_SEARCH))
    }

    @Test
    fun `isFeatureAvailable checks the correct toggle for each feature`() = runTest {
        every { aiPreferences.aiEnabled } returns flowOf(true)
        coEvery { aiPreferences.getGeminiApiKey() } returns "AIzaSomeFakeKey"
        every { aiPreferences.aiReadingInsights } returns flowOf(true)
        every { aiPreferences.aiSmartSearch } returns flowOf(false)
        every { aiPreferences.aiRecommendations } returns flowOf(true)
        every { aiPreferences.aiPanelReader } returns flowOf(false)
        every { aiPreferences.aiSfxTranslation } returns flowOf(true)
        every { aiPreferences.aiSummaryTranslation } returns flowOf(false)
        every { aiPreferences.aiSourceIntelligence } returns flowOf(true)
        every { aiPreferences.aiSmartNotifications } returns flowOf(false)
        every { aiPreferences.aiAutoCategorization } returns flowOf(true)

        assertTrue(gate.isFeatureAvailable(AiFeature.READING_INSIGHTS))
        assertFalse(gate.isFeatureAvailable(AiFeature.SMART_SEARCH))
        assertTrue(gate.isFeatureAvailable(AiFeature.RECOMMENDATIONS))
        assertFalse(gate.isFeatureAvailable(AiFeature.PANEL_READER))
        assertTrue(gate.isFeatureAvailable(AiFeature.SFX_TRANSLATION))
        assertFalse(gate.isFeatureAvailable(AiFeature.SUMMARY_TRANSLATION))
        assertTrue(gate.isFeatureAvailable(AiFeature.SOURCE_INTELLIGENCE))
        assertFalse(gate.isFeatureAvailable(AiFeature.SMART_NOTIFICATIONS))
        assertTrue(gate.isFeatureAvailable(AiFeature.AUTO_CATEGORIZATION))
    }
}
