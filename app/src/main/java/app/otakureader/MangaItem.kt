package app.otakureader

import androidx.compose.ui.graphics.Color
import app.otakureader.core.ui.theme.ContentType

data class MangaItem(
    val id: String,
    val title: String,
    val coverColor: Color,
    val contentType: ContentType,
    val unreadCount: Int,
    val genres: List<String>,
    val synopsis: String,
)

val SampleManga = listOf(
    MangaItem(
        "1", "Attack on Titan", Color(0xFF6B4F3A),
        ContentType.MANGA, 5, listOf("Action", "Drama"),
        "In a world where humanity lives inside cities surrounded by enormous walls...",
    ),
    MangaItem(
        "2", "Solo Leveling", Color(0xFF1A2A4A),
        ContentType.MANHWA, 3, listOf("Action", "Fantasy"),
        "Weakest hunter Sung Jin-Woo gains the ability to level up in a world full of monsters.",
    ),
    MangaItem(
        "3", "Demon Slayer", Color(0xFF1A3A3A),
        ContentType.MANGA, 12, listOf("Action", "Supernatural"),
        "Tanjiro Kamado sets out to become a demon slayer after his family is slaughtered.",
    ),
    MangaItem(
        "4", "Tower of God", Color(0xFF1A1A3A),
        ContentType.MANHWA, 8, listOf("Fantasy", "Action"),
        "Bam enters the Tower to find his friend Rachel, climbing floor by floor.",
    ),
    MangaItem(
        "5", "One Piece", Color(0xFF3A2A1A),
        ContentType.MANGA, 1, listOf("Adventure", "Comedy"),
        "Monkey D. Luffy and his pirate crew search for the legendary One Piece treasure.",
    ),
    MangaItem(
        "6", "Noblesse", Color(0xFF2A1A3A),
        ContentType.MANHWA, 0, listOf("Supernatural", "Action"),
        "Raizel awakens from 820 years of slumber to discover a changed modern world.",
    ),
    MangaItem(
        "7", "Jujutsu Kaisen", Color(0xFF1A1A2A),
        ContentType.MANGA, 7, listOf("Action", "Supernatural"),
        "Yuji Itadori joins a secret organization of sorcerers to kill a powerful Curse.",
    ),
    MangaItem(
        "8", "True Beauty", Color(0xFF3A1A2A),
        ContentType.MANHWA, 2, listOf("Romance", "Drama"),
        "Jugyeong masters the art of makeup and transforms her appearance at a new school.",
    ),
)
