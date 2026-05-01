package app.otakureader.feature.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

/**
 * Settings hub screen — shows top-level settings categories as navigable list items.
 *
 * The six main categories (Appearance, Library, Reader, Downloads, Tracking, Backup & Restore)
 * each navigate to their own dedicated sub-screen backed by a focused ViewModel.
 *
 * Smaller sections that are handled directly by the main [SettingsViewModel] (local source,
 * reading goals, data management, migration settings, about) remain inline here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToMigrationEntry: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToReader: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToTracking: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToDiscord: () -> Unit = {},
    onNavigateToWidgetConfiguration: () -> Unit = {},
    onNavigateToLocalSourceBrowser: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                SettingsEffect.NavigateToAbout -> onNavigateToAbout()
                SettingsEffect.NavigateToMigrationEntry -> onNavigateToMigrationEntry()
                else -> Unit // other effects are handled inside their respective sub-screens
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            // ── Sub-screen navigation categories ──────────────────────
            SettingsCategoryRow(
                title = stringResource(R.string.settings_appearance),
                onClick = onNavigateToAppearance,
            )
            SettingsCategoryRow(
                title = stringResource(R.string.settings_library),
                onClick = onNavigateToLibrary,
            )
            SettingsCategoryRow(
                title = stringResource(R.string.settings_reader),
                onClick = onNavigateToReader,
            )
            SettingsCategoryRow(
                title = stringResource(R.string.settings_downloads),
                onClick = onNavigateToDownloads,
            )
            SettingsCategoryRow(
                title = stringResource(R.string.settings_tracking),
                onClick = onNavigateToTracking,
            )
            SettingsCategoryRow(
                title = stringResource(R.string.settings_backup),
                onClick = onNavigateToBackup,
            )
            SettingsCategoryRow(
                title = stringResource(R.string.settings_discord),
                onClick = onNavigateToDiscord,
            )
            SettingsCategoryRow(
                title = stringResource(R.string.settings_widgets),
                onClick = onNavigateToWidgetConfiguration,
            )
            SettingsCategoryRow(
                title = stringResource(R.string.settings_local_source),
                onClick = onNavigateToLocalSourceBrowser,
            )

            // ── Local source ──────────────────────────────────────────
            HorizontalDivider()
            LocalSourceSection(state = state, onEvent = viewModel::onEvent)

            // ── Reading goals ─────────────────────────────────────────
            HorizontalDivider()
            ReadingGoalsSection(state = state, onEvent = viewModel::onEvent)

            // ── Data management ───────────────────────────────────────
            HorizontalDivider()
            DataManagementSection(state = state, onEvent = viewModel::onEvent)

            // ── Migration settings ────────────────────────────────────
            HorizontalDivider()
            MigrationSection(state = state, onEvent = viewModel::onEvent)

            // ── About ─────────────────────────────────────────────────
            HorizontalDivider()
            AboutSection(onEvent = viewModel::onEvent)
        }
    }
}

// ─── Shared utility (internal so sub-screen files in this module can use it) ─

@Composable
internal fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

// ─── Private composables for inline sections ──────────────────────────────────

@Composable
private fun SettingsCategoryRow(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun LocalSourceSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(title = stringResource(R.string.settings_local_source))

    var directoryText by remember(state.localSourceDirectory) {
        mutableStateOf(state.localSourceDirectory)
    }

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_scan_directory)) },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(R.string.settings_scan_directory_description),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = directoryText,
                    onValueChange = { directoryText = it },
                    label = { Text(stringResource(R.string.settings_directory_path)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        androidx.compose.material3.Button(
                            onClick = { onEvent(SettingsEvent.SetLocalSourceDirectory(directoryText)) },
                        ) {
                            Text(stringResource(R.string.settings_save))
                        }
                    },
                )
                Text(
                    text = stringResource(R.string.settings_scan_directory_supported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
    )
}

@Composable
private fun ReadingGoalsSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(title = stringResource(R.string.settings_reading_goals))

    // Daily chapter goal
    var dailyGoalSlider by remember { mutableFloatStateOf(state.dailyChapterGoal.toFloat()) }
    LaunchedEffect(state.dailyChapterGoal) {
        dailyGoalSlider = state.dailyChapterGoal.toFloat()
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_daily_chapter_goal)) },
        supportingContent = {
            Column {
                Text(
                    if (dailyGoalSlider.roundToInt() == 0) {
                        stringResource(R.string.settings_goals_disabled)
                    } else {
                        stringResource(R.string.settings_goals_chapters_per_day, dailyGoalSlider.roundToInt())
                    },
                )
                Slider(
                    value = dailyGoalSlider,
                    onValueChange = { dailyGoalSlider = it },
                    onValueChangeFinished = {
                        onEvent(SettingsEvent.SetDailyChapterGoal(dailyGoalSlider.roundToInt()))
                    },
                    valueRange = 0f..20f,
                    steps = 19,
                )
            }
        },
    )

    // Weekly chapter goal
    var weeklyGoalSlider by remember { mutableFloatStateOf(state.weeklyChapterGoal.toFloat()) }
    LaunchedEffect(state.weeklyChapterGoal) {
        weeklyGoalSlider = state.weeklyChapterGoal.toFloat()
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_weekly_chapter_goal)) },
        supportingContent = {
            Column {
                Text(
                    if (weeklyGoalSlider.roundToInt() == 0) {
                        stringResource(R.string.settings_goals_disabled)
                    } else {
                        stringResource(R.string.settings_goals_chapters_per_week, weeklyGoalSlider.roundToInt())
                    },
                )
                Slider(
                    value = weeklyGoalSlider,
                    onValueChange = { weeklyGoalSlider = it },
                    onValueChangeFinished = {
                        onEvent(SettingsEvent.SetWeeklyChapterGoal(weeklyGoalSlider.roundToInt()))
                    },
                    valueRange = 0f..50f,
                    steps = 49,
                )
            }
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_reading_reminders)) },
        supportingContent = { Text(stringResource(R.string.settings_reading_reminders_description)) },
        trailingContent = {
            Switch(
                checked = state.readingRemindersEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetReadingRemindersEnabled(it)) },
            )
        },
    )

    if (state.readingRemindersEnabled) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_reminder_time)) },
            supportingContent = {
                Column(modifier = Modifier.selectableGroup()) {
                    val hours = listOf(
                        stringResource(R.string.settings_reminder_morning) to 9,
                        stringResource(R.string.settings_reminder_afternoon) to 14,
                        stringResource(R.string.settings_reminder_evening) to 20,
                    )
                    hours.forEach { (label, hour) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.readingReminderHour == hour,
                                    onClick = { onEvent(SettingsEvent.SetReadingReminderHour(hour)) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(
                                selected = state.readingReminderHour == hour,
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

@Composable
private fun DataManagementSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(title = stringResource(R.string.settings_data_management))

    // Image disk cache size
    val diskCacheSteps = listOf(64, 128, 256, 512, 1024, 2048)
    val currentDiskCacheIdx = diskCacheSteps.indexOfFirst { it >= state.coilDiskCacheSizeMb }
        .takeIf { it >= 0 } ?: (diskCacheSteps.size - 1)
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_image_cache_size)) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.settings_image_cache_size_desc, diskCacheSteps[currentDiskCacheIdx]))
                Slider(
                    value = currentDiskCacheIdx.toFloat(),
                    onValueChange = { idx ->
                        onEvent(SettingsEvent.SetCoilDiskCacheSizeMb(diskCacheSteps[idx.toInt()]))
                    },
                    valueRange = 0f..(diskCacheSteps.size - 1).toFloat(),
                    steps = diskCacheSteps.size - 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_clear_image_cache)) },
        supportingContent = { Text(stringResource(R.string.settings_clear_image_cache_desc)) },
        trailingContent = {
            OutlinedButton(onClick = { onEvent(SettingsEvent.ClearImageCache) }) {
                Text(stringResource(R.string.settings_clear_button))
            }
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_clear_history)) },
        supportingContent = { Text(stringResource(R.string.settings_clear_history_desc)) },
        trailingContent = {
            OutlinedButton(
                onClick = { onEvent(SettingsEvent.ClearHistory) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.settings_clear_button))
            }
        },
    )

    // Navigate to source migration wizard
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_migrate_manga)) },
        supportingContent = { Text(stringResource(R.string.settings_migrate_manga_description)) },
        modifier = Modifier.clickable { onEvent(SettingsEvent.OnNavigateToMigration) },
    )
}

@Composable
private fun MigrationSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(title = stringResource(R.string.settings_migration))

    var thresholdSlider by remember(state.migrationSimilarityThreshold) {
        mutableFloatStateOf(state.migrationSimilarityThreshold)
    }
    ListItem(
        headlineContent = {
            Text(stringResource(R.string.settings_similarity_threshold, (thresholdSlider * 100).roundToInt()))
        },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(R.string.settings_similarity_threshold_description),
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = thresholdSlider,
                    onValueChange = { thresholdSlider = it },
                    onValueChangeFinished = {
                        onEvent(SettingsEvent.SetMigrationSimilarityThreshold(thresholdSlider))
                    },
                    valueRange = 0.5f..1.0f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_always_show_confirmation)) },
        supportingContent = { Text(stringResource(R.string.settings_always_show_confirmation_description)) },
        trailingContent = {
            Switch(
                checked = state.migrationAlwaysConfirm,
                onCheckedChange = { onEvent(SettingsEvent.SetMigrationAlwaysConfirm(it)) },
            )
        },
    )

    var minChaptersSlider by remember(state.migrationMinChapterCount) {
        mutableFloatStateOf(state.migrationMinChapterCount.toFloat())
    }
    ListItem(
        headlineContent = {
            Text(
                if (minChaptersSlider.roundToInt() == 0) {
                    stringResource(R.string.settings_min_chapter_count_no_filter)
                } else {
                    stringResource(R.string.settings_min_chapter_count, minChaptersSlider.roundToInt())
                },
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(R.string.settings_min_chapter_count_description),
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = minChaptersSlider,
                    onValueChange = { minChaptersSlider = it },
                    onValueChangeFinished = {
                        onEvent(SettingsEvent.SetMigrationMinChapterCount(minChaptersSlider.roundToInt()))
                    },
                    valueRange = 0f..50f,
                    steps = 49,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun AboutSection(onEvent: (SettingsEvent) -> Unit) {
    SectionHeader(title = stringResource(R.string.settings_about))
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_about_title)) },
        supportingContent = { Text(stringResource(R.string.settings_about_description)) },
        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
        modifier = Modifier.clickable { onEvent(SettingsEvent.NavigateToAbout) },
    )
}
