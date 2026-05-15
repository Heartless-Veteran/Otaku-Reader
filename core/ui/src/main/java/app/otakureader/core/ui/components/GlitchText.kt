package app.otakureader.core.ui.components

import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    var isSettled by remember { mutableStateOf(false) }

    LaunchedEffect(text) {
        isSettled = false
        kotlinx.coroutines.delay(600)
        isSettled = true
    }

    if (isSettled) {
        Text(
            text = text,
            modifier = modifier,
            style = style
        )
    } else {
        GlitchingText(text = text, modifier = modifier, style = style)
    }
}

@Composable
private fun GlitchingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle
) {
    val offsetR by rememberInfiniteTransition(label = "glitchR").animateValue(
        initialValue = 0.dp,
        targetValue = 3.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 200
                0.dp at 0
                2.dp at 40
                (-1).dp at 80
                3.dp at 120
                0.dp at 160
            }
        ),
        label = "offsetR"
    )

    val offsetG by rememberInfiniteTransition(label = "glitchG").animateValue(
        initialValue = 0.dp,
        targetValue = (-2).dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 180
                0.dp at 0
                (-2).dp at 50
                1.dp at 100
                (-1).dp at 140
                0.dp at 170
            }
        ),
        label = "offsetG"
    )

    val offsetB by rememberInfiniteTransition(label = "glitchB").animateValue(
        initialValue = 0.dp,
        targetValue = 2.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 220
                0.dp at 0
                1.dp at 60
                (-2).dp at 110
                2.dp at 160
                0.dp at 200
            }
        ),
        label = "offsetB"
    )

    Box(modifier = modifier) {
        // Red channel
        Text(
            text = text,
            modifier = Modifier.offset(x = offsetR, y = 0.dp),
            style = style.copy(color = Color(0xFFFF4757).copy(alpha = 0.7f))
        )
        // Green channel
        Text(
            text = text,
            modifier = Modifier.offset(x = offsetG, y = 1.dp),
            style = style.copy(color = Color(0xFF2ED573).copy(alpha = 0.7f))
        )
        // Blue channel
        Text(
            text = text,
            modifier = Modifier.offset(x = offsetB, y = (-1).dp),
            style = style.copy(color = Color(0xFF00D2D3).copy(alpha = 0.7f))
        )
        // Main text
        Text(
            text = text,
            style = style
        )
    }
}
