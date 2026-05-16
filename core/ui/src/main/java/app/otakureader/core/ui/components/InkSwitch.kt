package app.otakureader.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun InkSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "inkSwitchPos"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFFFF4757) else Color(0xFF2A2A35),
        animationSpec = tween(200),
        label = "inkSwitchTrack"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .background(
                color = trackColor,
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Ink trail effect
        if (checked) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(thumbPosition)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF4757).copy(alpha = 0.3f),
                                Color(0xFFFF4757).copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(20.dp)
                .graphicsLayer {
                    translationX = thumbPosition * 20.dp.toPx()
                }
                .background(
                    color = if (checked) Color.White else Color(0xFF808080),
                    shape = RoundedCornerShape(2.dp)
                )
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}
