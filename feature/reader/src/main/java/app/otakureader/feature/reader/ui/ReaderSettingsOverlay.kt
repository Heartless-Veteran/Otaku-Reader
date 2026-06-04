package app.otakureader.feature.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.otakureader.domain.model.ColorFilterMode
import app.otakureader.domain.model.ReaderMode
import app.otakureader.domain.model.ReadingDirection
import app.otakureader.feature.reader.R
import app.otakureader.feature.reader.ReaderEvent
import app.otakureader.feature.reader.ReaderSetting
import app.otakureader.feature.reader.ReaderState

/**
 * Quick-settings overlay panel — slide-in from the right with the most common
 * reader settings that users change during reading. Matches the settings sheet
 * in Komikku/Mihon (e.g., TachiyomiSY).
 *
 * Sections: Reading Mode, Display, Navigation, Filters, Webtoon, Actions
 */
@Composable
fun ReaderSettingsOverlay(
    state: ReaderState,
    onEvent: (ReaderEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .width(340.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reader_settings_title),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24),
                        contentDescription = stringResource(R.string.reader_settings_close)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSection(title = stringResource(R.string.reader_settings_mode)) {
                ModeSelector(
                    current = state.mode,
                    onSelect = { onEvent(ReaderEvent.OnModeChange(it)) }
                )
                DirectionSelector(
                    current = state.readingDirection,
                    onSelect = { onEvent(ReaderEvent.OnDirectionChange(it)) }
                )
            }

            SettingsSection(title = stringResource(R.string.reader_settings_display)) {
                ToggleRow(
                    label = stringResource(R.string.reader_settings_keep_screen_on),
                    checked = state.keepScreenOn,
                    onToggle = { onEvent(ReaderEvent.ToggleSetting(ReaderSetting.KEEP_SCREEN_ON)) }
                )
                ToggleRow(
                    label = stringResource(R.string.reader_settings_show_page_number),
                    checked = state.showPageNumber,
                    onToggle = { onEvent(ReaderEvent.ToggleSetting(ReaderSetting.SHOW_PAGE_NUMBER)) }
                )
                ToggleRow(
                    label = stringResource(R.string.reader_settings_crop_borders),
                    checked = state.cropBordersEnabled,
                    onToggle = { onEvent(ReaderEvent.ToggleSetting(ReaderSetting.CROP_BORDERS)) }
                )
                ToggleRow(
                    label = stringResource(R.string.reader_settings_double_tap_zoom),
                    checked = state.doubleTapZoomEnabled,
                    onToggle = { onEvent(ReaderEvent.ToggleSetting(ReaderSetting.DOUBLE_TAP_ZOOM)) }
                )
                BrightnessSlider(
                    value = state.brightness,
                    onChange = { onEvent(ReaderEvent.OnBrightnessChange(it)) }
                )
            }

            SettingsSection(title = stringResource(R.string.reader_settings_navigation)) {
                ToggleRow(
                    label = stringResource(R.string.reader_settings_volume_keys),
                    checked = state.volumeKeysEnabled,
                    onToggle = { onEvent(ReaderEvent.ToggleSetting(ReaderSetting.VOLUME_KEY_NAVIGATION)) }
                )
                ToggleRow(
                    label = stringResource(R.string.reader_settings_invert_volume_keys),
                    checked = state.volumeKeysInverted,
                    onToggle = { onEvent(ReaderEvent.ToggleSetting(ReaderSetting.VOLUME_KEYS_INVERTED)) }
                )
                ToggleRow(
                    label = stringResource(R.string.reader_settings_skip_read),
                    checked = state.skipReadChapters,
                    onToggle = { onEvent(ReaderEvent.ToggleSetting(ReaderSetting.SKIP_READ_CHAPTERS)) }
                )
            }

            SettingsSection(title = stringResource(R.string.reader_settings_filter)) {
                ColorFilterSelector(
                    current = state.colorFilterMode,
                    onSelect = { onEvent(ReaderEvent.SetColorFilterMode(it)) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        content()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun ModeSelector(
    current: ReaderMode,
    onSelect: (ReaderMode) -> Unit
) {
    val modes = listOf(
        ReaderMode.SINGLE_PAGE to R.string.reader_mode_single,
        ReaderMode.DUAL_PAGE to R.string.reader_mode_dual,
        ReaderMode.WEBTOON to R.string.reader_mode_webtoon,
        ReaderMode.SMART_PANELS to R.string.reader_mode_smart_panels,
    )
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        modes.forEach { (mode, labelRes) ->
            val selected = mode == current
            Card(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .clickable { onSelect(mode) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DirectionSelector(
    current: ReadingDirection,
    onSelect: (ReadingDirection) -> Unit
) {
    val directions = listOf(
        ReadingDirection.LTR to R.string.reader_direction_ltr,
        ReadingDirection.RTL to R.string.reader_direction_rtl,
        ReadingDirection.VERTICAL to R.string.reader_direction_vertical,
    )
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        directions.forEach { (dir, labelRes) ->
            val selected = dir == current
            Card(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .clickable { onSelect(dir) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BrightnessSlider(
    value: Float,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.reader_settings_brightness, (value * 100).toInt()),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0.1f..1.5f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColorFilterSelector(
    current: ColorFilterMode,
    onSelect: (ColorFilterMode) -> Unit
) {
    val filters = listOf(
        ColorFilterMode.NONE to R.string.color_filter_none,
        ColorFilterMode.INVERT to R.string.color_filter_invert,
        ColorFilterMode.GRAYSCALE to R.string.color_filter_grayscale,
        ColorFilterMode.SEPIA to R.string.color_filter_sepia,
        ColorFilterMode.BLUE_LIGHT to R.string.color_filter_blue_light,
        ColorFilterMode.CUSTOM_TINT to R.string.color_filter_custom_tint,
    )
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        filters.forEach { (mode, labelRes) ->
            val selected = mode == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mode) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        if (selected) R.drawable.ic_check_circle_24
                        else R.drawable.ic_circle_outline_24
                    ),
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
