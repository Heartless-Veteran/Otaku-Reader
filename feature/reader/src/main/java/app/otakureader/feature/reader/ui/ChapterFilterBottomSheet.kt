package app.otakureader.feature.reader.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.otakureader.feature.reader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterFilterBottomSheet(
    skipReadChapters: Boolean,
    skipFilteredChapters: Boolean,
    skipDuplicateChapters: Boolean,
    onToggleSkipRead: () -> Unit,
    onToggleSkipFiltered: () -> Unit,
    onToggleSkipDuplicate: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.reader_chapter_filter_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.reader_skip_read_chapters)) },
                trailingContent = {
                    Switch(checked = skipReadChapters, onCheckedChange = { onToggleSkipRead() })
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.reader_skip_filtered_chapters)) },
                trailingContent = {
                    Switch(checked = skipFilteredChapters, onCheckedChange = { onToggleSkipFiltered() })
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.reader_skip_duplicate_chapters)) },
                trailingContent = {
                    Switch(checked = skipDuplicateChapters, onCheckedChange = { onToggleSkipDuplicate() })
                },
            )
        }
    }
}
