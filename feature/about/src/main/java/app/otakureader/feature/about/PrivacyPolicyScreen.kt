package app.otakureader.feature.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * In-app privacy policy (#1021). Static, plain-language summary of the data
 * model: nothing is collected, everything is local, and each opt-in network
 * feature is explained. The canonical long-form text lives in PRIVACY.md at
 * the repository root; keep the two in sync when either changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.privacy_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Card {
                Text(
                    text = stringResource(R.string.privacy_summary),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }

            PrivacySection(R.string.privacy_collect_title, R.string.privacy_collect_body)
            PrivacySection(R.string.privacy_local_title, R.string.privacy_local_body)
            PrivacySection(R.string.privacy_sources_title, R.string.privacy_sources_body)
            PrivacySection(R.string.privacy_trackers_title, R.string.privacy_trackers_body)
            PrivacySection(R.string.privacy_extensions_title, R.string.privacy_extensions_body)
            PrivacySection(R.string.privacy_backups_title, R.string.privacy_backups_body)
            PrivacySection(R.string.privacy_crash_title, R.string.privacy_crash_body)
            PrivacySection(R.string.privacy_biometric_title, R.string.privacy_biometric_body)
            PrivacySection(R.string.privacy_contact_title, R.string.privacy_contact_body)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrivacySection(titleRes: Int, bodyRes: Int) {
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
