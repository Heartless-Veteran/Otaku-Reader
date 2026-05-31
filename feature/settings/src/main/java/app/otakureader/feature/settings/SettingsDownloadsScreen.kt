package app.otakureader.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.settings.viewmodel.DownloadViewModel
import kotlin.math.roundToInt

/**
 * Downloads sub-screen: auto-download, Wi-Fi constraints, concurrent downloads,
 * download location, download-ahead, delete-after-reading, save-as-CBZ.
 * Serviced by [DownloadViewModel] backed by [DownloadSettingsDelegate].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDownloadsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDataUsage: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val downloadLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onEvent(SettingsEvent.SetDownloadLocation(it.toString())) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is SettingsEffect.ShowDownloadLocationPicker -> downloadLocationLauncher.launch(null)
                is SettingsEffect.NavigateToDataUsage -> onNavigateToDataUsage()
                else -> Unit
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_downloads)) },
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
            DownloadsContent(state = state, onEvent = viewModel::onEvent)
        }
    }
}

fun NavGraphBuilder.settingsDownloadsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDataUsage: () -> Unit = {},
) {
    composable<Route.SettingsDownloads> {
        SettingsDownloadsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDataUsage = onNavigateToDataUsage,
        )
    }
}

// ─── Section composable ───────────────────────────────────────────────────────

@Suppress("LongMethod")
@Composable
private fun DownloadsContent(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(title = stringResource(R.string.settings_downloads))

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_auto_download_new_chapters)) },
        supportingContent = { Text(stringResource(R.string.settings_auto_download_new_chapters_description)) },
        trailingContent = {
            Switch(
                checked = state.autoDownloadEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetAutoDownloadEnabled(it)) },
            )
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_download_only_wifi)) },
        supportingContent = { Text(stringResource(R.string.settings_download_only_wifi_description)) },
        trailingContent = {
            Switch(
                checked = state.downloadOnlyOnWifi,
                onCheckedChange = { onEvent(SettingsEvent.SetDownloadOnlyOnWifi(it)) },
            )
        },
    )

    var autoLimitSlider by remember(state.autoDownloadLimit) {
        mutableFloatStateOf(state.autoDownloadLimit.toFloat())
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_auto_download_limit, autoLimitSlider.roundToInt())) },
        supportingContent = {
            Slider(
                value = autoLimitSlider,
                onValueChange = { autoLimitSlider = it },
                onValueChangeFinished = {
                    onEvent(SettingsEvent.SetAutoDownloadLimit(autoLimitSlider.roundToInt()))
                },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )

    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    SectionHeader(title = stringResource(R.string.settings_download_location))

    val locationText = state.downloadLocation?.substringAfterLast("/")
        ?: stringResource(R.string.settings_download_location_default)
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_download_location)) },
        supportingContent = { Text(locationText) },
        trailingContent = {
            OutlinedButton(onClick = { onEvent(SettingsEvent.RequestDownloadLocationPicker) }) {
                Text(stringResource(R.string.settings_change))
            }
        },
        modifier = Modifier.clickable { onEvent(SettingsEvent.RequestDownloadLocationPicker) },
    )

    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    SectionHeader(title = stringResource(R.string.settings_download_performance))

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
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )

    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    SectionHeader(title = stringResource(R.string.settings_download_ahead))

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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_download_ahead_wifi_only)) },
        supportingContent = { Text(stringResource(R.string.settings_download_ahead_wifi_only_description)) },
        trailingContent = {
            Switch(
                checked = state.downloadAheadOnlyOnWifi,
                onCheckedChange = { onEvent(SettingsEvent.SetDownloadAheadOnlyOnWifi(it)) },
            )
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_delete_after_reading)) },
        supportingContent = { Text(stringResource(R.string.settings_delete_after_reading_description)) },
        trailingContent = {
            Switch(
                checked = state.deleteAfterReading,
                onCheckedChange = { onEvent(SettingsEvent.SetDeleteAfterReading(it)) },
            )
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_save_as_cbz)) },
        supportingContent = { Text(stringResource(R.string.settings_save_as_cbz_description)) },
        trailingContent = {
            Switch(
                checked = state.saveAsCbz,
                onCheckedChange = { onEvent(SettingsEvent.SetSaveAsCbz(it)) },
            )
        },
    )

    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    SmartDownloadSection(state = state, onEvent = onEvent)

    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    SectionHeader(title = stringResource(R.string.settings_data_usage))

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_data_saver)) },
        supportingContent = { Text(stringResource(R.string.settings_data_saver_desc)) },
        trailingContent = {
            Switch(
                checked = state.downloadDataSaverEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetDownloadDataSaverEnabled(it)) },
            )
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_data_usage)) },
        modifier = Modifier.clickable { onEvent(SettingsEvent.NavigateToDataUsage) },
    )
}

@Suppress("LongMethod")
@Composable
private fun SmartDownloadSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(title = stringResource(R.string.settings_smart_download))

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_smart_download_enabled)) },
        supportingContent = { Text(stringResource(R.string.settings_smart_download_enabled_description)) },
        trailingContent = {
            Switch(
                checked = state.smartDownloadEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetSmartDownloadEnabled(it)) },
            )
        },
    )

    if (state.smartDownloadEnabled) {
        var aheadSlider by remember(state.smartDownloadChaptersAhead) {
            mutableFloatStateOf(state.smartDownloadChaptersAhead.toFloat())
        }
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.settings_smart_download_chapters_ahead, aheadSlider.roundToInt()))
            },
            supportingContent = {
                Slider(
                    value = aheadSlider,
                    onValueChange = { aheadSlider = it },
                    onValueChangeFinished = {
                        onEvent(SettingsEvent.SetSmartDownloadChaptersAhead(aheadSlider.roundToInt()))
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )

        var thresholdSlider by remember(state.smartDownloadThreshold) {
            mutableFloatStateOf(state.smartDownloadThreshold)
        }
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.settings_smart_download_threshold, (thresholdSlider * 100).roundToInt()))
            },
            supportingContent = {
                Slider(
                    value = thresholdSlider,
                    onValueChange = { thresholdSlider = it },
                    onValueChangeFinished = {
                        onEvent(SettingsEvent.SetSmartDownloadThreshold(thresholdSlider))
                    },
                    valueRange = 0.5f..0.95f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_smart_download_wifi_only)) },
            trailingContent = {
                Switch(
                    checked = state.smartDownloadWifiOnly,
                    onCheckedChange = { onEvent(SettingsEvent.SetSmartDownloadWifiOnly(it)) },
                )
            },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_smart_download_favorites_only)) },
            trailingContent = {
                Switch(
                    checked = state.smartDownloadFavoritesOnly,
                    onCheckedChange = { onEvent(SettingsEvent.SetSmartDownloadFavoritesOnly(it)) },
                )
            },
        )

        var storageSlider by remember(state.smartDownloadMinStorageMb) {
            mutableFloatStateOf(state.smartDownloadMinStorageMb.toFloat())
        }
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.settings_smart_download_min_storage, storageSlider.roundToInt()))
            },
            supportingContent = {
                Slider(
                    value = storageSlider,
                    onValueChange = { storageSlider = it },
                    onValueChangeFinished = {
                        onEvent(SettingsEvent.SetSmartDownloadMinStorageMb(storageSlider.roundToInt()))
                    },
                    valueRange = 100f..2000f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}
