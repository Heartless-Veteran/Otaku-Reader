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

fun DownloadSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    // ── Downloads ─────────────────────────────────────────────────────
            SectionHeader(title = stringResource(R.string.settings_downloads))

            // Auto-download new chapters
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_auto_download_new_chapters)) },
                supportingContent = { Text(stringResource(R.string.settings_auto_download_new_chapters_description)) },
                trailingContent = {
                    Switch(
                        checked = state.autoDownloadEnabled,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetAutoDownloadEnabled(it))
                        }
                    )
                }
            )

            // Download only on Wi-Fi
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_download_only_wifi)) },
                supportingContent = { Text(stringResource(R.string.settings_download_only_wifi_description)) },
                trailingContent = {
                    Switch(
                        checked = state.downloadOnlyOnWifi,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetDownloadOnlyOnWifi(it))
                        }
                    )
                }
            )

            // Auto-download limit
            var sliderPosition by remember(state.autoDownloadLimit) {
                mutableFloatStateOf(state.autoDownloadLimit.toFloat())
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_auto_download_limit, sliderPosition.roundToInt())) },
                supportingContent = {
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it },
                        onValueChangeFinished = {
                            onEvent(
                                SettingsEvent.SetAutoDownloadLimit(sliderPosition.roundToInt())
                            )
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_download_location))

            // Download location
            val locationText = state.downloadLocation?.let { 
                it.substringAfterLast("/") 
            } ?: stringResource(R.string.settings_download_location_default)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_download_location)) },
                supportingContent = { Text(locationText) },
                trailingContent = {
                    OutlinedButton(onClick = { onEvent(SettingsEvent.SetDownloadLocation(null)) }) {
                        Text(stringResource(R.string.settings_change))
                    }
                },
                modifier = Modifier.clickable {
                    onEvent(SettingsEvent.SetDownloadLocation(null))
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_download_performance))

            // Concurrent downloads
            var concurrentSlider by remember(state.concurrentDownloads) {
                mutableFloatStateOf(state.concurrentDownloads.toFloat())
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_concurrent_downloads, concurrentSlider.roundToInt())) },
                supportingContent = {
                    Slider(
                        value = concurrentSlider,
                        onValueChange = { concurrentSlider = it },
                        onValueChangeFinished = {
                            onEvent(SettingsEvent.SetConcurrentDownloads(concurrentSlider.roundToInt()))
                        },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_download_ahead))

            // Download ahead while reading
            var aheadSlider by remember(state.downloadAheadWhileReading) {
                mutableFloatStateOf(state.downloadAheadWhileReading.toFloat())
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_download_ahead_count, aheadSlider.roundToInt())) },
                supportingContent = {
                    Column {
                        Text(stringResource(R.string.settings_download_ahead_description))
                        Slider(
                            value = aheadSlider,
                            onValueChange = { aheadSlider = it },
                            onValueChangeFinished = {
                                onEvent(SettingsEvent.SetDownloadAheadWhileReading(aheadSlider.roundToInt()))
                            },
                            valueRange = 0f..5f,
                            steps = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )

            // Download ahead only on Wi-Fi
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_download_ahead_wifi_only)) },
                supportingContent = { Text(stringResource(R.string.settings_download_ahead_wifi_only_description)) },
                trailingContent = {
                    Switch(
                        checked = state.downloadAheadOnlyOnWifi,
                        onCheckedChange = { onEvent(SettingsEvent.SetDownloadAheadOnlyOnWifi(it)) }
                    )
                }
            )

            // Delete after reading
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_delete_after_reading)) },
                supportingContent = { Text(stringResource(R.string.settings_delete_after_reading_description)) },
                trailingContent = {
                    Switch(
                        checked = state.deleteAfterReading,
                        onCheckedChange = { onEvent(SettingsEvent.SetDeleteAfterReading(it)) }
                    )
                }
            )

            // Save as CBZ
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_save_as_cbz)) },
                supportingContent = { Text(stringResource(R.string.settings_save_as_cbz_description)) },
                trailingContent = {
                    Switch(
                        checked = state.saveAsCbz,
                        onCheckedChange = { onEvent(SettingsEvent.SetSaveAsCbz(it)) }
                    )
                }
            )
}

