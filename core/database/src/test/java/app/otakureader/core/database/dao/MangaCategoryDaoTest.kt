package app.otakureader.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.otakureader.core.database.OtakuReaderDatabase
import app.otakureader.core.database.entity.CategoryEntity
import app.otakureader.core.database.entity.MangaCategoryEntity
import app.otakureader.core.database.entity.MangaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaCategoryDaoTest {

 private lateinit var database: OtakuReaderDatabase
 private lateinit var mangaCategoryDao: MangaCategoryDao
 private lateinit var mangaDao: MangaDao
 private lateinit var categoryDao: CategoryDao

 @Before
 fun setup() {
 database = Room.inMemoryDatabaseBuilder(
 ApplicationProvider.getApplicationContext(),
 OtakuReaderDatabase::class.java
 ).allowMainThreadQueries().build()
 mangaCategoryDao = database.mangaCategoryDao()
 mangaDao = database.mangaDao()
 categoryDao = database.categoryDao()
 }

 @After
 fun teardown() {
 database.close()
 }

 @Test
 fun insertMangaCategory_createsLink() = runBlocking {
 val manga = MangaEntity(id = 1L, title = "Test", sourceId = 1L, url = "url", favorite = true)
 val category = CategoryEntity(id = 1L, name = "Reading", order = 0)
 mangaDao.insert(manga)
 categoryDao.insert(category)

 mangaCategoryDao.insert(MangaCategoryEntity(mangaId = 1L, categoryId = 1L))

 val categories = mangaCategoryDao.getCategoriesForManga(1L).first()
 assertEquals(1, categories.size)
 assertEquals(1L, categories[0].categoryId)
 }

 @Test
 fun deleteMangaCategory_removesLink() = runBlocking {
 val manga = MangaEntity(id = 1L, title = "Test", sourceId = 1L, url = "url", favorite = true)
 val category = CategoryEntity(id = 1L, name = "Reading", order = 0)
 mangaDao.insert(manga)
 categoryDao.insert(category)

 val link = MangaCategoryEntity(mangaId = 1L, categoryId = 1L)
 mangaCategoryDao.insert(link)
 mangaCategoryDao.delete(link)

 val categories = mangaCategoryDao.getCategoriesForManga(1L).first()
 assertTrue(categories.isEmpty())
 }

 @Test
 fun getMangaForCategory_returnsOnlyLinked() = runBlocking {
 // Insert 2 manga
 val manga1 = MangaEntity(id = 1L, title = "Manga 1", sourceId = 1L, url = "url1", favorite = true)
 val manga2 = MangaEntity(id = 2L, title = "Manga 2", sourceId = 1L, url = "url2", favorite = true)
 mangaDao.insert(manga1)
 mangaDao.insert(manga2)

 // Insert 2 categories
 val cat1 = CategoryEntity(id = 1L, name = "Reading", order = 0)
 val cat2 = CategoryEntity(id = 2L, name = "Completed", order = 1)
 categoryDao.insert(cat1)
 categoryDao.insert(cat2)

 // Link: manga1->cat1, manga2->cat2
 mangaCategoryDao.insert(MangaCategoryEntity(mangaId = 1L, categoryId = 1L))
 mangaCategoryDao.insert(MangaCategoryEntity(mangaId = 2L, categoryId = 2L))

 val readingManga = mangaCategoryDao.getMangaForCategory(1L).first()
 assertEquals(1, readingManga.size)
 assertEquals("Manga 1", readingManga[0].title)

 val completedManga = mangaCategoryDao.getMangaForCategory(2L).first()
 assertEquals(1, completedManga.size)
 assertEquals("Manga 2", completedManga[0].title)
 }

 @Test
 fun deleteManga_deletesLinksViaCascade() = runBlocking {
 val manga = MangaEntity(id = 1L, title = "Test", sourceId = 1L, url = "url", favorite = true)
 val category = CategoryEntity(id = 1L, name = "Reading", order = 0)
 mangaDao.insert(manga)
 categoryDao.insert(category)
 mangaCategoryDao.insert(MangaCategoryEntity(mangaId = 1L, categoryId = 1L))

 // Verify link exists
 val linksBefore = mangaCategoryDao.getCategoriesForManga(1L).first()
 assertEquals(1, linksBefore.size)

 // Delete manga (should cascade)
 mangaDao.deleteById(1L)

 // Link should be gone
 val linksAfter = mangaCategoryDao.getCategoriesForManga(1L).first()
 assertTrue(linksAfter.isEmpty())
 }

 @Test
 fun insertDuplicateLink_ignoresConflict() = runBlocking {
 val manga = MangaEntity(id = 1L, title = "Test", sourceId = 1L, url = "url", favorite = true)
 val category = CategoryEntity(id = 1L, name = "Reading", order = 0)
 mangaDao.insert(manga)
 categoryDao.insert(category)

 val link = MangaCategoryEntity(mangaId = 1L, categoryId = 1L)
 mangaCategoryDao.insert(link)
 mangaCategoryDao.insert(link) // Duplicate

 val categories = mangaCategoryDao.getCategoriesForManga(1L).first()
 assertEquals(1, categories.size)
 }
}
