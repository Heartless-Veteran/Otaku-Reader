package com.otakureader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.otakureader.ui.components.*
import com.otakureader.ui.theme.LocalOtakuColors

private data class CategoryItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val count: Int,
    val colorHex: String,
)

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val colors = LocalOtakuColors.current
    val cats = listOf(
        CategoryItem("Reading", Icons.Default.MenuBook, 12, "#6B4EFF"),
        CategoryItem("On Hold", Icons.Default.PauseCircle, 8, "#FBBF24"),
        CategoryItem("Completed", Icons.Default.CheckCircle, 64, "#4ADE80"),
        CategoryItem("Plan to Read", Icons.Default.Bookmark, 38, "#FF6B9D"),
        CategoryItem("Favorites", Icons.Default.Star, 19, "#4DDFFF"),
        CategoryItem("Action", Icons.Default.LocalFireDepartment, 42, "#F87171"),
    )

    Scaffold(containerColor = colors.bg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface1),
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = colors.fgMuted, modifier = Modifier.size(18.dp))
                }
                Text("Categories", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.fg, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface1),
                ) {
                    Icon(Icons.Default.Add, null, tint = colors.accentSoft, modifier = Modifier.size(18.dp))
                }
            }

            SectionHeader(
                "Drag to reorder · Tap icon to change",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cats.size) { i ->
                    val cat = cats[i]
                    val catColor = try {
                        Color(cat.colorHex.toColorInt())
                    } catch (e: Exception) {
                        colors.accent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface1)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(12.dp, 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            null,
                            tint = colors.fgDim,
                            modifier = Modifier.size(14.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(catColor.copy(alpha = 0.13f))
                                .border(1.dp, catColor.copy(alpha = 0.33f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(cat.icon, null, tint = catColor, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.fg)
                            MonoLabel("${cat.count} entries", fontSize = 11, modifier = Modifier.padding(top = 2.dp))
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, null, tint = colors.fgMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
