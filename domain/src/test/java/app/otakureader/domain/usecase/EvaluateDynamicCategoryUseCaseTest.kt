package app.otakureader.domain.usecase

import app.otakureader.domain.model.DynamicCategoryRule
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class EvaluateDynamicCategoryUseCaseTest {

    private val useCase = EvaluateDynamicCategoryUseCase()
    private val now = 1_000_000_000_000L

    private fun manga(
        id: Long,
        unread: Int = 0,
        status: MangaStatus = MangaStatus.UNKNOWN,
        genre: List<String> = emptyList(),
        lastUpdate: Long = 0,
        dateAdded: Long = 0,
    ) = Manga(
        id = id,
        sourceId = 1,
        url = "/$id",
        title = "Manga $id",
        unreadCount = unread,
        status = status,
        genre = genre,
        lastUpdate = lastUpdate,
        dateAdded = dateAdded,
    )

    @Test
    fun `empty rules match nothing`() {
        val result = useCase(emptyList(), listOf(manga(1)), now)
        assertEquals(emptySet<Long>(), result)
    }

    @Test
    fun `unread at least filters by unread count`() {
        val library = listOf(manga(1, unread = 0), manga(2, unread = 3), manga(3, unread = 10))
        val result = useCase(listOf(DynamicCategoryRule.UnreadAtLeast(3)), library, now)
        assertEquals(setOf(2L, 3L), result)
    }

    @Test
    fun `status rules match completed and ongoing`() {
        val library = listOf(
            manga(1, status = MangaStatus.COMPLETED),
            manga(2, status = MangaStatus.ONGOING),
            manga(3, status = MangaStatus.CANCELLED),
        )
        assertEquals(setOf(1L), useCase(listOf(DynamicCategoryRule.Completed), library, now))
        assertEquals(setOf(2L), useCase(listOf(DynamicCategoryRule.Ongoing), library, now))
    }

    @Test
    fun `recently added respects the day window`() {
        val library = listOf(
            manga(1, dateAdded = now - TimeUnit.DAYS.toMillis(2)),
            manga(2, dateAdded = now - TimeUnit.DAYS.toMillis(40)),
            manga(3, dateAdded = 0),
        )
        val result = useCase(listOf(DynamicCategoryRule.RecentlyAdded(7)), library, now)
        assertEquals(setOf(1L), result)
    }

    @Test
    fun `multiple rules combine with AND`() {
        val library = listOf(
            manga(1, unread = 5, genre = listOf("Action")),
            manga(2, unread = 1, genre = listOf("Action")),
            manga(3, unread = 5, genre = listOf("Comedy")),
        )
        val rules = listOf(DynamicCategoryRule.UnreadAtLeast(3), DynamicCategoryRule.GenreContains("action"))
        assertEquals(setOf(1L), useCase(rules, library, now))
    }
}
