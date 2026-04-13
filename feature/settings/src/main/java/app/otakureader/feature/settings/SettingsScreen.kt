package app.otakureader.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.otakureader.feature.settings.sections.AboutSection
import app.otakureader.feature.settings.sections.AiSection
import app.otakureader.feature.settings.sections.AppearanceSection
import app.otakureader.feature.settings.sections.BrowseSection
import app.otakureader.feature.settings.sections.DataStorageSection
import app.otakureader.feature.settings.sections.DiscordSection
import app.otakureader.feature.settings.sections.DownloadSection
import app.otakureader.feature.settings.sections.DownloadsSettingsSection
import app.otakureader.feature.settings.sections.LibrarySection
import app.otakureader.feature.settings.sections.LocalSourceSection
import app.otakureader.feature.settings.sections.MigrationSection
import app.otakureader.feature.settings.sections.NotificationsSection
import app.otakureader.feature.settings.sections.ReadingGoalsSection
import app.otakureader.feature.settings.sections.ReaderSection
import app.otakureader.feature.settings.sections.TrackingSection

/**
 * Settings screen for user-configurable options.
 * Built with Jetpack Compose as specified in the project architecture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMigrationEntry: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // File picker for creating backup
    val backupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.createBackup(it) }
    }

    // File picker for restoring backup
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onEvent(
            SettingsEvent.NotificationPermissionResult(
                isGranted = isGranted,
                shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.let { _ ->
                        // This should be checked before requesting, but we update state here
                        false
                    }
                } else {
                    false
                }
            )
        )
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SettingsEffect.LaunchBackupPicker -> {
                    backupFileLauncher.launch("otaku_reader_backup.json")
                }
                is SettingsEffect.LaunchRestorePicker -> {
                    restoreFileLauncher.launch(arrayOf("application/json"))
                }
                is SettingsEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                is SettingsEffect.NavigateToAppNotificationSettings -> {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
                is SettingsEffect.NavigateToMigrationEntry -> {
                    onNavigateToMigrationEntry()
                }
                is SettingsEffect.NavigateToAbout -> {
                    onNavigateToAbout()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        SettingsContent(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
    ) {
        AppearanceSection(state = state, onEvent = onEvent)
        LibrarySection(state = state, onEvent = onEvent)
        BrowseSection(state = state, onEvent = onEvent)
        DownloadsSettingsSection(state = state, onEvent = onEvent)
        ReaderSection(state = state, onEvent = onEvent)
        DownloadSection(state = state, onEvent = onEvent)
        NotificationsSection(state = state, onEvent = onEvent)
        ReadingGoalsSection(state = state, onEvent = onEvent)
        DataStorageSection(state = state, onEvent = onEvent)
        LocalSourceSection(state = state, onEvent = onEvent)
        TrackingSection(state = state, onEvent = onEvent)
        MigrationSection(state = state, onEvent = onEvent)
        DiscordSection(state = state, onEvent = onEvent)
        AiSection(state = state, onEvent = onEvent)
        AboutSection(onEvent = onEvent)
    }
}
