package app.otakureader.sourceapi

data class MangasPage(
    val mangas: List<SManga>,
    val hasNextPage: Boolean,
)
