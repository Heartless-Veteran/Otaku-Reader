package app.otakureader.feature.more

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.otakureader.core.ui.theme.LocalOtakuColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToExtensions: () -> Unit = {},
    onNavigateToFeed: () -> Unit = {},
    onNavigateToShareLibrary: () -> Unit = {},
    onNavigateToScanLibrary: () -> Unit = {},
    onNavigateToUpdateErrors: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.more_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            AppLogoCard(versionName = versionName)
            Spacer(modifier = Modifier.height(8.dp))

            MoreListItem(
                icon = Icons.Default.Settings,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                headline = stringResource(R.string.more_settings),
                supporting = stringResource(R.string.more_settings_desc),
                onClick = onNavigateToSettings
            )

            HorizontalDivider()

            MoreListItem(
                icon = Icons.Default.Extension,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                headline = stringResource(R.string.more_extensions),
                supporting = stringResource(R.string.more_extensions_desc),
                onClick = onNavigateToExtensions
            )

            HorizontalDivider()

            MoreListItem(
                icon = Icons.Default.Notifications,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                headline = stringResource(R.string.more_feed),
                supporting = stringResource(R.string.more_feed_desc),
                onClick = onNavigateToFeed
            )

            HorizontalDivider()

            MoreListItem(
                icon = Icons.Default.CloudUpload,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                headline = stringResource(R.string.more_backup),
                supporting = stringResource(R.string.more_backup_desc),
                onClick = onNavigateToBackup
            )

            HorizontalDivider()

            // QR Library Sharing (#711)
            MoreListItem(
                icon = Icons.Default.QrCode,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                headline = stringResource(R.string.more_share_library),
                supporting = stringResource(R.string.more_share_library_desc),
                onClick = onNavigateToShareLibrary
            )

            HorizontalDivider()

            MoreListItem(
                icon = Icons.Default.PhotoCamera,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                headline = stringResource(R.string.more_scan_library),
                supporting = stringResource(R.string.more_scan_library_desc),
                onClick = onNavigateToScanLibrary
            )

            HorizontalDivider()

            MoreListItem(
                icon = Icons.Default.Download,
                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.onErrorContainer,
                headline = stringResource(R.string.more_downloads),
                supporting = stringResource(R.string.more_downloads_desc),
                onClick = onNavigateToDownloads
            )

            HorizontalDivider()

            MoreListItem(
                icon = Icons.Default.ErrorOutline,
                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.onErrorContainer,
                headline = stringResource(R.string.more_update_errors),
                supporting = stringResource(R.string.more_update_errors_desc),
                onClick = onNavigateToUpdateErrors
            )

            HorizontalDivider()

            MoreListItem(
                icon = Icons.Default.QueryStats,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                headline = stringResource(R.string.more_statistics),
                supporting = stringResource(R.string.more_statistics_desc),
                onClick = onNavigateToStatistics
            )

            HorizontalDivider()

            MoreListItem(
                icon = Icons.Default.Bookmark,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                headline = stringResource(R.string.more_bookmarks),
                supporting = stringResource(R.string.more_bookmarks_description),
                onClick = onNavigateToBookmarks
            )

            HorizontalDivider()

            MoreListItem(
                icon = Icons.Default.Info,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                headline = stringResource(R.string.more_about),
                supporting = stringResource(R.string.more_about_desc),
                onClick = onNavigateToAbout
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppLogoCard(
    versionName: String,
    modifier: Modifier = Modifier,
) {
    val otaku = LocalOtakuColors.current
    val gradientEnd = Color.hsl(270f, 0.65f, if (otaku.isDark) 0.30f else 0.68f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(otaku.accent, gradientEnd),
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = stringResource(R.string.more_app_name),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.more_app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    if (versionName.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.more_version_label, versionName),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreListItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    headline: String,
    supporting: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = { Text(supporting) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = headline,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        },
        modifier = modifier.clickable { onClick() }
    )
}
