package app.otakureader.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Streak card showing current streak, best streak, and a 30-day heatmap.
 */
@Composable
fun StreakCard(
    currentStreak: Int,
    bestStreak: Int,
    readingActivityByDay: List<Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with fire icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (currentStreak > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reading Streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Streak numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakNumber(
                    value = currentStreak,
                    label = "Current",
                    isActive = currentStreak > 0
                )
                StreakNumber(
                    value = bestStreak,
                    label = "Best",
                    isActive = false
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 30-day heatmap
            Text(
                text = "Last 30 Days",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val last30Days = readingActivityByDay.takeLast(30)
            val maxValue = max(1, last30Days.maxOrNull() ?: 0)
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                last30Days.chunked(6).forEach { week -
gt;
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        week.forEach { count -
>
                            val intensity = (count.toFloat() / maxValue).coerceIn(0f, 1f)
                            val color = when {
                                count == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                intensity < 0.3f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                intensity < 0.6f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakNumber(
    value: Int,
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
