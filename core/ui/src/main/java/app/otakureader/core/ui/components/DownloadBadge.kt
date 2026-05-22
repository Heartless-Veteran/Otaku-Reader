package app.otakureader.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DownloadBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = Color(0xFF4CAF50),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
