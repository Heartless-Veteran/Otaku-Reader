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

fun DiscordSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    // ── Discord ───────────────────────────────────────────────────────
    SectionHeader(title = stringResource(R.string.settings_discord))

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_discord_rich_presence)) },
        supportingContent = { Text(stringResource(R.string.settings_discord_rich_presence_description)) },
        trailingContent = {
            Switch(
                checked = state.discordRpcEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetDiscordRpcEnabled(it)) }
            )
        }
    )
}

