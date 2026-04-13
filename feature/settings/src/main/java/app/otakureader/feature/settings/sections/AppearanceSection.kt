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
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

fun AppearanceSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    // ── Appearance ────────────────────────────────────────────────────
            SectionHeader(title = stringResource(R.string.settings_appearance))

            // Theme mode
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = {
                    Row(modifier = Modifier.selectableGroup()) {
                        val options = listOf(
                            stringResource(R.string.settings_theme_system) to 0,
                            stringResource(R.string.settings_theme_light) to 1,
                            stringResource(R.string.settings_theme_dark) to 2
                        )
                        options.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .selectable(
                                        selected = state.themeMode == value,
                                        onClick = {
                                            onEvent(SettingsEvent.SetThemeMode(value))
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(end = 8.dp)
                            ) {
                                RadioButton(
                                    selected = state.themeMode == value,
                                    onClick = null
                                )
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                                )
                            }
                        }
                    }
                }
            )

            // Pure Black dark mode
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_pure_black)) },
                supportingContent = { Text(stringResource(R.string.settings_pure_black_description)) },
                trailingContent = {
                    Switch(
                        checked = state.usePureBlackDarkMode,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetPureBlackDarkMode(it))
                        }
                    )
                }
            )

            // High contrast mode
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_high_contrast)) },
                supportingContent = { Text(stringResource(R.string.settings_high_contrast_description)) },
                trailingContent = {
                    Switch(
                        checked = state.useHighContrast,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetHighContrast(it))
                        }
                    )
                }
            )

            // Color scheme picker
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_color_scheme)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val schemes = listOf(
                            stringResource(R.string.settings_color_scheme_system_default) to 0,
                            stringResource(R.string.settings_color_scheme_dynamic) to 1,
                            stringResource(R.string.settings_color_scheme_green_apple) to 2,
                            stringResource(R.string.settings_color_scheme_lavender) to 3,
                            stringResource(R.string.settings_color_scheme_midnight_dusk) to 4,
                            stringResource(R.string.settings_color_scheme_strawberry_daiquiri) to 5,
                            stringResource(R.string.settings_color_scheme_tako) to 6,
                            stringResource(R.string.settings_color_scheme_teal_turquoise) to 7,
                            stringResource(R.string.settings_color_scheme_tidal_wave) to 8,
                            stringResource(R.string.settings_color_scheme_yotsuba) to 9,
                            stringResource(R.string.settings_color_scheme_yin_yang) to 10,
                            stringResource(R.string.settings_color_scheme_custom_accent) to COLOR_SCHEME_CUSTOM_ACCENT
                        )
                        schemes.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.colorScheme == value,
                                        onClick = { onEvent(SettingsEvent.SetColorScheme(value)) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = state.colorScheme == value,
                                    onClick = null
                                )
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            )

            // Custom accent color picker (shown when "Custom Accent" is selected)
            if (state.colorScheme == COLOR_SCHEME_CUSTOM_ACCENT) {
                AccentColorPicker(
                    selectedColor = state.customAccentColor,
                    onColorSelected = { onEvent(SettingsEvent.SetCustomAccentColor(it)) }
                )
            }

            // Language
            val context = LocalContext.current
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+: delegate to the system per-app language picker
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    supportingContent = { Text(stringResource(R.string.settings_language_system_settings_hint)) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }
                )
            } else {
                // Android 12 and below: in-app language picker
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    supportingContent = {
                        Column(modifier = Modifier.selectableGroup()) {
                            val localeTags = listOf("", "en", "ja", "ko", "zh-Hans", "es", "fr", "de", "pt", "ru")
                            val systemDefaultLabel = stringResource(R.string.settings_language_system_default)
                            localeTags.forEach { tag ->
                                val label = if (tag.isEmpty()) {
                                    systemDefaultLabel
                                } else {
                                    val loc = Locale.forLanguageTag(tag)
                                    loc.getDisplayName(loc).replaceFirstChar { it.uppercase() }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = state.locale == tag,
                                            onClick = { onEvent(SettingsEvent.SetLocale(tag)) },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = state.locale == tag,
                                        onClick = null
                                    )
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

}

