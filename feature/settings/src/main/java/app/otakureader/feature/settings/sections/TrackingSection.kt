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

fun TrackingSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    // ── Tracking ──────────────────────────────────────────────────────
    SectionHeader(title = stringResource(R.string.settings_tracking))

    if (state.trackers.isEmpty()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_no_tracker_services)) },
            supportingContent = { Text(stringResource(R.string.settings_no_tracker_services_description)) }
        )
        return
    }

    state.trackers.forEach { tracker ->
        var showLogin by remember(tracker.id) { mutableStateOf(false) }
        var username by remember(tracker.id) { mutableStateOf("") }
        var password by remember(tracker.id) { mutableStateOf("") }

        ListItem(
            headlineContent = { Text(tracker.name) },
            supportingContent = {
                if (tracker.isLoggedIn) {
                    Text(stringResource(R.string.settings_tracker_connected), color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(stringResource(R.string.settings_tracker_not_connected))
                }
            },
            trailingContent = {
                if (state.trackingLoginInProgress) {
                    CircularProgressIndicator()
                } else if (tracker.isLoggedIn) {
                    OutlinedButton(onClick = { onEvent(SettingsEvent.LogoutTracker(tracker.id)) }) {
                        Text(stringResource(R.string.settings_tracker_logout))
                    }
                } else {
                    Button(onClick = { showLogin = !showLogin }) {
                        Text(stringResource(R.string.settings_tracker_login))
                    }
                }
            }
        )

        if (showLogin && !tracker.isLoggedIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.settings_tracker_username)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.settings_tracker_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        onEvent(SettingsEvent.LoginTracker(tracker.id, username, password))
                        showLogin = false
                        username = ""
                        password = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_tracker_connect, tracker.name))
                }
            }
        }
    }
}

