package app.otakureader.data.repository

import app.otakureader.core.database.dao.CategorizationResultDao
import app.otakureader.core.database.entity.CategorizationResultEntity
import app.otakureader.domain.model.CategorizationResult
import app.otakureader.domain.model.CategorySuggestion
import app.otakureader.domain.model.CategoryType
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategorizationRepositoryImplTest {

    private lateinit var dao: CategorizationResultDao
    private lateinit var repository: CategorizationRepositoryImpl

    private val sampleSuggestions = listOf(
        CategorySuggestion(
            categoryName = "Action",
            confidenceScore = 0.95f,
            categoryType = CategoryType.GENRE
        ),
        CategorySuggestion(
            categoryName = "Shounen",
            confidenceScore = 0.88f,
            categoryType = CategoryType.DEMOGRAPHIC
        )
    )

    private val sampleResult = CategorizationResult(
        mangaId = 42L,
        suggestions = sampleSuggestions,
        appliedCategories = listOf("Action"),
        wasAutoApplied = false,
        timestamp = 1_000_000L
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = CategorizationRepositoryImpl(dao)
    }

    // ── saveCategorizationResult ──────────────────────────────────────────────

    @Test
    fun saveCategorizationResult_callsDaoInsert() = runTest {
        repository.saveCategorizationResult(sampleResult)

        coVerify(exactly = 1) { dao.insert(any()) }
    }

    @Test
    fun saveCategorizationResult_serializesCorrectMangaId() = runTest {
        var capturedEntity: CategorizationResultEntity? = null
        coEvery { dao.insert(any()) } answers {
            capturedEntity = firstArg()
        }

        repository.saveCategorizationResult(sampleResult)

        assertNotNull(capturedEntity)
        assertEquals(42L, capturedEntity!!.mangaId)
    }

    @Test
    fun saveCategorizationResult_storesWasAutoAppliedFlag() = runTest {
        var capturedEntity: CategorizationResultEntity? = null
        coEvery { dao.insert(any()) } answers {
            capturedEntity = firstArg()
        }
        val autoAppliedResult = sampleResult.copy(wasAutoApplied = true)

        repository.saveCategorizationResult(autoAppliedResult)

        assertTrue(capturedEntity!!.wasAutoApplied)
    }

    @Test
    fun saveCategorizationResult_storesTimestamp() = runTest {
        var capturedEntity: CategorizationResultEntity? = null
        coEvery { dao.insert(any()) } answers {
            capturedEntity = firstArg()
        }

        repository.saveCategorizationResult(sampleResult)

        assertEquals(1_000_000L, capturedEntity!!.timestamp)
    }

    // ── getCategorizationResult ───────────────────────────────────────────────

    @Test
    fun getCategorizationResult_whenDaoReturnsNull_returnsNull() = runTest {
        coEvery { dao.getByMangaId(42L) } returns null

        val result = repository.getCategorizationResult(42L)

        assertNull(result)
    }

    @Test
    fun getCategorizationResult_whenEntityExists_returnsDomainModel() = runTest {
        coEvery { dao.getByMangaId(42L) } returns buildEntity(mangaId = 42L)

        val result = repository.getCategorizationResult(42L)

        assertNotNull(result)
        assertEquals(42L, result!!.mangaId)
    }

    @Test
    fun getCategorizationResult_deserializesSuggestions() = runTest {
        val entity = buildEntity(mangaId = 1L)
        coEvery { dao.getByMangaId(1L) } returns entity

        val result = repository.getCategorizationResult(1L)

        assertNotNull(result)
        assertEquals(2, result!!.suggestions.size)
        assertEquals("Action", result.suggestions[0].categoryName)
        assertEquals(0.95f, result.suggestions[0].confidenceScore)
        assertEquals(CategoryType.GENRE, result.suggestions[0].categoryType)
    }

    @Test
    fun getCategorizationResult_deserializesAppliedCategories() = runTest {
        coEvery { dao.getByMangaId(42L) } returns buildEntity(mangaId = 42L)

        val result = repository.getCategorizationResult(42L)

        assertEquals(listOf("Action"), result!!.appliedCategories)
    }

    @Test
    fun getCategorizationResult_withCorruptedSuggestionsJson_returnsEmptySuggestions() = runTest {
        val corruptEntity = CategorizationResultEntity(
            mangaId = 5L,
            suggestionsJson = "this is not valid JSON!!!",
            appliedCategoriesJson = "[]",
            wasAutoApplied = false,
            timestamp = 0L
        )
        coEvery { dao.getByMangaId(5L) } returns corruptEntity

        val result = repository.getCategorizationResult(5L)

        assertNotNull(result)
        assertEquals(5L, result!!.mangaId)
        assertTrue(result.suggestions.isEmpty())
        assertTrue(result.appliedCategories.isEmpty())
    }

    @Test
    fun getCategorizationResult_withCorruptedAppliedCategoriesJson_returnsEmptyList() = runTest {
        val entity = buildEntity(mangaId = 7L)
        val corruptEntity = entity.copy(appliedCategoriesJson = "{{invalid}}")
        coEvery { dao.getByMangaId(7L) } returns corruptEntity

        val result = repository.getCategorizationResult(7L)

        assertNotNull(result)
        assertEquals(7L, result!!.mangaId)
        assertEquals(2, result.suggestions.size)
        assertTrue(result.appliedCategories.isEmpty())
    }

    // ── getCategorizationResultFlow ───────────────────────────────────────────

    @Test
    fun getCategorizationResultFlow_emitsNullWhenNoEntity() = runTest {
        every { dao.getByMangaIdFlow(99L) } returns flowOf(null)

        repository.getCategorizationResultFlow(99L).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun getCategorizationResultFlow_emitsDomainModelWhenEntityExists() = runTest {
        every { dao.getByMangaIdFlow(42L) } returns flowOf(buildEntity(mangaId = 42L))

        repository.getCategorizationResultFlow(42L).test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals(42L, result!!.mangaId)
            awaitComplete()
        }
    }

    @Test
    fun getCategorizationResultFlow_emitsMultipleUpdates() = runTest {
        every { dao.getByMangaIdFlow(42L) } returns flowOf(
            null,
            buildEntity(mangaId = 42L)
        )

        repository.getCategorizationResultFlow(42L).test {
            assertNull(awaitItem())
            assertNotNull(awaitItem())
            awaitComplete()
        }
    }

    // ── deleteCategorizationResult ────────────────────────────────────────────

    @Test
    fun deleteCategorizationResult_callsDaoDeleteByMangaId() = runTest {
        repository.deleteCategorizationResult(42L)

        coVerify(exactly = 1) { dao.deleteByMangaId(42L) }
    }

    // ── getPendingSuggestions ─────────────────────────────────────────────────

    @Test
    fun getPendingSuggestions_emitsEmptyListWhenNoPending() = runTest {
        every { dao.getPendingSuggestions() } returns flowOf(emptyList())

        repository.getPendingSuggestions().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun getPendingSuggestions_emitsDomainModels() = runTest {
        val entities = listOf(
            buildEntity(mangaId = 1L),
            buildEntity(mangaId = 2L)
        )
        every { dao.getPendingSuggestions() } returns flowOf(entities)

        repository.getPendingSuggestions().test {
            val results = awaitItem()
            assertEquals(2, results.size)
            assertEquals(1L, results[0].mangaId)
            assertEquals(2L, results[1].mangaId)
            awaitComplete()
        }
    }

    // ── markSuggestionsAsReviewed ─────────────────────────────────────────────

    @Test
    fun markSuggestionsAsReviewed_callsDaoMarkAsReviewed() = runTest {
        repository.markSuggestionsAsReviewed(42L)

        coVerify(exactly = 1) { dao.markAsReviewed(42L) }
    }

    // ── Roundtrip: save → retrieve ────────────────────────────────────────────

    @Test
    fun saveAndRetrieve_preservesSuggestionsIntegrity() = runTest {
        // Capture what gets inserted and use it for retrieval
        var storedEntity: CategorizationResultEntity? = null
        coEvery { dao.insert(any()) } answers {
            storedEntity = firstArg()
        }
        coEvery { dao.getByMangaId(42L) } answers {
            storedEntity
        }

        repository.saveCategorizationResult(sampleResult)
        val retrieved = repository.getCategorizationResult(42L)

        assertNotNull(retrieved)
        assertEquals(42L, retrieved!!.mangaId)
        assertEquals(2, retrieved.suggestions.size)

        val actionSuggestion = retrieved.suggestions.first { it.categoryName == "Action" }
        assertEquals(0.95f, actionSuggestion.confidenceScore)
        assertEquals(CategoryType.GENRE, actionSuggestion.categoryType)

        val shounenSuggestion = retrieved.suggestions.first { it.categoryName == "Shounen" }
        assertEquals(0.88f, shounenSuggestion.confidenceScore)
        assertEquals(CategoryType.DEMOGRAPHIC, shounenSuggestion.categoryType)

        assertEquals(listOf("Action"), retrieved.appliedCategories)
        assertFalse(retrieved.wasAutoApplied)
    }

    @Test
    fun saveAndRetrieve_withAllCategoryTypes_preservesEnumValues() = runTest {
        val allTypeSuggestions = listOf(
            CategorySuggestion("Action", 0.9f, CategoryType.GENRE),
            CategorySuggestion("Shounen", 0.8f, CategoryType.DEMOGRAPHIC),
            CategorySuggestion("Isekai", 0.7f, CategoryType.THEME),
            CategorySuggestion("OP MC", 0.6f, CategoryType.TROPE),
            CategorySuggestion("MyList", 0.5f, CategoryType.CUSTOM)
        )
        val result = CategorizationResult(
            mangaId = 99L,
            suggestions = allTypeSuggestions,
            timestamp = 0L
        )

        var storedEntity: CategorizationResultEntity? = null
        coEvery { dao.insert(any()) } answers { storedEntity = firstArg() }
        coEvery { dao.getByMangaId(99L) } answers { storedEntity }

        repository.saveCategorizationResult(result)
        val retrieved = repository.getCategorizationResult(99L)

        assertNotNull(retrieved)
        assertEquals(5, retrieved!!.suggestions.size)
        val types = retrieved.suggestions.map { it.categoryType }
        assertTrue(types.containsAll(CategoryType.entries))
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Builds a [CategorizationResultEntity] that represents [sampleResult]
     * for the given [mangaId].
     */
    private fun buildEntity(mangaId: Long): CategorizationResultEntity {
        val suggestionsJson = """[
            {"categoryName":"Action","confidenceScore":0.95,"categoryType":"GENRE"},
            {"categoryName":"Shounen","confidenceScore":0.88,"categoryType":"DEMOGRAPHIC"}
        ]"""
        val appliedCategoriesJson = """["Action"]"""
        return CategorizationResultEntity(
            mangaId = mangaId,
            suggestionsJson = suggestionsJson,
            appliedCategoriesJson = appliedCategoriesJson,
            wasAutoApplied = false,
            timestamp = 1_000_000L
        )
    }
}
