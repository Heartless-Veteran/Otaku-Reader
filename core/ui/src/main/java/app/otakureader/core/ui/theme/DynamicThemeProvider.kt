package app.otakureader.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalMangaCoverTheme: ProvidableCompositionLocal<MangaCoverTheme?> =
    staticCompositionLocalOf { null }

val LocalContentType: ProvidableCompositionLocal<ContentType> =
    compositionLocalOf { ContentType.MANGA }

val LocalAnimationSpec = staticCompositionLocalOf {
    androidx.compose.animation.core.SpringSpec<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
    )
}

@Composable
fun DynamicThemeProvider(
    theme: MangaCoverTheme?,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalMangaCoverTheme provides theme,
        LocalContentType provides (theme?.contentType ?: ContentType.MANGA),
        content = content
    )
}
