package app.otakureader.feature.more.qr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.otakureader.core.common.qr.generateQrCode
import app.otakureader.domain.model.ShareableLibrary
import app.otakureader.domain.model.ShareableManga
import app.otakureader.domain.model.MangaStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen that generates a QR code encoding the user's library.
 * The receiving device scans this code and imports the manga list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLibraryScreen(
    onNavigateBack: () -> Unit,
    onScanLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShareLibraryViewModel = hiltViewModel()
) {
    val mangaList by viewModel.library.collectAsStateWithLifecycle()
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mangaList) {
        val library = ShareableLibrary(manga = mangaList)
        val json = try {
            Json.encodeToString(library)
        } catch (e: Exception) {
            error = "Failed to encode library: ${e.message}"
            return@LaunchedEffect
        }
        // ZXing can handle ~2-3KB reliably at medium ECC level
        qrBitmap = generateQrCode(json, size = 512)
        if (qrBitmap == null) {
            error = "Failed to generate QR code"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(app.otakureader.feature.more.R.string.more_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Show this QR code to a friend to share your library",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                qrBitmap == null -> {
                    CircularProgressIndicator()
                }
                else -> {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR code containing library data",
                        modifier = Modifier.size(280.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${mangaList.size} manga shared",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onScanLibrary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan Someone Else's Library")
            }
        }
    }
}
