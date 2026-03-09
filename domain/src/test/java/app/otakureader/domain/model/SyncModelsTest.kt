package app.otakureader.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for sync data structures and serialization.
 */
class SyncModelsTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `SyncSnapshot serialization and deserialization`() {
        // Given
        val snapshot = SyncSnapshot(
            version = 1,
            createdAt = 1234567890L,
            deviceId = "test-device-123",
            deviceName = "Test Device",
            manga = listOf(
                SyncManga(
                    sourceId = 1L,
                    url = "/manga/test",
                    title = "Test Manga",
                    favorite = true,
                    categoryIds = listOf(1L, 2L),
                    lastModified = 1234567890L,
                    chapters = listOf(
                        SyncChapter(
                            url = "/chapter/1",
                            read = true,
                            bookmark = false,
                            lastPageRead = 10,
                            lastModified = 1234567890L
                        )
                    )
                )
            ),
            categories = listOf(
                SyncCategory(
                    id = 1L,
                    name = "Action",
                    order = 0,
                    lastModified = 1234567890L
                )
            ),
            metadata = SyncMetadata(
                syncVersion = 5L,
                baseSyncVersion = 4L,
                previousSnapshotHash = "abc123"
            )
        )

        // When
        val jsonString = json.encodeToString(snapshot)
        val deserialized = json.decodeFromString<SyncSnapshot>(jsonString)

        // Then
        assertEquals(snapshot.version, deserialized.version)
        assertEquals(snapshot.deviceId, deserialized.deviceId)
        assertEquals(snapshot.manga.size, deserialized.manga.size)
        assertEquals(snapshot.categories.size, deserialized.categories.size)

        // Verify manga data
        val originalManga = snapshot.manga[0]
        val deserializedManga = deserialized.manga[0]
        assertEquals(originalManga.sourceId, deserializedManga.sourceId)
        assertEquals(originalManga.url, deserializedManga.url)
        assertEquals(originalManga.title, deserializedManga.title)
        assertEquals(originalManga.favorite, deserializedManga.favorite)
        assertEquals(originalManga.chapters.size, deserializedManga.chapters.size)

        // Verify chapter data
        val originalChapter = originalManga.chapters[0]
        val deserializedChapter = deserializedManga.chapters[0]
        assertEquals(originalChapter.url, deserializedChapter.url)
        assertEquals(originalChapter.read, deserializedChapter.read)
        assertEquals(originalChapter.lastPageRead, deserializedChapter.lastPageRead)
    }

    @Test
    fun `SyncSnapshot with minimal data`() {
        // Given
        val snapshot = SyncSnapshot(
            deviceId = "minimal-device",
            manga = emptyList(),
            categories = emptyList()
        )

        // When
        val jsonString = json.encodeToString(snapshot)
        val deserialized = json.decodeFromString<SyncSnapshot>(jsonString)

        // Then
        assertEquals(SyncSnapshot.CURRENT_VERSION, deserialized.version)
        assertEquals("minimal-device", deserialized.deviceId)
        assertTrue(deserialized.manga.isEmpty())
        assertTrue(deserialized.categories.isEmpty())
    }

    @Test
    fun `SyncManga default values`() {
        // Given
        val manga = SyncManga(
            sourceId = 1L,
            url = "/test",
            title = "Test"
        )

        // Then
        assertFalse(manga.favorite)
        assertTrue(manga.categoryIds.isEmpty())
        assertTrue(manga.chapters.isEmpty())
        assertNull(manga.thumbnailUrl)
        assertNull(manga.notes)
    }

    @Test
    fun `SyncChapter default values`() {
        // Given
        val chapter = SyncChapter(url = "/chapter/1")

        // Then
        assertFalse(chapter.read)
        assertFalse(chapter.bookmark)
        assertEquals(0, chapter.lastPageRead)
    }

    @Test
    fun `SyncResult totalChanges calculation`() {
        // Given
        val result = SyncResult(
            success = true,
            mangaAdded = 5,
            mangaUpdated = 3,
            mangaDeleted = 2,
            chaptersUpdated = 10,
            categoriesAdded = 1,
            categoriesUpdated = 2,
            categoriesDeleted = 0
        )

        // Then
        assertEquals(23, result.totalChanges)
    }

    @Test
    fun `SyncResult with no changes`() {
        // Given
        val result = SyncResult(success = true)

        // Then
        assertEquals(0, result.totalChanges)
    }

    @Test
    fun `SyncMetadata default values`() {
        // Given
        val metadata = SyncMetadata()

        // Then
        assertEquals(0L, metadata.syncVersion)
        assertNull(metadata.baseSyncVersion)
        assertNull(metadata.previousSnapshotHash)
        assertNull(metadata.appVersion)
    }

    @Test
    fun `SyncSnapshot backward compatibility with missing fields`() {
        // Given - JSON with missing optional fields
        val jsonWithMissingFields = """
            {
                "version": 1,
                "createdAt": 1234567890,
                "deviceId": "old-device",
                "manga": [],
                "categories": []
            }
        """.trimIndent()

        // When
        val deserialized = json.decodeFromString<SyncSnapshot>(jsonWithMissingFields)

        // Then - Should use default values
        assertEquals("old-device", deserialized.deviceId)
        assertNull(deserialized.deviceName)
        assertTrue(deserialized.manga.isEmpty())
        assertEquals(SyncMetadata(), deserialized.metadata)
    }

    @Test
    fun `SyncSnapshot ignores unknown fields`() {
        // Given - JSON with extra unknown fields
        val jsonWithExtraFields = """
            {
                "version": 1,
                "createdAt": 1234567890,
                "deviceId": "test-device",
                "unknownField": "should be ignored",
                "manga": [],
                "categories": [],
                "futureFeature": { "data": "ignored" }
            }
        """.trimIndent()

        // When
        val deserialized = json.decodeFromString<SyncSnapshot>(jsonWithExtraFields)

        // Then - Should successfully parse despite unknown fields
        assertEquals("test-device", deserialized.deviceId)
        assertTrue(deserialized.manga.isEmpty())
    }

    @Test
    fun `SyncManga with multiple categories`() {
        // Given
        val manga = SyncManga(
            sourceId = 1L,
            url = "/manga/test",
            title = "Test Manga",
            favorite = true,
            categoryIds = listOf(1L, 2L, 3L)
        )

        // When
        val jsonString = json.encodeToString(manga)
        val deserialized = json.decodeFromString<SyncManga>(jsonString)

        // Then
        assertEquals(3, deserialized.categoryIds.size)
        assertTrue(deserialized.categoryIds.containsAll(listOf(1L, 2L, 3L)))
    }

    @Test
    fun `SyncChapter progress tracking`() {
        // Given
        val chapter = SyncChapter(
            url = "/chapter/1",
            read = false,
            bookmark = true,
            lastPageRead = 15
        )

        // When
        val jsonString = json.encodeToString(chapter)
        val deserialized = json.decodeFromString<SyncChapter>(jsonString)

        // Then
        assertFalse(deserialized.read)
        assertTrue(deserialized.bookmark)
        assertEquals(15, deserialized.lastPageRead)
    }
}
