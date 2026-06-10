@file:Suppress("MaxLineLength")
package app.otakureader.feature.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.otakureader.core.extension.domain.model.Extension
import app.otakureader.core.extension.domain.model.InstallStatus
import app.otakureader.core.ui.component.ErrorScreen
import app.otakureader.core.ui.component.LoadingScreen
import app.otakureader.feature.browse.R
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsBottomSheet(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRepositories: () -> Unit = {},
    onNavigateToExtensionDetail: (packageName: String) -> Unit = {},
    viewModel: ExtensionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ExtensionsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is ExtensionsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize()
    ) {
        ExtensionsContent(
            state = state,
            onEvent = viewModel::onEvent,
            snackbarHostState = snackbarHostState,
            onClose = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            },
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToRepositories = onNavigateToRepositories,
            onNavigateToExtensionDetail = onNavigateToExtensionDetail,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionsContent(
    state: ExtensionsState,
    onEvent: (ExtensionsEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRepositories: () -> Unit = {},
    onNavigateToExtensionDetail: (packageName: String) -> Unit = {},
) {
    val selectedTab = state.selectedTab
    val tabs = listOf("Installed", "Available", "Updates")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.extensions_sheet_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.extensions_close))
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(ExtensionsEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.extensions_refresh))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.extensions_settings))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            TextField(
                value = state.searchQuery,
                onValueChange = { onEvent(ExtensionsEvent.OnSearchQueryChange(it)) },
                placeholder = { Text(stringResource(R.string.extensions_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.extensions_search_cd)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter & Sort controls
            FilterAndSortRow(
                showNsfw = state.showNsfw,
                sortMode = state.sortMode,
                onToggleNsfw = { onEvent(ExtensionsEvent.ToggleNsfw(it)) },
                onSetSortMode = { onEvent(ExtensionsEvent.SetSortMode(it)) }
            )

            // Update All button (only in Updates tab, visible whenever updates exist)
            if (selectedTab == 2 && state.updateCount > 0) {
                UpdateAllButton(
                    updateCount = state.updateCount,
                    isUpdating = state.isUpdatingAll,
                    onUpdateAll = { onEvent(ExtensionsEvent.UpdateAllExtensions) }
                )
            }

            RepositoryManager(
                repositories = state.repositories,
                onAdd = { onEvent(ExtensionsEvent.AddRepository(it)) },
                onRemove = { onEvent(ExtensionsEvent.RemoveRepository(it)) },
                onOpenFullManager = onNavigateToRepositories,
            )

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onEvent(ExtensionsEvent.SelectTab(index)) },
                        text = {
                            when (index) {
                                2 -> if (state.updateCount > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(title)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(
                                                text = state.updateCount.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                } else Text(title)
                                else -> Text(title)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ExtensionsList(
                    extensions = state.installedExtensions,
                    isLoading = state.isLoading,
                    error = state.error,
                    onInstall = { /* Already installed */ },
                    onUninstall = { onEvent(ExtensionsEvent.UninstallExtension(it)) },
                    onUpdate = { onEvent(ExtensionsEvent.UpdateExtension(it)) },
                    onToggleEnabled = { ext, enabled ->
                        onEvent(ExtensionsEvent.ToggleExtensionEnabled(ext, enabled))
                    },
                    onRefresh = { onEvent(ExtensionsEvent.Refresh) },
                    onViewDetails = { onNavigateToExtensionDetail(it.pkgName) }
                )
                1 -> Column {
                    if (state.hasUnverifiedExtensions) {
                        TrustBanner(visible = true)
                    }
                    ExtensionsList(
                        extensions = state.availableExtensions,
                        isLoading = state.isLoading,
                        error = state.error,
                        onInstall = { onEvent(ExtensionsEvent.InstallExtension(it)) },
                        onUninstall = { /* Not installed */ },
                        onUpdate = { /* No update */ },
                        onToggleEnabled = { _, _ -> },
                        onRefresh = { onEvent(ExtensionsEvent.Refresh) },
                        onViewDetails = { onNavigateToExtensionDetail(it.pkgName) }
                    )
                }
                2 -> ExtensionsList(
                    extensions = state.extensionsWithUpdates,
                    isLoading = state.isLoading,
                    error = state.error,
                    onInstall = { /* Already installed */ },
                    onUninstall = { onEvent(ExtensionsEvent.UninstallExtension(it)) },
                    onUpdate = { onEvent(ExtensionsEvent.UpdateExtension(it)) },
                    onToggleEnabled = { ext, enabled ->
                        onEvent(ExtensionsEvent.ToggleExtensionEnabled(ext, enabled))
                    },
                    onRefresh = { onEvent(ExtensionsEvent.Refresh) },
                    onViewDetails = { onNavigateToExtensionDetail(it.pkgName) }
                )
            }

            // Unverified install confirmation dialog
            if (state.showUnverifiedInstallDialog) {
                UnverifiedInstallDialog(
                    extension = state.pendingUnverifiedExtension,
                    onDismiss = { onEvent(ExtensionsEvent.DismissUnverifiedDialog) },
                    onConfirm = { onEvent(ExtensionsEvent.ConfirmUnverifiedInstall) }
                )
            }
        }
    }
}

@Composable
private fun ExtensionsList(
    extensions: List<Extension>,
    isLoading: Boolean,
    error: String?,
    onInstall: (Extension) -> Unit,
    onUninstall: (Extension) -> Unit,
    onUpdate: (Extension) -> Unit,
    onToggleEnabled: (Extension, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onViewDetails: (Extension) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> LoadingScreen(modifier = modifier)
        error != null -> ErrorScreen(
            message = error,
            onRetry = onRefresh,
            modifier = modifier
        )
        extensions.isEmpty() -> EmptyExtensionsView(modifier = modifier)
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(extensions, key = { it.id }) { extension ->
                ExtensionItem(
                    extension = extension,
                    onInstall = { onInstall(extension) },
                    onUninstall = { onUninstall(extension) },
                    onUpdate = { onUpdate(extension) },
                    onToggleEnabled = { enabled -> onToggleEnabled(extension, enabled) },
                    onViewDetails = { onViewDetails(extension) }
                )
            }
        }
    }
}

