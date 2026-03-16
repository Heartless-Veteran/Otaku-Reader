package app.otakureader.domain.ai

/**
 * Enumeration of individual AI-powered features in the application.
 *
 * Each entry maps to a per-feature toggle in [AiPreferences] and is used by
 * [AiFeatureGate] to determine whether a specific feature is currently enabled.
 */
enum class AiFeature {
    /** AI-powered reading statistics and insights. */
    READING_INSIGHTS,

    /** Natural-language smart search queries. */
    SMART_SEARCH,

    /** Personalised manga recommendations based on reading history. */
    RECOMMENDATIONS,

    /** Panel-aware reader using Gemini Vision. */
    PANEL_READER,

    /** In-page sound-effect translation. */
    SFX_TRANSLATION,

    /** Automatic translation of chapter summaries. */
    SUMMARY_TRANSLATION,

    /** Intelligent scoring and ranking of manga sources. */
    SOURCE_INTELLIGENCE,

    /** Context-aware update notification summaries. */
    SMART_NOTIFICATIONS,

    /** Automatic manga categorisation when added to the library. */
    AUTO_CATEGORIZATION,
}
