package app.otakureader.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

/**
 * Local source browser and configuration screen.
 *
 * This screen surfaces the existing local-source backend that scans folders, CBZ/ZIP archives,
 * EPUB files, ComicInfo.xml metadata, series.json metadata, and image folders from the configured
 * directory. It intentionally avoids duplicating scanner logic; Browse continues to consume the
 * local source through the normal source APIs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSourceBrowserScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var directoryText by remember { mutableStateOf(state.localSourceDirectory) }

    LaunchedEffect(state.localSourceDirectory) {
        directoryText = state.localSourceDirectory
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_local_source)) },
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
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
        ) {
            item(key = "scan_header") {
                SectionHeader(title = stringResource(R.string.settings_scan_directory))
            }
            item(key = "scan_directory") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_directory_path)) },
                    supportingContent = {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_scan_directory_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            OutlinedTextField(
                                value = directoryText,
                                onValueChange = { directoryText = it },
                                label = { Text(stringResource(R.string.settings_directory_path)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = { viewModel.onEvent(SettingsEvent.SetLocalSourceDirectory(directoryText)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            ) {
                                Text(stringResource(R.string.settings_save))
                            }
                        }
                    },
                )
            }
            item(key = "formats_divider") { HorizontalDivider() }
            item(key = "formats_header") {
                SectionHeader(title = stringResource(R.string.settings_local_source_supported_formats))
            }
            item(key = "format_archives") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_local_source_archives)) },
                    supportingContent = { Text(stringResource(R.string.settings_local_source_archives_description)) },
                )
            }
            item(key = "format_folders") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_local_source_folders)) },
                    supportingContent = { Text(stringResource(R.string.settings_local_source_folders_description)) },
                )
            }
            item(key = "format_metadata") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_local_source_metadata)) },
                    supportingContent = { Text(stringResource(R.string.settings_local_source_metadata_description)) },
                )
            }
        }
                headlineContent = { Text(stringResource(R.string.settings_local_source_metadata)) },
                supportingContent = { Text(stringResource(R.string.settings_local_source_metadata_description)) },
            )
        }
    }
}

fun NavGraphBuilder.localSourceBrowserScreen(onNavigateBack: () -> Unit) {
    composable<Route.LocalSourceBrowser> {
        LocalSourceBrowserScreen(onNavigateBack = onNavigateBack)
    }
}
