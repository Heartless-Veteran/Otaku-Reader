package app.otakureader.domain.repository

import app.otakureader.domain.model.ReadingList
import app.otakureader.domain.model.ReadingListItem
import app.otakureader.domain.model.ReadingListMangaItem
import kotlinx.coroutines.flow.Flow

interface ReadingListRepository {
    fun getAllLists(): Flow<List<ReadingList>>
    suspend fun getListById(listId: Long): ReadingList?
    suspend fun createList(name: String, description: String?, color: Int?): Long
    suspend fun updateList(list: ReadingList)
    suspend fun deleteList(listId: Long)
    
    suspend fun addMangaToList(listId: Long, mangaId: Long, note: String? = null)
    suspend fun removeMangaFromList(listId: Long, mangaId: Long)
    suspend fun isMangaInList(listId: Long, mangaId: Long): Boolean
    fun getListsForManga(mangaId: Long): Flow<List<ReadingListItem>>
    suspend fun updateItemNote(listId: Long, mangaId: Long, note: String?)
    suspend fun reorderItem(listId: Long, mangaId: Long, sortOrder: Int)
    fun getItemCount(listId: Long): Flow<Int>
    
    /**
     * Get a list with all its manga (for UI display).
     * Junction metadata (note, sortOrder, addedAt) is preserved per manga entry.
     */
    fun getListWithManga(listId: Long): Flow<Pair<ReadingList, List<ReadingListMangaItem>>>
}