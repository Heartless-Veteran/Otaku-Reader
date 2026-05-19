package app.otakureader.domain.backup

data class ImportResult(
    val mangaImported: Int,
    val chaptersImported: Int,
    val categoriesImported: Int,
    val skipped: Int,
    val totalManga: Int,
    val totalChapters: Int,
)
