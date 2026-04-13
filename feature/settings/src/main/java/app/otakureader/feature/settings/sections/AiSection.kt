package app.otakureader.feature.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.otakureader.core.preferences.AiTier
import app.otakureader.core.ui.theme.COLOR_SCHEME_CUSTOM_ACCENT
import app.otakureader.feature.reader.model.ImageQuality
import app.otakureader.feature.settings.SettingsEvent
import app.otakureader.feature.settings.SettingsState
import app.otakureader.feature.settings.R
import java.util.Locale
import kotlin.math.roundToInt

fun AiSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    // ── AI ────────────────────────────────────────────────────────────
    SectionHeader(title = stringResource(R.string.settings_ai_features))

    // Master toggle
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_ai_enable)) },
        supportingContent = { Text(stringResource(R.string.settings_ai_enable_description)) },
        trailingContent = {
            Switch(
                checked = state.aiEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetAiEnabled(it)) }
            )
        }
    )

    // API Key input (only shown when AI is enabled)
    if (state.aiEnabled) {
        var apiKeyInput by remember { mutableStateOf("") }
        var apiKeyVisible by remember { mutableStateOf(false) }
        val isKeyFormatValid = apiKeyInput.isBlank() || isGeminiApiKeyFormatValid(apiKeyInput)

        // Remove API key confirmation dialog
        if (state.showRemoveApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(SettingsEvent.DismissRemoveApiKeyDialog) },
                title = { Text(stringResource(R.string.settings_ai_remove_key_dialog_title)) },
                text = { Text(stringResource(R.string.settings_ai_remove_key_dialog_text)) },
                confirmButton = {
                    Button(onClick = { onEvent(SettingsEvent.ConfirmRemoveAiApiKey) }) {
                        Text(stringResource(R.string.settings_ai_remove_key_confirm))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { onEvent(SettingsEvent.DismissRemoveApiKeyDialog) }) {
                        Text(stringResource(R.string.settings_ai_remove_key_cancel))
                    }
                }
            )
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_ai_gemini_api_key)) },
            supportingContent = {
                Column {
                    if (state.aiApiKeySet) {
                        Text(
                            text = stringResource(R.string.settings_ai_api_key_set),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text(stringResource(R.string.settings_ai_api_key_label)) },
                        singleLine = true,
                        isError = !isKeyFormatValid,
                        supportingText = if (!isKeyFormatValid) {
                            { Text(stringResource(R.string.settings_ai_api_key_invalid_format)) }
                        } else null,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = if (apiKeyVisible) stringResource(R.string.settings_ai_hide_key)
                                    else stringResource(R.string.settings_ai_show_key)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onEvent(SettingsEvent.SetAiApiKey(apiKeyInput))
                            apiKeyInput = ""
                        },
                        enabled = apiKeyInput.isNotBlank() && isKeyFormatValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.settings_ai_save_api_key))
                    }
                    if (state.aiApiKeySet) {
                        OutlinedButton(
                            onClick = { onEvent(SettingsEvent.RemoveAiApiKey) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(stringResource(R.string.settings_ai_remove_key))
                        }
                    }
                }
            }
        )

        // Tier selection
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_ai_service_tier)) },
            supportingContent = {
                Row(modifier = Modifier.selectableGroup()) {
                    listOf(
                        stringResource(R.string.settings_ai_tier_free) to AiTier.FREE,
                        stringResource(R.string.settings_ai_tier_standard) to AiTier.STANDARD,
                        stringResource(R.string.settings_ai_tier_pro) to AiTier.PRO
                    ).forEach { (label, tier) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .selectable(
                                    selected = state.aiTier == tier,
                                    onClick = { onEvent(SettingsEvent.SetAiTier(tier)) },
                                    role = Role.RadioButton
                                )
                                .padding(end = 8.dp)
                        ) {
                            RadioButton(selected = state.aiTier == tier, onClick = null)
                            Text(text = label, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SectionHeader(title = stringResource(R.string.settings_ai_feature_toggles))

        // Individual feature toggles
        val features = listOf(
            AiFeatureToggle(stringResource(R.string.settings_ai_reading_insights), stringResource(R.string.settings_ai_reading_insights_desc), state.aiReadingInsights) { SettingsEvent.SetAiReadingInsights(it) },
            AiFeatureToggle(stringResource(R.string.settings_ai_smart_search), stringResource(R.string.settings_ai_smart_search_desc), state.aiSmartSearch) { SettingsEvent.SetAiSmartSearch(it) },
            AiFeatureToggle(stringResource(R.string.settings_ai_recommendations), stringResource(R.string.settings_ai_recommendations_desc), state.aiRecommendations) { SettingsEvent.SetAiRecommendations(it) },
            AiFeatureToggle(stringResource(R.string.settings_ai_panel_reader), stringResource(R.string.settings_ai_panel_reader_desc), state.aiPanelReader) { SettingsEvent.SetAiPanelReader(it) },
            AiFeatureToggle(stringResource(R.string.settings_ai_sfx_translation), stringResource(R.string.settings_ai_sfx_translation_desc), state.aiSfxTranslation) { SettingsEvent.SetAiSfxTranslation(it) },
            AiFeatureToggle(stringResource(R.string.settings_ai_summary_translation), stringResource(R.string.settings_ai_summary_translation_desc), state.aiSummaryTranslation) { SettingsEvent.SetAiSummaryTranslation(it) },
            AiFeatureToggle(stringResource(R.string.settings_ai_source_intelligence), stringResource(R.string.settings_ai_source_intelligence_desc), state.aiSourceIntelligence) { SettingsEvent.SetAiSourceIntelligence(it) },
            AiFeatureToggle(stringResource(R.string.settings_ai_smart_notifications), stringResource(R.string.settings_ai_smart_notifications_desc), state.aiSmartNotifications) { SettingsEvent.SetAiSmartNotifications(it) },
            AiFeatureToggle(stringResource(R.string.settings_ai_auto_categorization), stringResource(R.string.settings_ai_auto_categorization_desc), state.aiAutoCategorization) { SettingsEvent.SetAiAutoCategorization(it) }
        )

        features.forEach { feature ->
            ListItem(
                headlineContent = { Text(feature.label) },
                supportingContent = { Text(feature.description) },
                trailingContent = {
                    Switch(
                        checked = feature.enabled,
                        onCheckedChange = { onEvent(feature.makeEvent(it)) }
                    )
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SectionHeader(title = stringResource(R.string.settings_ai_usage))

        // Token usage
        val usageLabel = if (state.aiTokenTrackingPeriod.isNotBlank()) {
            stringResource(R.string.settings_ai_tokens_used_period, state.aiTokensUsedThisMonth, state.aiTokenTrackingPeriod)
        } else {
            stringResource(R.string.settings_ai_tokens_used_month, state.aiTokensUsedThisMonth)
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_ai_monthly_token_usage)) },
            supportingContent = { Text(usageLabel) }
        )

        // Clear AI cache
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_ai_clear_cache)) },
            supportingContent = { Text(stringResource(R.string.settings_ai_clear_cache_description)) },
            trailingContent = {
                OutlinedButton(onClick = { onEvent(SettingsEvent.ClearAiCache) }) {
                    Text(stringResource(R.string.settings_ai_clear))
                }
            }
        )
    }
}

