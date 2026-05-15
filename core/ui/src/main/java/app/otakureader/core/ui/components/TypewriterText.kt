package app.otakureader.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    speedMs: Long = 45L
) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        displayedText = ""
        text.forEachIndexed { index, _ ->
            displayedText = text.take(index + 1)
            kotlinx.coroutines.delay(speedMs)
        }
    }

    Text(
        text = displayedText,
        modifier = modifier,
        style = style
    )
}

@Preview
@Composable
private fun TypewriterTextPreview() {
    TypewriterText(
        text = "Attack on Titan: Final Season",
        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(color = Color.White)
    )
}
