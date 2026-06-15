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

    @Test
    fun `from source filters by sourceId`() {
        val library = listOf(
            manga(1).copy(sourceId = 1),
            manga(2).copy(sourceId = 2),
            manga(3).copy(sourceId = 1),
        )
        val result = useCase(listOf(DynamicCategoryRule.FromSource(1)), library, now)
        assertEquals(setOf(1L, 3L), result)
    }

    @Test
    fun `user dropped rule filters dropped manga`() {
        val library = listOf(
            manga(1).copy(userDropped = true),
            manga(2).copy(userDropped = false),
            manga(3).copy(userDropped = true),
        )
        val result = useCase(listOf(DynamicCategoryRule.UserDropped), library, now)
        assertEquals(setOf(1L, 3L), result)
    }

    @Test
    fun `user completed rule filters user completed manga`() {
        val library = listOf(
            manga(1).copy(userCompleted = true),
            manga(2).copy(userCompleted = false),
            manga(3).copy(userCompleted = true),
        )
        val result = useCase(listOf(DynamicCategoryRule.UserCompleted), library, now)
        assertEquals(setOf(1L, 3L), result)
    }

    @Test
    fun `currently reading filters manga with last read activity`() {
        val library = listOf(
            manga(1).copy(lastRead = now - TimeUnit.HOURS.toMillis(2)),
            manga(2).copy(lastRead = null),
            manga(3).copy(lastRead = now - TimeUnit.DAYS.toMillis(5)),
        )
        val result = useCase(listOf(DynamicCategoryRule.CurrentlyReading), library, now)
        assertEquals(setOf(1L, 3L), result)
    }

    @Test
    fun `inactive for days filters never-read manga older than threshold`() {
        val library = listOf(
            manga(1, dateAdded = now - TimeUnit.DAYS.toMillis(20)).copy(lastRead = null),
            manga(2, dateAdded = now - TimeUnit.DAYS.toMillis(5)).copy(lastRead = null),
            manga(3, dateAdded = now - TimeUnit.DAYS.toMillis(20)).copy(lastRead = now - TimeUnit.DAYS.toMillis(25)),
        )
        val result = useCase(listOf(DynamicCategoryRule.InactiveForDays(14)), library, now)
        // manga(1): added >14 days ago, never read -> matches
        // manga(2): added <14 days ago -> doesn't match
        // manga(3): last read >14 days ago -> matches
        assertEquals(setOf(1L, 3L), result)
    }

    @Test
    fun `inactive for days filters recently read manga correctly`() {
        val library = listOf(
            manga(1).copy(lastRead = now - TimeUnit.DAYS.toMillis(5)),  // Active
            manga(2).copy(lastRead = now - TimeUnit.DAYS.toMillis(20)), // Inactive
            manga(3).copy(lastRead = now - TimeUnit.DAYS.toMillis(14)), // Exactly at boundary
        )
        val result = useCase(listOf(DynamicCategoryRule.InactiveForDays(14)), library, now)
        // manga(1): read 5 days ago, not inactive -> doesn't match
        // manga(2): read 20 days ago, inactive -> matches
        // manga(3): read exactly 14 days ago, inactive -> matches
        assertEquals(setOf(2L, 3L), result)
    }
}
