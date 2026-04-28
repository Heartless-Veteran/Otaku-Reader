package app.otakureader.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Preview palette for the Otaku Reader theme system.
 *
 * Run these previews in Android Studio or via `./gradlew :core:ui:compileDebugSources`
 * to verify all color schemes render correctly.
 */

@Preview(name = "Light - Default")
@Composable
private fun PreviewLightDefault() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        ThemePalettePreview()
    }
}

@Preview(name = "Dark - Default")
@Composable
private fun PreviewDarkDefault() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ThemePalettePreview()
    }
}

@Preview(name = "Dark - Pure Black")
@Composable
private fun PreviewPureBlack() {
    OtakuReaderTheme(darkTheme = true, usePureBlack = true) {
        ThemePalettePreview()
    }
}

@Preview(name = "Light - High Contrast")
@Composable
private fun PreviewHighContrastLight() {
    OtakuReaderTheme(darkTheme = false, useHighContrast = true) {
        ThemePalettePreview()
    }
}

@Preview(name = "Dark - High Contrast")
@Composable
private fun PreviewHighContrastDark() {
    OtakuReaderTheme(darkTheme = true, useHighContrast = true) {
        ThemePalettePreview()
    }
}

@Preview(name = "Custom Accent - Green")
@Composable
private fun PreviewCustomAccent() {
    OtakuReaderTheme(
        darkTheme = false,
        colorScheme = COLOR_SCHEME_CUSTOM_ACCENT,
        customAccentColor = 0xFF4CAF50
    ) {
        ThemePalettePreview()
    }
}

@Composable
private fun ThemePalettePreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Otaku Reader Theme",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            ColorSwatch(name = "Primary", color = MaterialTheme.colorScheme.primary)
            ColorSwatch(name = "Secondary", color = MaterialTheme.colorScheme.secondary)
            ColorSwatch(name = "Tertiary", color = MaterialTheme.colorScheme.tertiary)
            ColorSwatch(name = "Background", color = MaterialTheme.colorScheme.background)
            ColorSwatch(name = "Surface", color = MaterialTheme.colorScheme.surface)
            ColorSwatch(name = "Surface Variant", color = MaterialTheme.colorScheme.surfaceVariant)
            ColorSwatch(name = "Error", color = MaterialTheme.colorScheme.error)

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sample Card",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "This card uses surface and onSurface colors automatically.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { }) {
                Text("Primary Button")
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color)
        )
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = color.toString().substring(0, minOf(16, color.toString().length)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
