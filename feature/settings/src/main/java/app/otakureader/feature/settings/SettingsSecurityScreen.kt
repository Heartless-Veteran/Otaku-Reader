package app.otakureader.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route

/**
 * Security settings: optional biometric / device-credential app lock and its grace period.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSecurityScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_security)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_biometric_lock)) },
                supportingContent = { Text(stringResource(R.string.settings_biometric_lock_description)) },
                trailingContent = {
                    Switch(
                        checked = state.biometricLockEnabled,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.SetBiometricLockEnabled(it)) },
                    )
                },
            )

            if (state.biometricLockEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_lock_timeout)) },
                    supportingContent = {
                        Column(modifier = Modifier.selectableGroup()) {
                            val options = listOf(
                                stringResource(R.string.settings_lock_timeout_immediate) to 0,
                                stringResource(R.string.settings_lock_timeout_1min) to 1,
                                stringResource(R.string.settings_lock_timeout_5min) to 5,
                                stringResource(R.string.settings_lock_timeout_15min) to 15,
                            )
                            options.forEach { (label, minutes) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = state.biometricLockTimeoutMinutes == minutes,
                                            onClick = { viewModel.onEvent(SettingsEvent.SetBiometricLockTimeout(minutes)) },
                                            role = Role.RadioButton,
                                        )
                                        .padding(vertical = 4.dp),
                                ) {
                                    RadioButton(
                                        selected = state.biometricLockTimeoutMinutes == minutes,
                                        onClick = null,
                                    )
                                    Text(text = label, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

fun NavGraphBuilder.settingsSecurityScreen(onNavigateBack: () -> Unit) {
    composable<Route.SettingsSecurity> {
        SettingsSecurityScreen(onNavigateBack = onNavigateBack)
    }
}
