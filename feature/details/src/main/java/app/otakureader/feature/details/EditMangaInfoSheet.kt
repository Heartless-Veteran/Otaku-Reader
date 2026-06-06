package app.otakureader.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMangaInfoSheet(
    manga: Manga,
    onSave: (
        title: String?,
        description: String?,
        author: String?,
        artist: String?,
        thumbnailUrl: String?,
        genres: List<String>?,
        status: MangaStatus?,
    ) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(manga.title) }
    var description by remember { mutableStateOf(manga.description ?: "") }
    var author by remember { mutableStateOf(manga.author ?: "") }
    var artist by remember { mutableStateOf(manga.artist ?: "") }
    var thumbnailUrl by remember { mutableStateOf(manga.thumbnailUrl ?: "") }
    var genres by remember { mutableStateOf(manga.genre.joinToString(", ")) }
    var status by remember { mutableStateOf(manga.status) }
    var showResetDialog by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.details_edit_info_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.details_edit_field_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text(stringResource(R.string.details_edit_field_author)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(stringResource(R.string.details_edit_field_artist)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.details_edit_field_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
            )

            OutlinedTextField(
                value = genres,
                onValueChange = { genres = it },
                label = { Text(stringResource(R.string.details_edit_field_genres)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = thumbnailUrl,
                onValueChange = { thumbnailUrl = it },
                label = { Text(stringResource(R.string.details_edit_field_cover_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it },
            ) {
                OutlinedTextField(
                    value = status.displayLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.details_edit_field_status)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false },
                ) {
                    MangaStatus.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.displayLabel()) },
                            onClick = {
                                status = s
                                statusExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { showResetDialog = true }) {
                    Text(stringResource(R.string.details_edit_info_reset))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.details_edit_info_cancel))
                    }
                    TextButton(
                        onClick = {
                            onSave(
                                title.trim().takeIf { it.isNotEmpty() },
                                description.trim().takeIf { it.isNotEmpty() },
                                author.trim().takeIf { it.isNotEmpty() },
                                artist.trim().takeIf { it.isNotEmpty() },
                                thumbnailUrl.trim().takeIf { it.isNotEmpty() },
                                genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    .takeIf { it.isNotEmpty() },
                                status,
                            )
                        },
                    ) {
                        Text(stringResource(R.string.details_edit_info_save))
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.details_edit_reset_confirm_title)) },
            text = { Text(stringResource(R.string.details_edit_reset_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onReset()
                    },
                ) {
                    Text(stringResource(R.string.details_edit_reset_confirm_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.details_edit_info_cancel))
                }
            },
        )
    }
}

private fun MangaStatus.displayLabel(): String = when (this) {
    MangaStatus.UNKNOWN -> "Unknown"
    MangaStatus.ONGOING -> "Ongoing"
    MangaStatus.COMPLETED -> "Completed"
    MangaStatus.LICENSED -> "Licensed"
    MangaStatus.PUBLISHING_FINISHED -> "Publishing Finished"
    MangaStatus.CANCELLED -> "Cancelled"
    MangaStatus.ON_HIATUS -> "On Hiatus"
}