@Composable
private fun EmptyExtensionsView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No extensions found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExtensionItem(
    extension: Extension,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onUpdate: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onViewDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Extension icon
                AsyncImage(
                    model = extension.iconUrl,
                    contentDescription = extension.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = extension.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "v${extension.versionName} • ${extension.lang.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (extension.sources.isNotEmpty()) {
                        Text(
                            text = "${extension.sources.size} source(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (extension.signatureHash != null) "Trusted" else "Unverified",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (extension.signatureHash != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                // Capability badges: shown only when the flag is true so the row is invisible
                // for most extensions and adds zero visual noise.
                ExtensionCapabilityBadges(extension = extension)
            }

            // Action buttons based on status
            when (extension.status) {
                InstallStatus.INSTALLED -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Switch(
                            checked = extension.isEnabled,
                            onCheckedChange = onToggleEnabled
                        )
                        IconButton(onClick = onUninstall) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.extensions_uninstall))
                        }
                    }
                }
                InstallStatus.HAS_UPDATE -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Switch(
                            checked = extension.isEnabled,
                            onCheckedChange = onToggleEnabled
                        )
                        IconButton(onClick = onUpdate) {
                            Icon(
                                Icons.Default.Update,
                                contentDescription = stringResource(R.string.extensions_update),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                InstallStatus.AVAILABLE -> {
                    IconButton(onClick = onInstall) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = stringResource(R.string.extensions_install),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                InstallStatus.INSTALLING, InstallStatus.UPDATING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    // Error or other states - show install button
                    IconButton(onClick = onInstall) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.extensions_install))
                    }
                }
            }

            // Details link — always visible at the bottom of the card
            TextButton(
                onClick = onViewDetails,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 8.dp, bottom = 4.dp)
            ) {
                Text(stringResource(R.string.extension_detail_view_details))
            }
        }
    }
}

