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

fun DataStorageSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    // ── Data & Storage ────────────────────────────────────────────────
            SectionHeader(title = stringResource(R.string.settings_backup_restore_migration))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_create_backup)) },
                supportingContent = { Text(stringResource(R.string.settings_create_backup_description)) },
                trailingContent = {
                    if (state.isBackupInProgress) {
                        CircularProgressIndicator()
                    } else {
                        Button(onClick = { onEvent(SettingsEvent.OnCreateBackup) }) {
                            Text(stringResource(R.string.settings_backup_button))
                        }
                    }
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_restore_backup)) },
                supportingContent = { Text(stringResource(R.string.settings_restore_backup_description)) },
                trailingContent = {
                    if (state.isRestoreInProgress) {
                        CircularProgressIndicator()
                    } else {
                        Button(onClick = { onEvent(SettingsEvent.OnRestoreBackup) }) {
                            Text(stringResource(R.string.settings_restore_button))
                        }
                    }
                }
            )

            // ── Automatic backups ──
            HorizontalDivider()
            SectionHeader(title = stringResource(R.string.settings_automatic_backups))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_enable_auto_backup)) },
                supportingContent = { Text(stringResource(R.string.settings_enable_auto_backup_description)) },
                trailingContent = {
                    Switch(
                        checked = state.autoBackupEnabled,
                        onCheckedChange = { onEvent(SettingsEvent.SetAutoBackupEnabled(it)) }
                    )
                }
            )

            if (state.autoBackupEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_backup_frequency)) },
                    supportingContent = {
                        Column(modifier = Modifier.selectableGroup()) {
                            val options = listOf(
                                stringResource(R.string.settings_backup_frequency_6h) to 6,
                                stringResource(R.string.settings_backup_frequency_12h) to 12,
                                stringResource(R.string.settings_backup_frequency_daily) to 24,
                                stringResource(R.string.settings_backup_frequency_2days) to 48,
                                stringResource(R.string.settings_backup_frequency_weekly) to 168
                            )
                            options.forEach { (label, hours) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = state.autoBackupIntervalHours == hours,
                                            onClick = { onEvent(SettingsEvent.SetAutoBackupInterval(hours)) },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = state.autoBackupIntervalHours == hours,
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

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_backups_to_keep)) },
                    supportingContent = {
                        Column(modifier = Modifier.selectableGroup()) {
                            listOf(3, 5, 10).forEach { count ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = state.autoBackupMaxCount == count,
                                            onClick = { onEvent(SettingsEvent.SetAutoBackupMaxCount(count)) },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = state.autoBackupMaxCount == count,
                                        onClick = null
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_backup_count, count),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                )

                if (state.localBackupFiles.isNotEmpty()) {
                    SectionHeader(title = stringResource(R.string.settings_restore_from_auto))
                    state.localBackupFiles.forEach { fileName ->
                        val isRestoringThisFile = state.restoringBackupFileName == fileName
                        ListItem(
                            headlineContent = { Text(fileName) },
                            trailingContent = {
                                if (isRestoringThisFile) {
                                    CircularProgressIndicator()
                                } else {
                                    OutlinedButton(
                                        enabled = !state.isRestoreInProgress,
                                        onClick = { onEvent(SettingsEvent.RestoreLocalBackup(fileName)) }
                                    ) {
                                        Text(stringResource(R.string.settings_restore_button))
                                    }
                                }
                            }
                        )
                    }
                } else {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_no_auto_backups)) },
                        supportingContent = { Text(stringResource(R.string.settings_no_auto_backups_description)) }
                    )
                }
            }

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_migrate_manga)) },
                supportingContent = { Text(stringResource(R.string.settings_migrate_manga_description)) },
                modifier = Modifier.clickable { onEvent(SettingsEvent.OnNavigateToMigration) }
            )
}