/**
 * Holds display metadata and the toggle state for a single AI feature switch.
 */
private data class AiFeatureToggle(
    val label: String,
    val description: String,
    val enabled: Boolean,
    val makeEvent: (Boolean) -> SettingsEvent
)

/**
 * Preset accent colors for the custom accent color picker.
 * Each pair is (display name, ARGB Long).
 */
private val AccentColorPresets: List<Pair<String, Long>> = listOf(
    "Red" to 0xFFE53935L,
    "Pink" to 0xFFD81B60L,
    "Purple" to 0xFF8E24AAL,
    "Deep Purple" to 0xFF5E35B1L,
    "Indigo" to 0xFF3949ABL,
    "Blue" to 0xFF1E88E5L,
    "Light Blue" to 0xFF039BE5L,
    "Cyan" to 0xFF00ACC1L,
    "Teal" to 0xFF00897BL,
    "Green" to 0xFF43A047L,
    "Light Green" to 0xFF7CB342L,
    "Lime" to 0xFFC0CA33L,
    "Yellow" to 0xFFFDD835L,
    "Amber" to 0xFFFFB300L,
    "Orange" to 0xFFFB8C00L,
    "Deep Orange" to 0xFFF4511EL,
    "Brown" to 0xFF6D4C41L,
    "Blue Grey" to 0xFF546E7AL
)

/**
 * A grid of color swatches for selecting a custom accent color.
 * Uses FlowRow instead of LazyVerticalGrid to avoid infinite-height measurement
 * inside the parent scrollable Column.
 */
