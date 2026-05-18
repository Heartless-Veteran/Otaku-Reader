package app.otakureader.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route

/**
 * Widget configuration and discovery screen.
 *
 * The app already ships Glance widgets for continue reading, recent updates, and now reading.
 * This screen makes those widgets discoverable from Settings and documents what each one shows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigurationScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_widgets)) },
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
            SectionHeader(title = stringResource(R.string.settings_widgets_available))
            WidgetInfoItem(
                title = stringResource(R.string.settings_widget_continue_reading),
                description = stringResource(R.string.settings_widget_continue_reading_description),
            )
            WidgetInfoItem(
                title = stringResource(R.string.settings_widget_recent_updates),
                description = stringResource(R.string.settings_widget_recent_updates_description),
            )
            WidgetInfoItem(
                title = stringResource(R.string.settings_widget_now_reading),
                description = stringResource(R.string.settings_widget_now_reading_description),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_widgets_how_to_add)) },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.settings_widgets_how_to_add_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
private fun WidgetInfoItem(title: String, description: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
    )
}

fun NavGraphBuilder.widgetConfigurationScreen(onNavigateBack: () -> Unit) {
    composable<Route.WidgetConfiguration> {
        WidgetConfigurationScreen(onNavigateBack = onNavigateBack)
    }
}
