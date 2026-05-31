package app.otakureader.domain.model

/** Summary of a Tachiyomi/Mihon backup's contents, shown to the user before importing. */
data class TachiyomiBackupPreview(
    val mangaCount: Int,
    val categoryCount: Int,
    val chapterCount: Int,
    val trackingCount: Int,
)
