package com.otakureader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.ui.theme.LocalOtakuColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(onDismiss: () -> Unit) {
    val colors = LocalOtakuColors.current
    var downloadedOnly by remember { mutableStateOf(false) }
    var hasUnread by remember { mutableStateOf(true) }
    var contentRating by remember { mutableStateOf("Safe") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .size(36.dp, 4.dp)
                    .clip(CircleShape)
                    .background(colors.borderStrong),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Filter", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.fg)
                TextButton(onClick = {
                    downloadedOnly = false
                    hasUnread = true
                    contentRating = "Safe"
                }) {
                    Text("Reset", color = colors.accentSoft, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Quick filters")
            Spacer(Modifier.height(10.dp))

            FilterToggleRow(
                label = "Downloaded only",
                sub = "Hide entries with no chapters on device",
                checked = downloadedOnly,
                onToggle = { downloadedOnly = !downloadedOnly },
            )
            Spacer(Modifier.height(8.dp))
            FilterToggleRow(
                label = "Has unread chapters",
                sub = "Hide fully-read manga",
                checked = hasUnread,
                onToggle = { hasUnread = !hasUnread },
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("Content rating")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Safe", "Suggestive", "Mature").forEach { rating ->
                    val active = contentRating == rating
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) colors.accentDim else colors.surface2)
                            .border(1.dp, if (active) colors.accent else colors.border, RoundedCornerShape(12.dp))
                            .clickable { contentRating = rating }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            rating,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) colors.accentSoft else colors.fg,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Ignore scanlator groups")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Aurora", "Pyre", "Petals", "Nightowl").forEach { s ->
                    OtakuChip(
                        label = s,
                        trailingLabel = "×",
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterToggleRow(
    label: String,
    sub: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalOtakuColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface2)
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.fg)
            Text(sub, fontSize = 11.sp, color = colors.fgDim, modifier = Modifier.padding(top = 2.dp))
        }
        OtakuToggle(checked = checked, onCheckedChange = { onToggle() })
    }
}
