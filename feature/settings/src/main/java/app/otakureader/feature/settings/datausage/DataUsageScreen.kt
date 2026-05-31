package app.otakureader.feature.settings.datausage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.domain.model.DataUsageRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataUsageScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataUsageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Usage") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val tabs = DataUsageTab.entries
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                tabs.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.onEvent(DataUsageEvent.SelectTab(tab)) },
                        text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            val entries = when (state.selectedTab) {
                DataUsageTab.TODAY -> state.todayEntries
                DataUsageTab.WEEK -> state.weekEntries
                DataUsageTab.MONTH -> state.monthEntries
            }

            NetworkSummaryChips(entries = entries)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val grouped = entries.groupBy { it.category }
                items(grouped.entries.toList()) { (category, rows) ->
                    DataUsageRow(category = category, bytes = rows.sumOf { it.bytes })
                }
            }
        }
    }
}

@Composable
private fun NetworkSummaryChips(entries: List<DataUsageRecord>) {
    val wifiBytes = entries.filter { it.network == "WIFI" }.sumOf { it.bytes }
    val mobileBytes = entries.filter { it.network == "MOBILE" }.sumOf { it.bytes }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SuggestionChip(onClick = {}, label = { Text("Wi-Fi: ${formatBytes(wifiBytes)}") })
        SuggestionChip(onClick = {}, label = { Text("Mobile: ${formatBytes(mobileBytes)}") })
    }
}

@Composable
private fun DataUsageRow(category: String, bytes: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formatBytes(bytes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L         -> "%.1f KB".format(bytes / 1_024.0)
    else                    -> "$bytes B"
}

fun NavGraphBuilder.dataUsageScreen(onNavigateBack: () -> Unit) {
    composable<Route.DataUsage> {
        DataUsageScreen(onNavigateBack = onNavigateBack)
    }
}
