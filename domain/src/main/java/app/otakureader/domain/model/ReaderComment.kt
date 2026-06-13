package app.otakureader.domain.model

/**
 * A private, on-device comment written from the reader's comments overlay.
 *
 * Scope is encoded by [chapterId]: null means a book-level comment that is visible from
 * any chapter of the manga; non-null ties the comment to one chapter.
 */
data class ReaderComment(
    val id: Long = 0,
    val mangaId: Long,
    val chapterId: Long? = null,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isBookScoped: Boolean get() = chapterId == null
}
