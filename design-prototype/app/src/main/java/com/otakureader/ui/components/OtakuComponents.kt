package com.otakureader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontFamily = JetbrainsMonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = LocalOtakuColors.current.fgDim,
    )
}

@Composable
fun OtakuChip(
    label: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingLabel: String? = null,
) {
    val colors = LocalOtakuColors.current
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(if (active) colors.accentDim else colors.surface1)
            .border(1.dp, if (active) colors.accent else colors.borderStrong, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leadingContent?.invoke()
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) colors.accentSoft else colors.fgMuted,
            letterSpacing = 0.2.sp,
        )
        if (trailingLabel != null) {
            Text(
                text = trailingLabel,
                fontFamily = JetbrainsMonoFamily,
                fontSize = 11.sp,
                color = (if (active) colors.accentSoft else colors.fgMuted).copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
fun OtakuCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalOtakuColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun OtakuToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOtakuColors.current
    Box(
        modifier = modifier
            .size(38.dp, 22.dp)
            .clip(CircleShape)
            .background(if (checked) colors.accent else colors.surface3)
            .clip(CircleShape),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalOtakuColors.current.fgDim,
    fontSize: Int = 11,
) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = JetbrainsMonoFamily,
        fontSize = fontSize.sp,
        color = color,
    )
}

@Composable
fun OtakuSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    trackColor: Color = LocalOtakuColors.current.surface3,
    fillColor: Color = LocalOtakuColors.current.accent,
) {
    val colors = LocalOtakuColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(trackColor),
        contentAlignment = Alignment.CenterStart,
    ) {
        val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(fillColor),
        )
    }
}
