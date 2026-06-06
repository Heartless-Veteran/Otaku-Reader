package app.otakureader.feature.settings.navorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.core.preferences.NavTab
import app.otakureader.feature.settings.R

fun NavGraphBuilder.settingsNavOrderScreen(onNavigateBack: () -> Unit) {
    composable<Route.SettingsNavOrder> {
        SettingsNavOrderScreen(onNavigateBack = onNavigateBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNavOrderScreen(
    onNavigateBack: () -> Unit,
    viewModel: NavOrderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_order_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(NavOrderEvent.Reset()) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.nav_order_reset))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            itemsIndexed(state.tabs) { index, tab ->
                NavTabRow(
                    tab = tab,
                    index = index,
                    total = state.tabs.size,
                    onMoveUp = { viewModel.onEvent(NavOrderEvent.MoveUp(index)) },
                    onMoveDown = { viewModel.onEvent(NavOrderEvent.MoveDown(index)) },
                )
            }
        }
    }
}

@Composable
private fun NavTabRow(
    tab: NavTab,
    index: Int,
    total: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = tab.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.nav_order_move_up))
            }
            IconButton(onClick = onMoveDown, enabled = index < total - 1) {
                Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.nav_order_move_down))
            }
        }
    }
}

@Composable
private fun NavTab.displayName(): String = when (this) {
    NavTab.LIBRARY -> stringResource(R.string.nav_tab_library)
    NavTab.UPDATES -> stringResource(R.string.nav_tab_updates)
    NavTab.BROWSE -> stringResource(R.string.nav_tab_browse)
    NavTab.HISTORY -> stringResource(R.string.nav_tab_history)
    NavTab.MORE -> stringResource(R.string.nav_tab_more)
}
