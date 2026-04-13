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

fun NotificationsSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    // ── Notifications ─────────────────────────────────────────────────
            SectionHeader(title = stringResource(R.string.settings_notifications))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_enable_notifications)) },
                supportingContent = { Text(stringResource(R.string.settings_enable_notifications_description)) },
                trailingContent = {
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetNotificationsEnabled(it))
                        }
                    )
                }
            )

            // Update check interval
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_update_check_interval)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val intervals = listOf(
                            stringResource(R.string.settings_update_interval_6h) to 6,
                            stringResource(R.string.settings_update_interval_12h) to 12,
                            stringResource(R.string.settings_update_interval_24h) to 24
                        )
                        intervals.forEach { (label, hours) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.updateCheckInterval == hours,
                                        onClick = {
                                            onEvent(SettingsEvent.SetUpdateInterval(hours))
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = state.updateCheckInterval == hours,
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

