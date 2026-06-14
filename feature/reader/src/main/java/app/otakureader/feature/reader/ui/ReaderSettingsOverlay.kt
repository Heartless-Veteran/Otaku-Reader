package app.otakureader.feature.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.otakureader.domain.model.ColorFilterMode
import app.otakureader.domain.model.ReaderMode
import app.otakureader.domain.model.ReadingDirection
import app.otakureader.feature.reader.R

/**
 * Compact quick-settings overlay triggered by long-pressing the center tap zone.
 *
 * Exposes the four most commonly adjusted in-session settings without requiring
 * the user to open the full [ReaderMenuOverlay]:
 *  - Reading mode (Single / Dual / Webtoon / Smart Panels)
 *  - Reading direction (LTR / RTL / Vertical)
 *  - Brightness
 *  - Color filter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsOverlay(
    isVisible: Boolean,
    currentMode: ReaderMode,
    readingDirection: ReadingDirection,
    brightness: Float,
    colorFilterMode: ColorFilterMode,
    cropBordersEnabled: Boolean,
    incognitoMode: Boolean,
    onModeChange: (ReaderMode) -> Unit,
    onDirectionChange: (ReadingDirection) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onColorFilterChange: (ColorFilterMode) -> Unit,
    onToggleCropBorders: () -> Unit,
    onToggleIncognito: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.reader_quick_settings_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Reading mode
            Text(
                text = stringResource(R.string.reader_mode_title),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                ReaderMode.entries.forEach { mode ->
                    FilterChip(
                        selected = currentMode == mode,
                        onClick = { onModeChange(mode) },
                        label = { Text(mode.toLabel()) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Reading direction
            Text(
                text = stringResource(R.string.reader_direction_title),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                ReadingDirection.entries.forEach { direction ->
                    FilterChip(
                        selected = readingDirection == direction,
                        onClick = { onDirectionChange(direction) },
                        label = { Text(direction.toLabel()) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Brightness
            Text(
                text = stringResource(R.string.reader_brightness),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = 0.1f..1.5f,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Color filter
            Text(
                text = stringResource(R.string.reader_color_filter),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                ColorFilterMode.entries.forEach { mode ->
                    FilterChip(
                        selected = colorFilterMode == mode,
                        onClick = { onColorFilterChange(mode) },
                        label = { Text(mode.toLabel()) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Toggles (state + persistence already handled by the ViewModel)
            ReaderToggleRow(
                label = stringResource(R.string.reader_crop_borders),
                checked = cropBordersEnabled,
                onToggle = onToggleCropBorders,
            )
            ReaderToggleRow(
                label = stringResource(R.string.reader_incognito_mode),
                checked = incognitoMode,
                onToggle = onToggleIncognito,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReaderToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    // Whole row is clickable with vertical padding so the touch target meets the
    // 48dp accessibility minimum; the Switch is driven by the row (onCheckedChange = null).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = ToggleRowVerticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = null)
    }
}

private val ToggleRowVerticalPadding = 8.dp

@Composable
private fun ReaderMode.toLabel(): String = when (this) {
    ReaderMode.SINGLE_PAGE -> stringResource(R.string.reader_mode_single)
    ReaderMode.DUAL_PAGE -> stringResource(R.string.reader_mode_dual)
    ReaderMode.WEBTOON -> stringResource(R.string.reader_mode_webtoon)
    ReaderMode.SMART_PANELS -> stringResource(R.string.reader_mode_smart)
}

@Composable
private fun ReadingDirection.toLabel(): String = when (this) {
    ReadingDirection.LTR -> stringResource(R.string.reader_direction_ltr)
    ReadingDirection.RTL -> stringResource(R.string.reader_direction_rtl)
    ReadingDirection.VERTICAL -> stringResource(R.string.reader_direction_vertical)
}

@Composable
private fun ColorFilterMode.toLabel(): String = when (this) {
    ColorFilterMode.NONE -> stringResource(R.string.reader_color_filter_none)
    ColorFilterMode.SEPIA -> stringResource(R.string.reader_color_filter_sepia)
    ColorFilterMode.GRAYSCALE -> stringResource(R.string.reader_color_filter_greyscale)
    ColorFilterMode.INVERT -> stringResource(R.string.reader_color_filter_invert)
    ColorFilterMode.CUSTOM_TINT -> stringResource(R.string.reader_color_filter_custom_tint)
}
