package app.otakureader.core.ai.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.otakureader.core.ai.model.AiConfig as AiModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration manager for AI-related settings.
 *
 * This class provides persistent storage and retrieval of AI configuration parameters
 * using DataStore. It handles:
 * - Generation parameters (temperature, max tokens, etc.)
 * - Provider selection and defaults
 * - Feature flags for AI capabilities
 * - Request timeout settings
 *
 * **Usage:**
 * ```kotlin
 * @Inject lateinit var aiConfig: AiConfigManager
 *
 * // Get current config
 * val config = aiConfig.getConfig().first()
 *
 * // Update temperature
 * aiConfig.setTemperature(0.7)
 * ```
 *
 * @param context The application context for DataStore
 */
@Singleton
class AiConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore: DataStore<Preferences> = context.aiConfigDataStore

    // ==================== Config Flow ====================

    /**
     * Get the current AI configuration as a Flow.
     *
     * @return Flow emitting [AiModelConfig] with current settings
     */
    fun getConfig(): Flow<AiModelConfig> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AiModelConfig(
                maxTokens = preferences[KEY_MAX_TOKENS],
                temperature = preferences[KEY_TEMPERATURE],
                topP = preferences[KEY_TOP_P],
                topK = preferences[KEY_TOP_K],
                requestTimeoutMillis = preferences[KEY_TIMEOUT_MILLIS] ?: DEFAULT_TIMEOUT_MILLIS,
                enableSafetyFilters = preferences[KEY_SAFETY_FILTERS] ?: DEFAULT_SAFETY_FILTERS
            )
        }

    /**
     * Get the current AI configuration (one-shot).
     *
     * @return Current [AiModelConfig]
     */
    suspend fun getConfigOnce(): AiModelConfig = getConfig().first()

    // ==================== Individual Settings ====================

    /**
     * Get the maximum tokens setting.
     *
     * @return Flow emitting max tokens or null if using default
     */
    fun getMaxTokens(): Flow<Int?> = getPreference(KEY_MAX_TOKENS)

    /**
     * Set the maximum tokens for generation.
     *
     * @param tokens Maximum number of tokens (null to use model default)
     */
    suspend fun setMaxTokens(tokens: Int?) {
        setPreference(KEY_MAX_TOKENS, tokens)
    }

    /**
     * Get the temperature setting.
     *
     * @return Flow emitting temperature (0.0 to 1.0) or null if using default
     */
    fun getTemperature(): Flow<Double?> = getPreference(KEY_TEMPERATURE)

    /**
     * Set the temperature for generation.
     *
     * @param temperature Temperature value (0.0 = deterministic, 1.0 = creative, null for default)
     */
    suspend fun setTemperature(temperature: Double?) {
        temperature?.let {
            require(it in 0.0..1.0) { "Temperature must be between 0.0 and 1.0" }
        }
        setPreference(KEY_TEMPERATURE, temperature)
    }

    /**
     * Get the top-p (nucleus sampling) setting.
     *
     * @return Flow emitting top-p value or null if using default
     */
    fun getTopP(): Flow<Double?> = getPreference(KEY_TOP_P)

    /**
     * Set the top-p value for nucleus sampling.
     *
     * @param topP Top-p value (0.0 to 1.0, null for default)
     */
    suspend fun setTopP(topP: Double?) {
        topP?.let {
            require(it in 0.0..1.0) { "Top-P must be between 0.0 and 1.0" }
        }
        setPreference(KEY_TOP_P, topP)
    }

    /**
     * Get the top-k setting.
     *
     * @return Flow emitting top-k value or null if using default
     */
    fun getTopK(): Flow<Int?> = getPreference(KEY_TOP_K)

    /**
     * Set the top-k value for sampling.
     *
     * @param topK Top-k value (null for default)
     */
    suspend fun setTopK(topK: Int?) {
        topK?.let {
            require(it > 0) { "Top-K must be positive" }
        }
        setPreference(KEY_TOP_K, topK)
    }

    /**
     * Get the request timeout in milliseconds.
     *
     * @return Flow emitting timeout in milliseconds
     */
    fun getRequestTimeoutMillis(): Flow<Long> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_TIMEOUT_MILLIS] ?: DEFAULT_TIMEOUT_MILLIS }

    /**
     * Set the request timeout.
     *
     * @param timeoutMillis Timeout in milliseconds (must be positive)
     */
    suspend fun setRequestTimeoutMillis(timeoutMillis: Long) {
        require(timeoutMillis > 0) { "Timeout must be positive" }
        setPreference(KEY_TIMEOUT_MILLIS, timeoutMillis)
    }

    /**
     * Get the safety filters enabled state.
     *
     * @return Flow emitting true if safety filters are enabled
     */
    fun getSafetyFiltersEnabled(): Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_SAFETY_FILTERS] ?: DEFAULT_SAFETY_FILTERS }

    /**
     * Set whether safety filters are enabled.
     *
     * @param enabled true to enable safety filters
     */
    suspend fun setSafetyFiltersEnabled(enabled: Boolean) {
        setPreference(KEY_SAFETY_FILTERS, enabled)
    }

    // ==================== Preset Configurations ====================

    /**
     * Apply a preset configuration.
     *
     * @param preset The preset to apply
     */
    suspend fun applyPreset(preset: ConfigPreset) {
        when (preset) {
            ConfigPreset.DEFAULT -> applyConfig(AiModelConfig.DEFAULT)
            ConfigPreset.FAST -> applyConfig(AiModelConfig.FAST)
            ConfigPreset.CREATIVE -> applyConfig(AiModelConfig.CREATIVE)
            ConfigPreset.PRECISE -> applyConfig(AiModelConfig.PRECISE)
        }
    }

    /**
     * Apply a custom configuration.
     *
     * @param config The configuration to apply
     */
    suspend fun applyConfig(config: AiModelConfig) {
        dataStore.edit { preferences ->
            config.maxTokens?.let { preferences[KEY_MAX_TOKENS] = it }
                ?: preferences.remove(KEY_MAX_TOKENS)
            config.temperature?.let { preferences[KEY_TEMPERATURE] = it }
                ?: preferences.remove(KEY_TEMPERATURE)
            config.topP?.let { preferences[KEY_TOP_P] = it }
                ?: preferences.remove(KEY_TOP_P)
            config.topK?.let { preferences[KEY_TOP_K] = it }
                ?: preferences.remove(KEY_TOP_K)
            preferences[KEY_TIMEOUT_MILLIS] = config.requestTimeoutMillis
            preferences[KEY_SAFETY_FILTERS] = config.enableSafetyFilters
        }
    }

    /**
     * Reset all configuration to defaults.
     */
    suspend fun resetToDefaults() {
        applyPreset(ConfigPreset.DEFAULT)
    }

    // ==================== Feature Flags ====================

    /**
     * Check if AI features are enabled by the user.
     *
     * @return Flow emitting true if AI features are enabled
     */
    fun getAiFeaturesEnabled(): Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_AI_FEATURES_ENABLED] ?: DEFAULT_AI_FEATURES_ENABLED }

    /**
     * Set whether AI features are enabled.
     *
     * @param enabled true to enable AI features
     */
    suspend fun setAiFeaturesEnabled(enabled: Boolean) {
        setPreference(KEY_AI_FEATURES_ENABLED, enabled)
    }

    /**
     * Get the default AI provider.
     *
     * @return Flow emitting the provider identifier (e.g., "gemini")
     */
    fun getDefaultProvider(): Flow<String> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_DEFAULT_PROVIDER] ?: DEFAULT_PROVIDER }

    /**
     * Set the default AI provider.
     *
     * @param provider The provider identifier
     */
    suspend fun setDefaultProvider(provider: String) {
        require(provider.isNotBlank()) { "Provider cannot be blank" }
        setPreference(KEY_DEFAULT_PROVIDER, provider)
    }

    // ==================== Private Helpers ====================

    private inline fun <reified T> getPreference(key: Preferences.Key<T>): Flow<T?> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[key] }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T?) {
        dataStore.edit { preferences ->
            if (value != null) {
                preferences[key] = value
            } else {
                preferences.remove(key)
            }
        }
    }

    companion object {
        private const val DATA_STORE_NAME = "ai_config"

        // Default values
        private const val DEFAULT_TIMEOUT_MILLIS = 30_000L
        private const val DEFAULT_SAFETY_FILTERS = true
        private const val DEFAULT_AI_FEATURES_ENABLED = false
        private const val DEFAULT_PROVIDER = "gemini"

        // Preference keys
        private val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        private val KEY_TEMPERATURE = doublePreferencesKey("temperature")
        private val KEY_TOP_P = doublePreferencesKey("top_p")
        private val KEY_TOP_K = intPreferencesKey("top_k")
        private val KEY_TIMEOUT_MILLIS = longPreferencesKey("timeout_millis")
        private val KEY_SAFETY_FILTERS = booleanPreferencesKey("safety_filters")
        private val KEY_AI_FEATURES_ENABLED = booleanPreferencesKey("ai_features_enabled")
        private val KEY_DEFAULT_PROVIDER = stringPreferencesKey("default_provider")

        private val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(
            name = DATA_STORE_NAME
        )
    }
}

/**
 * Preset configuration profiles.
 */
enum class ConfigPreset {
    /** Conservative default settings */
    DEFAULT,
    /** Fast responses with lower quality */
    FAST,
    /** Creative outputs with higher diversity */
    CREATIVE,
    /** Precise outputs for factual tasks */
    PRECISE
}
