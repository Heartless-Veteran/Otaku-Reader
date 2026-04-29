package app.otakureader.feature.more.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.otakureader.domain.model.ShareableLibrary
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.serialization.json.Json

/**
 * Screen that scans a QR code from another device and imports the library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanLibraryScreen(
    onNavigateBack: () -> Unit,
    onLibraryScanned: (ShareableLibrary) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scannedLibrary by remember { mutableStateOf<ShareableLibrary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchQrScanner(context) { result ->
                isScanning = false
                if (result.contents == null) {
                    error = "Scan cancelled"
                    return@launchQrScanner
                }
                try {
                    scannedLibrary = Json.decodeFromString(result.contents)
                } catch (e: Exception) {
                    error = "Invalid QR code: ${e.message}"
                }
            }
        } else {
            error = "Camera permission required to scan QR codes"
            isScanning = false
        }
    }

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        isScanning = false
        if (result.contents == null) {
            error = "Scan cancelled"
            return@rememberLauncherForActivityResult
        }
        try {
            scannedLibrary = Json.decodeFromString(result.contents)
        } catch (e: Exception) {
            error = "Invalid QR code: ${e.message}"
        }
    }

    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                isScanning = true
                val options = ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Scan a library QR code")
                    setCameraId(0)
                    setBeepEnabled(false)
                    setBarcodeImageEnabled(false)
                }
                qrLauncher.launch(options)
            }
            else -> cameraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Library") },
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
            when {
                isScanning -> {
                    CircularProgressIndicator()
                    Text("Opening camera...", style = MaterialTheme.typography.bodyMedium)
                }
                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        error = null
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Try Again")
                    }
                }
                scannedLibrary != null -> {
                    val library = scannedLibrary!!
                    Text(
                        text = "Library found!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "${library.manga.size} manga ready to import",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onLibraryScanned(library) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add to My Library")
                    }
                    Button(
                        onClick = {
                            scannedLibrary = null
                            error = null
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scan Another")
                    }
                }
                else -> {
                    Text(
                        text = "Point your camera at a library QR code",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun launchQrScanner(
    context: android.content.Context,
    onResult: (com.journeyapps.barcodescanner.ScanIntentResult) -> Unit
) {
    // Handled by the launcher in the composable
}
