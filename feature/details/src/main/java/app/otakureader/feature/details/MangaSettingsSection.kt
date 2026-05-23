@file:Suppress("MaxLineLength")
package app.otakureader.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.otakureader.core.preferences.DeleteAfterReadMode
import app.otakureader.feature.details.R

@Composable
internal fun DeleteAfterReadOption(
    override: DeleteAfterReadMode,
    globalEnabled: Boolean,
    onChange: (DeleteAfterReadMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.details_delete_after_read)) },
            supportingContent = {
                Column(modifier = Modifier.selectableGroup()) {
                    val options = listOf(
                        (if (globalEnabled) {
                            stringResource(R.string.details_delete_follow_global_on)
                        } else {
                            stringResource(R.string.details_delete_follow_global_off)
                        }) to DeleteAfterReadMode.INHERIT,
                        stringResource(R.string.details_delete_on) to DeleteAfterReadMode.ENABLED,
                        stringResource(R.string.details_delete_off) to DeleteAfterReadMode.DISABLED
                    )
                    options.forEach { (label, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = override == value,
                                    onClick = { onChange(value) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = override == value,
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
    }
}

@Composable
internal fun NotificationOption(
    notifyEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.details_notify_title)) },
        supportingContent = {
            Text(
                if (notifyEnabled) stringResource(R.string.details_notify_enabled)
                else stringResource(R.string.details_notify_disabled)
            )
        },
        leadingContent = {
            Icon(
                imageVector = if (notifyEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                contentDescription = if (notifyEnabled) {
                    stringResource(R.string.details_notify_icon_on)
                } else {
                    stringResource(R.string.details_notify_icon_off)
                },
                tint = if (notifyEnabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = notifyEnabled,
                onCheckedChange = { onToggle() }
            )
        },
        modifier = modifier
    )
}

/**
 * Reader settings section for per-manga configuration (#260, #264)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderSettingsSection(
    manga: app.otakureader.domain.model.Manga,
    onEvent: (DetailsContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.details_reader_settings)) },
            supportingContent = {
                val hasOverrides = manga.readerDirection != null ||
                                   manga.readerMode != null ||
                                   manga.readerColorFilter != null ||
                                   manga.readerCustomTintColor != null ||
                                   manga.readerBackgroundColor != null ||
                                   manga.preloadPagesBefore != null ||
                                   manga.preloadPagesAfter != null
                Text(
                    if (hasOverrides) stringResource(R.string.details_reader_custom_applied)
                    else stringResource(R.string.details_reader_default)
                )
            },
            trailingContent = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) {
                            stringResource(R.string.details_collapse)
                        } else {
                            stringResource(R.string.details_expand)
                        }
                    )
                }
            }
        )

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Reading Direction
                Text(
                    text = stringResource(R.string.details_reading_direction),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.selectableGroup()) {
                    DirectionOption(stringResource(R.string.details_direction_ltr), 0, manga.readerDirection, onEvent)
                    DirectionOption(stringResource(R.string.details_direction_rtl), 1, manga.readerDirection, onEvent)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reader Mode
                Text(
                    text = stringResource(R.string.details_reader_mode),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderModeOption(stringResource(R.string.details_mode_single), 0, manga.readerMode, onEvent)
                    ReaderModeOption(stringResource(R.string.details_mode_dual), 1, manga.readerMode, onEvent)
                    ReaderModeOption(stringResource(R.string.details_mode_webtoon), 2, manga.readerMode, onEvent)
                    ReaderModeOption(stringResource(R.string.details_mode_smart), 3, manga.readerMode, onEvent)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Filter
                Text(
                    text = stringResource(R.string.details_color_filter),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorFilterOption(stringResource(R.string.details_filter_none), 0, manga.readerColorFilter, onEvent)
                    ColorFilterOption(stringResource(R.string.details_filter_sepia), 1, manga.readerColorFilter, onEvent)
                    ColorFilterOption(stringResource(R.string.details_filter_greyscale), 2, manga.readerColorFilter, onEvent)
                    ColorFilterOption(stringResource(R.string.details_filter_invert), 3, manga.readerColorFilter, onEvent)
                    ColorFilterOption(stringResource(R.string.details_filter_custom_tint), 4, manga.readerColorFilter, onEvent)
                }

                // Custom Tint Color Picker (shown when Custom Tint is selected)
                if (manga.readerColorFilter == 4) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomTintColorPicker(
                        currentColor = manga.readerCustomTintColor,
                        onColorChange = { onEvent(DetailsContract.Event.SetReaderCustomTintColor(it)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Background Color
                Text(
                    text = stringResource(R.string.details_background_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                BackgroundColorPicker(
                    currentColor = manga.readerBackgroundColor,
                    onColorChange = { onEvent(DetailsContract.Event.SetReaderBackgroundColor(it)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Preload Pages
                Text(
                    text = stringResource(R.string.details_page_preloading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PreloadOption(
                    label = stringResource(R.string.details_pages_before),
                    value = manga.preloadPagesBefore,
                    onChange = { onEvent(DetailsContract.Event.SetPreloadPagesBefore(it)) }
                )
                PreloadOption(
                    label = stringResource(R.string.details_pages_after),
                    value = manga.preloadPagesAfter,
                    onChange = { onEvent(DetailsContract.Event.SetPreloadPagesAfter(it)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Reset button
                TextButton(
                    onClick = {
                        onEvent(DetailsContract.Event.SetReaderDirection(null))
                        onEvent(DetailsContract.Event.SetReaderMode(null))
                        onEvent(DetailsContract.Event.SetReaderColorFilter(null))
                        onEvent(DetailsContract.Event.SetReaderCustomTintColor(null))
                        onEvent(DetailsContract.Event.SetReaderBackgroundColor(null))
                        onEvent(DetailsContract.Event.SetPreloadPagesBefore(null))
                        onEvent(DetailsContract.Event.SetPreloadPagesAfter(null))
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.details_reset_defaults))
                }
            }
        }
    }
}

@Composable
internal fun DirectionOption(
    label: String,
    value: Int,
    currentValue: Int?,
    onEvent: (DetailsContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .selectable(
                selected = currentValue == value,
                onClick = { onEvent(DetailsContract.Event.SetReaderDirection(value)) },
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        RadioButton(
            selected = currentValue == value,
            onClick = null
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
internal fun PreloadOption(
    label: String,
    value: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val decrementDesc = stringResource(R.string.details_stepper_decrement_description)
    val incrementDesc = stringResource(R.string.details_stepper_increment_description)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { onChange((value ?: 0).coerceAtLeast(0) - 1) },
                enabled = (value ?: 0) > 0,
                modifier = Modifier.semantics { contentDescription = decrementDesc }
            ) {
                Text(stringResource(R.string.details_stepper_decrement))
            }
            Text(
                text = (value ?: 0).toString(),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            TextButton(
                onClick = { onChange((value ?: 0).coerceAtMost(9) + 1) },
                enabled = (value ?: 0) < 10,
                modifier = Modifier.semantics { contentDescription = incrementDesc }
            ) {
                Text(stringResource(R.string.details_stepper_increment))
            }
        }
    }
}

@Composable
internal fun ReaderModeOption(
    label: String,
    value: Int,
    currentValue: Int?,
    onEvent: (DetailsContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = currentValue == value,
        onClick = { onEvent(DetailsContract.Event.SetReaderMode(value)) },
        label = { Text(label) },
        modifier = modifier
    )
}

@Composable
internal fun ColorFilterOption(
    label: String,
    value: Int,
    currentValue: Int?,
    onEvent: (DetailsContract.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = currentValue == value,
        onClick = { onEvent(DetailsContract.Event.SetReaderColorFilter(value)) },
        label = { Text(label) },
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomTintColorPicker(
    currentColor: Long?,
    onColorChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.details_custom_tint_color),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presetColors = listOf(
                0xFFFF6B6B to stringResource(R.string.details_color_red),
                0xFFFFA500 to stringResource(R.string.details_color_orange),
                0xFFFFD93D to stringResource(R.string.details_color_yellow),
                0xFF6BCB77 to stringResource(R.string.details_color_green),
                0xFF4D96FF to stringResource(R.string.details_color_blue),
                0xFF9D84B7 to stringResource(R.string.details_color_purple),
                0xFFFFB6C1 to stringResource(R.string.details_color_pink)
            )
            presetColors.forEach { (color, name) ->
                ColorChip(
                    color = color,
                    label = name,
                    selected = currentColor == color,
                    onClick = { onColorChange(color) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = { onColorChange(null) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.details_reset))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BackgroundColorPicker(
    currentColor: Long?,
    onColorChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.details_override_background),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presetColors = listOf(
                0xFF000000 to stringResource(R.string.details_bg_black),
                0xFF1A1A1A to stringResource(R.string.details_bg_dark_gray),
                0xFF808080 to stringResource(R.string.details_bg_gray),
                0xFFFFFFFF to stringResource(R.string.details_bg_white),
                0xFFFFF8DC to stringResource(R.string.details_bg_cream)
            )
            presetColors.forEach { (color, name) ->
                ColorChip(
                    color = color,
                    label = name,
                    selected = currentColor == color,
                    onClick = { onColorChange(color) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = { onColorChange(null) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.details_reset))
        }
    }
}

@Composable
internal fun ColorChip(
    color: Long,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = Color(color),
                        shape = CircleShape
                    )
            )
        },
        modifier = modifier
    )
}
