package app.otakureader.domain.repository

import app.otakureader.domain.model.FeedItem
import app.otakureader.domain.model.FeedSavedSearch
import app.otakureader.domain.model.FeedSource
import kotlinx.coroutines.flow.Flow

/**
 * Repository for feed-related operations.
 * Manages feed sources, saved searches, and feed content.
 */
interface FeedRepository {
    // Feed Sources
    fun getFeedSources(): Flow<List<FeedSource>>
    suspend fun addFeedSource(sourceId: Long, sourceName: String)
    suspend fun removeFeedSource(sourceId: Long)
    suspend fun toggleFeedSource(sourceId: Long, enabled: Boolean)
    suspend fun updateFeedSourceOrder(sourceId: Long, order: Int)
    suspend fun updateFeedSourceItemCount(sourceId: Long, count: Int)

    // Feed Content
    fun getFeedItems(limit: Int = 100): Flow<List<FeedItem>>
    fun getFeedItemsForSource(sourceId: Long, limit: Int = 20): Flow<List<FeedItem>>
    suspend fun refreshFeed()
    suspend fun markFeedItemAsRead(feedItemId: Long)
    suspend fun clearFeedHistory()

    // Saved Searches
    fun getSavedSearches(): Flow<List<FeedSavedSearch>>
    suspend fun addSavedSearch(sourceId: Long, sourceName: String, query: String, filters: Map<String, String>)
    suspend fun removeSavedSearch(searchId: Long)
    suspend fun updateSavedSearchOrder(searchId: Long, order: Int)
}
