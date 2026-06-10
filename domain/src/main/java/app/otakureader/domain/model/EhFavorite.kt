package app.otakureader.domain.model

/** A single gallery entry parsed from the E-Hentai favorites page. */
data class EhFavorite(
    /** Relative gallery path, e.g. "/g/123456/abc12345/". */
    val galleryUrl: String,
    val title: String,
    val thumbnailUrl: String?,
)
