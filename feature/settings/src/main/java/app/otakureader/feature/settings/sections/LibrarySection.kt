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

fun LibrarySection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    // ── Library ───────────────────────────────────────────────────────
            SectionHeader(title = stringResource(R.string.settings_library))

            // Grid size – use a local slider state so DataStore is written only when
            // the user finishes dragging, not on every intermediate position.
            var sliderPosition by remember(state.libraryGridSize) {
                mutableFloatStateOf(state.libraryGridSize.toFloat())
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_grid_columns, sliderPosition.roundToInt())) },
                supportingContent = {
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it },
                        onValueChangeFinished = {
                            onEvent(
                                SettingsEvent.SetLibraryGridSize(sliderPosition.roundToInt())
                            )
                        },
                        valueRange = 2f..5f,
                        steps = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )

            // Badges
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_unread_badges)) },
                supportingContent = { Text(stringResource(R.string.settings_show_unread_badges_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showBadges,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetShowBadges(it))
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_library_updates))

            // Update only on Wi-Fi
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_update_wifi_only)) },
                supportingContent = { Text(stringResource(R.string.settings_update_wifi_only_description)) },
                trailingContent = {
                    Switch(
                        checked = state.updateOnlyOnWifi,
                        onCheckedChange = { onEvent(SettingsEvent.SetUpdateOnlyOnWifi(it)) }
                    )
                }
            )

            // Update only pinned categories
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_update_pinned_only)) },
                supportingContent = { Text(stringResource(R.string.settings_update_pinned_only_description)) },
                trailingContent = {
                    Switch(
                        checked = state.updateOnlyPinnedCategories,
                        onCheckedChange = { onEvent(SettingsEvent.SetUpdateOnlyPinnedCategories(it)) }
                    )
                }
            )

            // Auto refresh on start
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_auto_refresh)) },
                supportingContent = { Text(stringResource(R.string.settings_auto_refresh_description)) },
                trailingContent = {
                    Switch(
                        checked = state.autoRefreshOnStart,
                        onCheckedChange = { onEvent(SettingsEvent.SetAutoRefreshOnStart(it)) }
                    )
                }
            )

            // Show update progress
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_update_progress)) },
                supportingContent = { Text(stringResource(R.string.settings_show_update_progress_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showUpdateProgress,
                        onCheckedChange = { onEvent(SettingsEvent.SetShowUpdateProgress(it)) }
                    )
                }
            )
}

