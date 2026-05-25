package app.otakureader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.otakureader.core.ui.components.OtakuChip

@Composable
internal fun CategoryFilterChips(
    categories: List<CategoryItem>,
    selectedCategory: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // "All" chip
        item {
            OtakuChip(
                label = stringResource(R.string.library_category_all),
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
            )
        }

        // Category chips
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            OtakuChip(
                label = category.name,
                selected = selectedCategory == category.id,
                onClick = { onCategorySelected(category.id) },
            )
        }
    }
}

@Composable
internal fun ReadingListFilterChips(
    readingLists: List<ReadingListFilterItem>,
    selectedListId: Long?,
    onListSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            OtakuChip(
                label = stringResource(R.string.library_reading_list_all),
                selected = selectedListId == null,
                onClick = { onListSelected(null) },
            )
        }

        items(
            items = readingLists,
            key = { it.id }
        ) { list ->
            OtakuChip(
                label = list.name,
                selected = selectedListId == list.id,
                onClick = { onListSelected(list.id) },
            )
        }
    }
}
