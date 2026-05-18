package app.otakureader.core.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val PageTurnSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 500,
    easing = FastOutSlowInEasing
)

private val PageFadeSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 350,
    easing = LinearEasing
)

@Composable
fun PageTurnTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = PageTurnSpec,
        label = "pageTurn"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = PageFadeSpec,
        label = "pageFade"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                this.rotationY = (1f - progress) * -15f
                this.cameraDistance = 12f * density
                this.transformOrigin = TransformOrigin(0f, 0.5f)
                this.shadowElevation = if (visible) 8.dp.toPx() else 0f
            }
    ) {
        content()
    }
}

@Composable
fun <T> pageTurnSpec(): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
    (fadeIn(animationSpec = tween(300, delayMillis = 50)) + scaleIn(initialScale = 0.95f, animationSpec = tween(400))).togetherWith(
        fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 1.05f, animationSpec = tween(300))
    )
}
