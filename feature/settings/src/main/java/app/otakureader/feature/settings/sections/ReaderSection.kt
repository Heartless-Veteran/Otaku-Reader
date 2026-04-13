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

fun ReaderSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    // ── Reader ────────────────────────────────────────────────────────
            SectionHeader(title = stringResource(R.string.settings_reader))

            // Reader mode – ordinal order matches ReaderMode enum:
            // SINGLE_PAGE=0, DUAL_PAGE=1, WEBTOON=2, SMART_PANELS=3
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reading_mode)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val modes = listOf(
                            stringResource(R.string.settings_reading_mode_single_page) to 0,
                            stringResource(R.string.settings_reading_mode_dual_page) to 1,
                            stringResource(R.string.settings_reading_mode_webtoon) to 2,
                            stringResource(R.string.settings_reading_mode_smart_panels) to 3
                        )
                        modes.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.readerMode == value,
                                        onClick = {
                                            onEvent(SettingsEvent.SetReaderMode(value))
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = state.readerMode == value,
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

            // Keep screen on
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_keep_screen_on)) },
                supportingContent = { Text(stringResource(R.string.settings_keep_screen_on_description)) },
                trailingContent = {
                    Switch(
                        checked = state.keepScreenOn,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetKeepScreenOn(it))
                        }
                    )
                }
            )

            // Incognito mode
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_incognito_mode)) },
                supportingContent = { Text(stringResource(R.string.settings_incognito_mode_description)) },
                trailingContent = {
                    Switch(
                        checked = state.incognitoMode,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetIncognitoMode(it))
                        }
                    )
                }
            )

            // Page preloading
            var preloadBeforeSlider by remember(state.preloadPagesBefore) {
                mutableFloatStateOf(state.preloadPagesBefore.toFloat())
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_preload_before, preloadBeforeSlider.roundToInt())) },
                supportingContent = {
                    Column {
                        Text(stringResource(R.string.settings_preload_before_description))
                        Slider(
                            value = preloadBeforeSlider,
                            onValueChange = { preloadBeforeSlider = it },
                            onValueChangeFinished = {
                                onEvent(SettingsEvent.SetPreloadPagesBefore(preloadBeforeSlider.roundToInt()))
                            },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )

            var preloadAfterSlider by remember(state.preloadPagesAfter) {
                mutableFloatStateOf(state.preloadPagesAfter.toFloat())
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_preload_after, preloadAfterSlider.roundToInt())) },
                supportingContent = {
                    Column {
                        Text(stringResource(R.string.settings_preload_after_description))
                        Slider(
                            value = preloadAfterSlider,
                            onValueChange = { preloadAfterSlider = it },
                            onValueChangeFinished = {
                                onEvent(SettingsEvent.SetPreloadPagesAfter(preloadAfterSlider.roundToInt()))
                            },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )

            // Crop borders
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_crop_borders)) },
                supportingContent = { Text(stringResource(R.string.settings_crop_borders_description)) },
                trailingContent = {
                    Switch(
                        checked = state.cropBordersEnabled,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetCropBordersEnabled(it))
                        }
                    )
                }
            )

            // Image quality
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_image_quality)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val qualities = ImageQuality.entries.map { quality ->
                            stringResource(quality.stringRes) to quality.name
                        }
                        qualities.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.imageQuality == value,
                                        onClick = {
                                            onEvent(SettingsEvent.SetImageQuality(value))
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = state.imageQuality == value,
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

            // Data saver mode
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_data_saver)) },
                supportingContent = { Text(stringResource(R.string.settings_data_saver_description)) },
                trailingContent = {
                    Switch(
                        checked = state.dataSaverEnabled,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetDataSaverEnabled(it))
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_reader_display))

            // Fullscreen
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_fullscreen)) },
                supportingContent = { Text(stringResource(R.string.settings_fullscreen_description)) },
                trailingContent = {
                    Switch(
                        checked = state.fullscreen,
                        onCheckedChange = { onEvent(SettingsEvent.SetFullscreen(it)) }
                    )
                }
            )

            // Show content in cutout
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_cutout)) },
                supportingContent = { Text(stringResource(R.string.settings_show_cutout_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showContentInCutout,
                        onCheckedChange = { onEvent(SettingsEvent.SetShowContentInCutout(it)) }
                    )
                }
            )

            // Show page number
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_page_number)) },
                supportingContent = { Text(stringResource(R.string.settings_show_page_number_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showPageNumber,
                        onCheckedChange = { onEvent(SettingsEvent.SetShowPageNumber(it)) }
                    )
                }
            )

            // Background color
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_background_color)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val colors = listOf(
                            stringResource(R.string.settings_bg_black) to 0,
                            stringResource(R.string.settings_bg_white) to 1,
                            stringResource(R.string.settings_bg_gray) to 2,
                            stringResource(R.string.settings_bg_auto) to 3
                        )
                        colors.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.backgroundColor == value,
                                        onClick = { onEvent(SettingsEvent.SetBackgroundColor(value)) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = state.backgroundColor == value, onClick = null)
                                Text(text = label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            )

            // Animate page transitions
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_animate_transitions)) },
                supportingContent = { Text(stringResource(R.string.settings_animate_transitions_description)) },
                trailingContent = {
                    Switch(
                        checked = state.animatePageTransitions,
                        onCheckedChange = { onEvent(SettingsEvent.SetAnimatePageTransitions(it)) }
                    )
                }
            )

            // Show reading mode overlay
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_mode_overlay)) },
                supportingContent = { Text(stringResource(R.string.settings_show_mode_overlay_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showReadingModeOverlay,
                        onCheckedChange = { onEvent(SettingsEvent.SetShowReadingModeOverlay(it)) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_reader_scale))

            // Reader scale type
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_scale_type)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val scales = listOf(
                            stringResource(R.string.settings_scale_fit_screen) to 0,
                            stringResource(R.string.settings_scale_fit_width) to 1,
                            stringResource(R.string.settings_scale_fit_height) to 2,
                            stringResource(R.string.settings_scale_original) to 3,
                            stringResource(R.string.settings_scale_smart_fit) to 4
                        )
                        scales.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.readerScale == value,
                                        onClick = { onEvent(SettingsEvent.SetReaderScale(value)) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = state.readerScale == value, onClick = null)
                                Text(text = label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            )

            // Auto zoom wide images
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_auto_zoom_wide)) },
                supportingContent = { Text(stringResource(R.string.settings_auto_zoom_wide_description)) },
                trailingContent = {
                    Switch(
                        checked = state.autoZoomWideImages,
                        onCheckedChange = { onEvent(SettingsEvent.SetAutoZoomWideImages(it)) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_tap_zones))

            // Tap zone configuration
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_tap_zone_config)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val configs = listOf(
                            stringResource(R.string.settings_tap_default) to 0,
                            stringResource(R.string.settings_tap_left_handed) to 1,
                            stringResource(R.string.settings_tap_kindle) to 2,
                            stringResource(R.string.settings_tap_edge) to 3
                        )
                        configs.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.tapZoneConfig == value,
                                        onClick = { onEvent(SettingsEvent.SetTapZoneConfig(value)) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = state.tapZoneConfig == value, onClick = null)
                                Text(text = label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            )

            // Invert tap zones
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_invert_tap_zones)) },
                supportingContent = { Text(stringResource(R.string.settings_invert_tap_zones_description)) },
                trailingContent = {
                    Switch(
                        checked = state.invertTapZones,
                        onCheckedChange = { onEvent(SettingsEvent.SetInvertTapZones(it)) }
                    )
                }
            )

            // Show tap zones overlay
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_tap_zones)) },
                supportingContent = { Text(stringResource(R.string.settings_show_tap_zones_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showTapZonesOverlay,
                        onCheckedChange = { onEvent(SettingsEvent.SetShowTapZonesOverlay(it)) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_volume_keys))

            // Volume keys enabled
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_volume_keys_paging)) },
                supportingContent = { Text(stringResource(R.string.settings_volume_keys_paging_description)) },
                trailingContent = {
                    Switch(
                        checked = state.volumeKeysEnabled,
                        onCheckedChange = { onEvent(SettingsEvent.SetVolumeKeysEnabled(it)) }
                    )
                }
            )

            // Volume keys inverted
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_volume_keys_inverted)) },
                supportingContent = { Text(stringResource(R.string.settings_volume_keys_inverted_description)) },
                trailingContent = {
                    Switch(
                        checked = state.volumeKeysInverted,
                        onCheckedChange = { onEvent(SettingsEvent.SetVolumeKeysInverted(it)) },
                        enabled = state.volumeKeysEnabled
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_reader_interaction))

            // Double tap animation speed
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_double_tap_speed)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val speeds = listOf(
                            stringResource(R.string.settings_speed_slow) to 0,
                            stringResource(R.string.settings_speed_normal) to 1,
                            stringResource(R.string.settings_speed_fast) to 2
                        )
                        speeds.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.doubleTapAnimationSpeed == value,
                                        onClick = { onEvent(SettingsEvent.SetDoubleTapAnimationSpeed(value)) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = state.doubleTapAnimationSpeed == value, onClick = null)
                                Text(text = label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            )

            // Show actions on long tap
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_long_tap_actions)) },
                supportingContent = { Text(stringResource(R.string.settings_long_tap_actions_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showActionsOnLongTap,
                        onCheckedChange = { onEvent(SettingsEvent.SetShowActionsOnLongTap(it)) }
                    )
                }
            )

            // Save pages to separate folders
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_save_separate_folders)) },
                supportingContent = { Text(stringResource(R.string.settings_save_separate_folders_description)) },
                trailingContent = {
                    Switch(
                        checked = state.savePagesToSeparateFolders,
                        onCheckedChange = { onEvent(SettingsEvent.SetSavePagesToSeparateFolders(it)) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_webtoon))

            // Webtoon side padding
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_webtoon_padding)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val paddings = listOf(
                            stringResource(R.string.settings_padding_none) to 0,
                            stringResource(R.string.settings_padding_small) to 1,
                            stringResource(R.string.settings_padding_medium) to 2,
                            stringResource(R.string.settings_padding_large) to 3
                        )
                        paddings.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.webtoonSidePadding == value,
                                        onClick = { onEvent(SettingsEvent.SetWebtoonSidePadding(value)) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = state.webtoonSidePadding == value, onClick = null)
                                Text(text = label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            )

            // Menu hide sensitivity
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_menu_sensitivity)) },
                supportingContent = {
                    Column(modifier = Modifier.selectableGroup()) {
                        val sensitivities = listOf(
                            stringResource(R.string.settings_sensitivity_low) to 0,
                            stringResource(R.string.settings_sensitivity_medium) to 1,
                            stringResource(R.string.settings_sensitivity_high) to 2
                        )
                        sensitivities.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = state.webtoonMenuHideSensitivity == value,
                                        onClick = { onEvent(SettingsEvent.SetWebtoonMenuHideSensitivity(value)) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = state.webtoonMenuHideSensitivity == value, onClick = null)
                                Text(text = label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            )

            // Double tap zoom
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_webtoon_double_tap)) },
                supportingContent = { Text(stringResource(R.string.settings_webtoon_double_tap_description)) },
                trailingContent = {
                    Switch(
                        checked = state.webtoonDoubleTapZoom,
                        onCheckedChange = { onEvent(SettingsEvent.SetWebtoonDoubleTapZoom(it)) }
                    )
                }
            )

            // Disable zoom out
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_disable_zoom_out)) },
                supportingContent = { Text(stringResource(R.string.settings_disable_zoom_out_description)) },
                trailingContent = {
                    Switch(
                        checked = state.webtoonDisableZoomOut,
                        onCheckedChange = { onEvent(SettingsEvent.SetWebtoonDisableZoomOut(it)) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_eink))

            // Flash on page change
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_eink_flash)) },
                supportingContent = { Text(stringResource(R.string.settings_eink_flash_description)) },
                trailingContent = {
                    Switch(
                        checked = state.einkFlashOnPageChange,
                        onCheckedChange = { onEvent(SettingsEvent.SetEinkFlashOnPageChange(it)) }
                    )
                }
            )

            // Black and white mode
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_eink_bw)) },
                supportingContent = { Text(stringResource(R.string.settings_eink_bw_description)) },
                trailingContent = {
                    Switch(
                        checked = state.einkBlackAndWhite,
                        onCheckedChange = { onEvent(SettingsEvent.SetEinkBlackAndWhite(it)) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = stringResource(R.string.settings_reader_behavior))

            // Skip read chapters
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_skip_read)) },
                supportingContent = { Text(stringResource(R.string.settings_skip_read_description)) },
                trailingContent = {
                    Switch(
                        checked = state.skipReadChapters,
                        onCheckedChange = { onEvent(SettingsEvent.SetSkipReadChapters(it)) }
                    )
                }
            )

            // Skip filtered chapters
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_skip_filtered)) },
                supportingContent = { Text(stringResource(R.string.settings_skip_filtered_description)) },
                trailingContent = {
                    Switch(
                        checked = state.skipFilteredChapters,
                        onCheckedChange = { onEvent(SettingsEvent.SetSkipFilteredChapters(it)) }
                    )
                }
            )

            // Skip duplicate chapters
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_skip_duplicates)) },
                supportingContent = { Text(stringResource(R.string.settings_skip_duplicates_description)) },
                trailingContent = {
                    Switch(
                        checked = state.skipDuplicateChapters,
                        onCheckedChange = { onEvent(SettingsEvent.SetSkipDuplicateChapters(it)) }
                    )
                }
            )

            // Always show chapter transition
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_chapter_transition)) },
                supportingContent = { Text(stringResource(R.string.settings_show_chapter_transition_description)) },
                trailingContent = {
                    Switch(
                        checked = state.alwaysShowChapterTransition,
                        onCheckedChange = { onEvent(SettingsEvent.SetAlwaysShowChapterTransition(it)) }
                    )
                }
            )
}

