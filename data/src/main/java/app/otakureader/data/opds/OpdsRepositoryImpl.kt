package app.otakureader.data.opds

import app.otakureader.core.database.dao.OpdsServerDao
import app.otakureader.core.database.entity.OpdsServerEntity
import app.otakureader.domain.model.OpdsFeed
import app.otakureader.domain.model.OpdsServer
import app.otakureader.domain.repository.OpdsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpdsRepositoryImpl @Inject constructor(
    private val opdsServerDao: OpdsServerDao,
    private val opdsClient: OpdsClient
) : OpdsRepository {

    override fun getServers(): Flow<List<OpdsServer>> {
        return opdsServerDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getServer(serverId: Long): OpdsServer? {
        return opdsServerDao.getById(serverId)?.toDomain()
    }

    override suspend fun saveServer(server: OpdsServer): Long {
        return if (server.id == 0L) {
            opdsServerDao.insert(server.toEntity())
        } else {
            opdsServerDao.update(server.toEntity())
            server.id
        }
    }

    override suspend fun deleteServer(serverId: Long) {
        opdsServerDao.deleteById(serverId)
    }

    override suspend fun browseCatalog(server: OpdsServer, feedUrl: String): Result<OpdsFeed> {
        return try {
            val feed = opdsClient.fetchFeed(server, feedUrl)
            Result.success(feed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchCatalog(
        server: OpdsServer,
        searchUrl: String,
        query: String
    ): Result<OpdsFeed> {
        return try {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
            // Check if searchUrl is an OpenSearch description URL
            val actualSearchUrl = if (searchUrl.contains("opensearchdescription")) {
                val template = opdsClient.fetchSearchTemplate(server, searchUrl)
                template?.replace("{searchTerms}", encodedQuery)
                    ?: throw OpdsException("Could not find search template")
            } else {
                // Assume it's a direct search URL template
                searchUrl.replace("{searchTerms}", encodedQuery)
            }
            val feed = opdsClient.fetchFeed(server, actualSearchUrl)
            Result.success(feed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun OpdsServerEntity.toDomain(): OpdsServer = OpdsServer(
        id = id,
        name = name,
        url = url,
        username = username,
        password = password
    )

    private fun OpdsServer.toEntity(): OpdsServerEntity = OpdsServerEntity(
        id = id,
        name = name,
        url = url,
        username = username,
        password = password
    )
}
