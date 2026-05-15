package app.otakureader.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
enum class ContentType {
    MANGA,
    MANHWA
}

@Immutable
enum class ReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    VERTICAL
}

fun ContentType.mangaAccent(): Color = Color(0xFFFF4757)
fun ContentType.manhwaAccent(): Color = Color(0xFF9B59B6)

fun detectContentType(sourceTag: String?, readingDirection: ReadingDirection): ContentType {
    if (sourceTag == null) return ContentType.MANGA
    val normalized = sourceTag.lowercase()
    return when {
        normalized.contains("korean") ||
        normalized.contains("korea") ||
        normalized.contains("webtoon") ||
        normalized.contains("manhwa") ||
        normalized.contains("naver") ||
        normalized.contains("kakao") ||
        normalized.contains("lezhin") ||
        normalized.contains("toon") ||
        normalized.contains("webtoons") -> ContentType.MANHWA
        normalized.contains("chinese") ||
        normalized.contains("manhua") -> ContentType.MANGA
        readingDirection == ReadingDirection.VERTICAL -> ContentType.MANHWA
        else -> ContentType.MANGA
    }
}
