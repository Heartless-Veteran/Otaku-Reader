package app.otakureader.domain.ai

/**
 * Gate that decides whether AI features are currently usable.
 *
 * An AI feature is considered available when all of the following are true:
 * 1. The build variant includes the AI module (not a FOSS/noop build).
 * 2. The master AI toggle in settings is **on**.
 * 3. A valid API key has been configured (BYOK).
 * 4. The per-feature toggle (if any) is **on**.
 *
 * All AI use cases should check this gate before calling the AI backend, so that
 * they degrade gracefully when AI is unavailable rather than throwing exceptions.
 */
interface AiFeatureGate {

    /**
     * Returns `true` if the global AI prerequisite conditions are satisfied
     * (master toggle enabled and API key configured), regardless of individual
     * feature toggles.
     */
    suspend fun isAiAvailable(): Boolean

    /**
     * Returns `true` if [feature] is available.
     *
     * This implies [isAiAvailable] is true **and** the per-feature toggle for
     * [feature] is also enabled.
     */
    suspend fun isFeatureAvailable(feature: AiFeature): Boolean
}