/**
 * Displays glanceable capability badges for an extension.
 *
 * - Amber "CF" chip: extension requires Cloudflare bypass (any of its sources has hasCloudflare).
 * - Blue "README" chip: extension has README documentation in its repository.
 * - Green "CHANGELOG" chip: extension has a changelog in its repository.
 *
 * The Row is only shown when at least one badge is visible; otherwise it collapses to nothing,
 * so extensions without any of these flags incur zero layout cost.
 */
@Composable
private fun ExtensionCapabilityBadges(
    extension: Extension,
    modifier: Modifier = Modifier
) {
    val hasAny = extension.hasCloudflare || extension.hasReadme || extension.hasChangelog
    if (!hasAny) return

    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (extension.hasCloudflare) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = stringResource(R.string.extension_has_cloudflare),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    labelColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
        if (extension.hasReadme) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = stringResource(R.string.extension_has_readme),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
        if (extension.hasChangelog) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = stringResource(R.string.extension_has_changelog),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }
    }
}

@Composable
private fun FilterAndSortRow(
    showNsfw: Boolean,
    sortMode: SortMode,
    onToggleNsfw: (Boolean) -> Unit,
    onSetSortMode: (SortMode) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // NSFW Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onToggleNsfw(!showNsfw) }
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.extensions_icon_cd),
                tint = if (showNsfw) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.extensions_nsfw_label),
                style = MaterialTheme.typography.labelMedium,
                color = if (showNsfw) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = showNsfw,
                onCheckedChange = onToggleNsfw,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sort dropdown
        Box {
            TextButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = stringResource(R.string.extensions_language_cd),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (sortMode) {
                        SortMode.NAME -> stringResource(R.string.extensions_sort_name)
                        SortMode.RECENTLY_ADDED -> stringResource(R.string.extensions_sort_recently_added)
                        SortMode.LANGUAGE -> stringResource(R.string.extensions_sort_language)
                    }
                )
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.extensions_sort_name)) },
                    onClick = {
                        onSetSortMode(SortMode.NAME)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (sortMode == SortMode.NAME) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.extensions_installed_cd))
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.extensions_sort_recently_added)) },
                    onClick = {
                        onSetSortMode(SortMode.RECENTLY_ADDED)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (sortMode == SortMode.RECENTLY_ADDED) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.extensions_installed_cd))
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.extensions_sort_language)) },
                    onClick = {
                        onSetSortMode(SortMode.LANGUAGE)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (sortMode == SortMode.LANGUAGE) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.extensions_installed_cd))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UpdateAllButton(
    updateCount: Int,
    isUpdating: Boolean,
    onUpdateAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(enabled = !isUpdating) { onUpdateAll() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = stringResource(R.string.extensions_language_flag_cd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isUpdating) {
                            stringResource(R.string.extensions_updating_all)
                        } else {
                            stringResource(R.string.extensions_update_all)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = pluralStringResource(R.plurals.extensions_updates_available, updateCount, updateCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isUpdating) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = updateCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryManager(
    repositories: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpenFullManager: () -> Unit = {},
) {
    var repoInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(R.string.extensions_repositories_title), style = MaterialTheme.typography.titleMedium)

        repositories.forEach { repo ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = repo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(repo) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.extensions_remove_repo))
                }
            }
        }

        OutlinedTextField(
            value = repoInput,
            onValueChange = { repoInput = it },
            placeholder = { Text(stringResource(R.string.extensions_repo_url_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onOpenFullManager) {
                Text(stringResource(R.string.extensions_manage_repositories))
            }
            TextButton(
                onClick = {
                    onAdd(repoInput)
                    repoInput = ""
                },
                enabled = repoInput.isNotBlank()
            ) {
                Text(stringResource(R.string.extensions_add_repository))
            }
        }

        HorizontalDivider()
    }
}


@Composable
private fun UnverifiedInstallDialog(
    extension: Extension?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (extension == null) return
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.extension_unverified_title)) },
        text = {
            Column {
                Text(stringResource(R.string.extension_unverified_body))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = extension.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.extension_install_anyway))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.extension_cancel))
            }
        }
    )
}

@Composable
private fun TrustBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.extension_repo_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
