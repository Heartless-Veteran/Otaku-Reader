package app.otakureader.core.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color

@Immutable
data class MangaCoverTheme(
    val dominant: Color = Color(0xFF12121A),
    val vibrant: Color = Color(0xFFFF4757),
    val muted: Color = Color(0xFF2A2A35),
    val darkVibrant: Color = Color(0xFF0A0A0F),
    val bodyText: Color = Color(0xFFE0E0E5),
    val titleText: Color = Color(0xFFFFFFFF),
    val contentType: ContentType = ContentType.MANGA,
    val accentGlow: Color = vibrant.copy(alpha = 0.3f)
) {
    val isDark: Boolean get() = true
}

val MangaDefaultTheme = MangaCoverTheme(
    dominant = Color(0xFF12121A),
    vibrant = Color(0xFFFF4757),
    muted = Color(0xFF2A2A35),
    darkVibrant = Color(0xFF0A0A0F),
    bodyText = Color(0xFFE0E0E5),
    titleText = Color(0xFFFFFFFF),
    contentType = ContentType.MANGA,
    accentGlow = Color(0xFFFF4757).copy(alpha = 0.3f)
)

val ManhwaDefaultTheme = MangaCoverTheme(
    dominant = Color(0xFF16161F),
    vibrant = Color(0xFF9B59B6),
    muted = Color(0xFF2E2E3A),
    darkVibrant = Color(0xFF0D0D12),
    bodyText = Color(0xFFE0E0E5),
    titleText = Color(0xFFFFFFFF),
    contentType = ContentType.MANHWA,
    accentGlow = Color(0xFF9B59B6).copy(alpha = 0.3f)
)

private val ThemeAnimationSpec: AnimationSpec<Color> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

@Composable
fun animateMangaCoverTheme(
    target: MangaCoverTheme,
    animationSpec: AnimationSpec<Color> = ThemeAnimationSpec
): MangaCoverTheme {
    val dominant: State<Color> = animateColorAsState(target.dominant, animationSpec, label = "dominant")
    val vibrant: State<Color> = animateColorAsState(target.vibrant, animationSpec, label = "vibrant")
    val muted: State<Color> = animateColorAsState(target.muted, animationSpec, label = "muted")
    val darkVibrant: State<Color> = animateColorAsState(target.darkVibrant, animationSpec, label = "darkVibrant")
    val bodyText: State<Color> = animateColorAsState(target.bodyText, animationSpec, label = "bodyText")
    val titleText: State<Color> = animateColorAsState(target.titleText, animationSpec, label = "titleText")
    val accentGlow: State<Color> = animateColorAsState(target.accentGlow, animationSpec, label = "accentGlow")

    return MangaCoverTheme(
        dominant = dominant.value,
        vibrant = vibrant.value,
        muted = muted.value,
        darkVibrant = darkVibrant.value,
        bodyText = bodyText.value,
        titleText = titleText.value,
        contentType = target.contentType,
        accentGlow = accentGlow.value
    )
}
