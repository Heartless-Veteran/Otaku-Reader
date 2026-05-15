package app.otakureader.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun GlowSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "glowSwitchPos"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(300),
        label = "glowSwitchAlpha"
    )

    val glowPulse by rememberInfiniteTransition(label = "glowPulse").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val glowColor = Color(0xFF9B59B6).copy(alpha = glowAlpha * glowPulse * 0.5f)

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .background(
                color = if (checked) Color(0xFF2A1F35) else Color(0xFF1E1E2A),
                shape = RoundedCornerShape(15.dp)
            )
            .border(
                width = if (checked) 1.dp else 0.dp,
                color = glowColor,
                shape = RoundedCornerShape(15.dp)
            )
            .shadow(
                elevation = if (checked) 8.dp else 0.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Glow trail
        if (checked) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(thumbPosition)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF9B59B6).copy(alpha = glowAlpha * 0.3f),
                                Color(0xFF00D2D3).copy(alpha = glowAlpha * 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(15.dp)
                    )
            )
        }

        // Thumb with glow
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(24.dp)
                .graphicsLayer {
                    translationX = thumbPosition * 22.dp.toPx()
                }
                .background(
                    brush = if (checked) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF9B59B6),
                                Color(0xFF7B3FA0)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3A3A4A),
                                Color(0xFF2A2A38)
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .shadow(
                    elevation = if (checked) 6.dp else 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = if (checked) glowColor else Color.Transparent,
                    spotColor = if (checked) glowColor else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Preview
@Composable
private fun GlowSwitchPreview() {
    var checked by remember { mutableStateOf(true) }
    GlowSwitch(checked = checked, onCheckedChange = { checked = it })
}
