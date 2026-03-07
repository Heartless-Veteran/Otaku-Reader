package app.otakureader.domain.model

data class Category(
    val id: Long,
    val name: String,
    val order: Int = 0,
    val mangaCount: Int = 0
)
