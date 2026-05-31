package app.otakureader.domain.model

data class ImportResult(
    val mangaImported: Int,
    val chaptersImported: Int,
    val categoriesImported: Int,
    val skipped: Int,
    val totalManga: Int,
    val totalChapters: Int,
    val trackingImported: Int = 0,
)
