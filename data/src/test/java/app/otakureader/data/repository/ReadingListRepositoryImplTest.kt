package app.otakureader.data.repository

import app.otakureader.core.database.dao.ReadingListDao
import app.otakureader.core.database.entity.ReadingListEntity
import app.otakureader.core.database.entity.ReadingListItemEntity
import app.otakureader.domain.model.ReadingList
import app.otakureader.domain.model.ReadingListItem
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReadingListRepositoryImplTest {

    private lateinit var readingListDao: ReadingListDao
    private lateinit var repository: ReadingListRepositoryImpl

    private fun makeListEntity(
        id: Long = 1L,
        name: String = "My List",
        description: String? = null,
        color: Int? = null,
        sortOrder: Int = 0
    ) = ReadingListEntity(
        id = id,
        name = name,
        description = description,
        color = color,
        sortOrder = sortOrder
    )

    private fun makeItemEntity(
        listId: Long = 1L,
        mangaId: Long = 10L,
        sortOrder: Int = 0,
        note: String? = null
    ) = ReadingListItemEntity(
        listId = listId,
        mangaId = mangaId,
        sortOrder = sortOrder,
        note = note
    )

    @Before
    fun setUp() {
        readingListDao = mockk()
        repository = ReadingListRepositoryImpl(readingListDao)
    }

    // ---- getAllLists ----

    @Test
    fun `getAllLists returns mapped domain models`() = runTest {
        val entities = listOf(
            makeListEntity(id = 1L, name = "Summer Binge"),
            makeListEntity(id = 2L, name = "Re-read Later")
        )
        every { readingListDao.getAllLists() } returns flowOf(entities)

        repository.getAllLists().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Summer Binge", result[0].name)
            assertEquals("Re-read Later", result[1].name)
            awaitComplete()
        }
    }

    @Test
    fun `getAllLists with no lists returns empty`() = runTest {
        every { readingListDao.getAllLists() } returns flowOf(emptyList())

        repository.getAllLists().test {
            assertEquals(emptyList<ReadingList>(), awaitItem())
            awaitComplete()
        }
    }

    // ---- getListById ----

    @Test
    fun `getListById returns mapped domain model when found`() = runTest {
        coEvery { readingListDao.getListById(1L) } returns makeListEntity(id = 1L, name = "My List")

        val result = repository.getListById(1L)

        assertEquals(1L, result?.id)
        assertEquals("My List", result?.name)
    }

    @Test
    fun `getListById returns null when not found`() = runTest {
        coEvery { readingListDao.getListById(999L) } returns null

        val result = repository.getListById(999L)

        assertNull(result)
    }

    // ---- createList ----

    @Test
    fun `createList inserts entity and returns generated id`() = runTest {
        coEvery { readingListDao.insertList(any()) } returns 42L

        val id = repository.createList("New List", description = "Desc", color = 0xFF0000)

        assertEquals(42L, id)
        coVerify {
            readingListDao.insertList(match {
                it.name == "New List" && it.description == "Desc" && it.color == 0xFF0000
            })
        }
    }

    @Test
    fun `createList with null description and color`() = runTest {
        coEvery { readingListDao.insertList(any()) } returns 1L

        repository.createList("Minimal", null, null)

        coVerify {
            readingListDao.insertList(match { it.name == "Minimal" && it.description == null && it.color == null })
        }
    }

    // ---- updateList ----

    @Test
    fun `updateList calls dao with correct entity conversion`() = runTest {
        val list = ReadingList(id = 1L, name = "Updated", description = "New desc")
        coEvery { readingListDao.updateList(any()) } returns Unit

        repository.updateList(list)

        coVerify {
            readingListDao.updateList(match { it.id == 1L && it.name == "Updated" })
        }
    }

    // ---- deleteList ----

    @Test
    fun `deleteList calls dao deleteListById`() = runTest {
        coEvery { readingListDao.deleteListById(5L) } returns Unit

        repository.deleteList(5L)

        coVerify { readingListDao.deleteListById(5L) }
    }

    // ---- addMangaToList ----

    @Test
    fun `addMangaToList inserts item entity with correct values`() = runTest {
        coEvery { readingListDao.addMangaToList(any()) } returns Unit

        repository.addMangaToList(listId = 1L, mangaId = 10L, note = "Great manga!")

        coVerify {
            readingListDao.addMangaToList(match {
                it.listId == 1L && it.mangaId == 10L && it.note == "Great manga!"
            })
        }
    }

    @Test
    fun `addMangaToList with null note`() = runTest {
        coEvery { readingListDao.addMangaToList(any()) } returns Unit

        repository.addMangaToList(listId = 1L, mangaId = 10L, note = null)

        coVerify { readingListDao.addMangaToList(match { it.note == null }) }
    }

    // ---- removeMangaFromList ----

    @Test
    fun `removeMangaFromList calls dao with correct args`() = runTest {
        coEvery { readingListDao.removeMangaFromList(1L, 10L) } returns Unit

        repository.removeMangaFromList(1L, 10L)

        coVerify { readingListDao.removeMangaFromList(1L, 10L) }
    }

    // ---- isMangaInList ----

    @Test
    fun `isMangaInList returns true when manga is in list`() = runTest {
        coEvery { readingListDao.isMangaInList(1L, 10L) } returns true

        assertTrue(repository.isMangaInList(1L, 10L))
    }

    @Test
    fun `isMangaInList returns false when manga is not in list`() = runTest {
        coEvery { readingListDao.isMangaInList(1L, 99L) } returns false

        assertFalse(repository.isMangaInList(1L, 99L))
    }

    // ---- getListsForManga ----

    @Test
    fun `getListsForManga returns list items for the manga`() = runTest {
        val items = listOf(makeItemEntity(listId = 1L, mangaId = 10L, note = "Good"))
        every { readingListDao.getListsForManga(10L) } returns flowOf(items)

        repository.getListsForManga(10L).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(1L, result[0].listId)
            assertEquals(10L, result[0].mangaId)
            awaitComplete()
        }
    }

    @Test
    fun `getListsForManga returns empty when manga is not in any list`() = runTest {
        every { readingListDao.getListsForManga(99L) } returns flowOf(emptyList())

        repository.getListsForManga(99L).test {
            assertEquals(emptyList<ReadingListItem>(), awaitItem())
            awaitComplete()
        }
    }

    // ---- updateItemNote ----

    @Test
    fun `updateItemNote calls dao with correct args`() = runTest {
        coEvery { readingListDao.updateItemNote(1L, 10L, "Updated note") } returns Unit

        repository.updateItemNote(1L, 10L, "Updated note")

        coVerify { readingListDao.updateItemNote(1L, 10L, "Updated note") }
    }

    @Test
    fun `updateItemNote with null clears note`() = runTest {
        coEvery { readingListDao.updateItemNote(1L, 10L, null) } returns Unit

        repository.updateItemNote(1L, 10L, null)

        coVerify { readingListDao.updateItemNote(1L, 10L, null) }
    }

    // ---- reorderItem ----

    @Test
    fun `reorderItem calls dao with correct sort order`() = runTest {
        coEvery { readingListDao.updateItemSortOrder(1L, 10L, 5) } returns Unit

        repository.reorderItem(1L, 10L, 5)

        coVerify { readingListDao.updateItemSortOrder(1L, 10L, 5) }
    }

    // ---- getItemCount ----

    @Test
    fun `getItemCount returns count flow from dao`() = runTest {
        every { readingListDao.getItemCount(1L) } returns flowOf(3)

        repository.getItemCount(1L).test {
            assertEquals(3, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getItemCount returns zero for empty list`() = runTest {
        every { readingListDao.getItemCount(99L) } returns flowOf(0)

        repository.getItemCount(99L).test {
            assertEquals(0, awaitItem())
            awaitComplete()
        }
    }

    // ---- entity to domain mapping ----

    @Test
    fun `reading list entity is correctly mapped to domain model`() = runTest {
        val entity = makeListEntity(
            id = 7L, name = "Epic Reads", description = "Best of the best",
            color = 0x0000FF, sortOrder = 3
        )
        every { readingListDao.getAllLists() } returns flowOf(listOf(entity))

        repository.getAllLists().test {
            val list = awaitItem().first()
            assertEquals(7L, list.id)
            assertEquals("Epic Reads", list.name)
            assertEquals("Best of the best", list.description)
            assertEquals(3, list.sortOrder)
            awaitComplete()
        }
    }
}
