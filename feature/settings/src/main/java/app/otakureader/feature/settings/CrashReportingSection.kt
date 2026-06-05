@file:Suppress("MaxLineLength")
package app.otakureader.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.otakureader.core.preferences.CrashReportingStore

/**
 * Settings UI for the optional Sentry-backed crash reporting feature (#952).
 *
 * Holds its own minimal state via [remember] + a directly-instantiated [CrashReportingStore]
 * so it doesn't ripple into [SettingsViewModel] (which would force test-mock updates across
 * every existing setting). The store is keystore-backed, so direct instantiation is safe.
 *
 * The DSN field is empty by default and reporting stays off until the user supplies a DSN
 * AND flips the opt-in switch — until then no events leave the device.
 */
@Composable
internal fun CrashReportingSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { CrashReportingStore(context) }

    var dsn by remember { mutableStateOf(store.dsn) }
    var optedIn by remember { mutableStateOf(store.optedIn) }
    var savedHintVisible by remember { mutableStateOf(false) }

    // Clear the "Saved" hint after a moment so it doesn't linger forever on the settings page.
    LaunchedEffect(savedHintVisible) {
        if (savedHintVisible) {
            kotlinx.coroutines.delay(SAVED_HINT_VISIBLE_MS)
            savedHintVisible = false
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(title = stringResource(R.string.settings_crash_reporting))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_crash_opt_in)) },
            supportingContent = {
                Text(
                    text = stringResource(R.string.settings_crash_opt_in_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(
                    checked = optedIn,
                    onCheckedChange = { newValue ->
                        optedIn = newValue
                        store.optedIn = newValue
                        savedHintVisible = true
                    },
                )
            },
        )

        OutlinedTextField(
            value = dsn,
            onValueChange = { dsn = it },
            label = { Text(stringResource(R.string.settings_crash_dsn_label)) },
            placeholder = { Text("https://<key>@<host>/<project>") },
            singleLine = true,
            supportingText = {
                Text(stringResource(R.string.settings_crash_dsn_helper))
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (savedHintVisible) {
                Text(
                    text = stringResource(R.string.settings_crash_saved),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp).align(androidx.compose.ui.Alignment.CenterVertically),
                )
            }
            TextButton(onClick = {
                store.dsn = dsn
                savedHintVisible = true
            }) {
                Text(stringResource(R.string.settings_crash_save_dsn))
            }
        }

        Text(
            text = stringResource(R.string.settings_crash_restart_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private const val SAVED_HINT_VISIBLE_MS = 2_500L
