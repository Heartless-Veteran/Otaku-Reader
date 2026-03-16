package app.otakureader.feature.settings

/**
 * Returns `true` if [key] is a plausibly valid Gemini API key format.
 *
 * Gemini keys follow the Google API key convention: they start with the prefix
 * `AIza` and are at least 20 characters long. This is a lightweight format
 * check — it does **not** verify the key against the Gemini service.
 *
 * Used from both [SettingsViewModel] (validation before persist) and
 * [SettingsScreen] (disabling the Save button on obviously wrong input).
 */
internal fun isGeminiApiKeyFormatValid(key: String): Boolean =
    key.startsWith("AIza") && key.length >= 20
