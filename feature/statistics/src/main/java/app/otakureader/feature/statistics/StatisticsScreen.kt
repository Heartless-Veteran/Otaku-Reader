package app.otakureader.feature.statistics

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.otakureader.core.navigation.LocalNavigator
import app.otakureader.core.ui.share.createShareIntent
import app.otakureader.feature.statistics.components.ReadingStatsCard
import app.otakureader.feature.statistics.components.StreakCard
import app.otakureader.feature.statistics.components.TimeDistributionCard

/**
 * Statistics screen with shareable reading stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var shareBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            shareBitmap = captureShareableCard(context, uiState)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share stats"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shareable card (this is what gets captured for sharing)
            StatisticsShareCard(
                uiState = uiState,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            ReadingStatsCard(
                totalChapters = uiState.totalChapters,
                totalPages = uiState.totalPages,
                totalManga = uiState.totalManga,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            StreakCard(
                currentStreak = uiState.currentStreak,
                longestStreak = uiState.longestStreak,
                history = uiState.streakHistory,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            TimeDistributionCard(
                hourDistribution = uiState.hourDistribution,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    // Trigger share sheet when bitmap is captured
    LaunchedEffect(shareBitmap) {
        shareBitmap?.let { bitmap ->
            val uri = app.otakureader.core.ui.share.saveBitmapToCache(context, bitmap)
            val intent = createShareIntent(uri, "My Otaku Reader Stats")
            context.startActivity(intent)
            shareBitmap = null
        }
    }
}