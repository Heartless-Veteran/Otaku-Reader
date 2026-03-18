package app.otakureader.domain.usecase

import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Advanced library search with support for:
 * - Exclude terms (prefix with "-")
 * - Exact phrases (quoted)
 * - Tag filtering
 * - Status filtering
 * - Author/artist search
 */
class SearchLibraryMangaUseCase @Inject constructor(
    private val mangaRepository: MangaRepository
) {
    operator fun invoke(query: String): Flow<List<Manga>> {
        return mangaRepository.getLibraryManga().map { mangaList ->
            if (query.isBlank()) return@map mangaList

            val searchCriteria = parseQuery(query)
            mangaList.filter { manga ->
                matchesCriteria(manga, searchCriteria)
            }
        }
    }

    private data class SearchCriteria(
        val includeTerms: List<String> = emptyList(),
        val excludeTerms: List<String> = emptyList(),
        val exactPhrases: List<String> = emptyList(),
        val tags: List<String> = emptyList(),
        val excludeTags: List<String> = emptyList(),
        val authorQuery: String? = null,
        val statusFilter: MangaStatusFilter? = null
    )

    private enum class MangaStatusFilter {
        ONGOING, COMPLETED, UNKNOWN
    }

    private fun parseQuery(query: String): SearchCriteria {
        val includeTerms = mutableListOf<String>()
        val excludeTerms = mutableListOf<String>()
        val exactPhrases = mutableListOf<String>()
        val tags = mutableListOf<String>()
        val excludeTags = mutableListOf<String>()
        var authorQuery: String? = null
        var statusFilter: MangaStatusFilter? = null

        // Regex to match quoted phrases
        val phraseRegex = """"([^"]+)"""".toRegex()
        val phrases = phraseRegex.findAll(query).map { it.groupValues[1] }.toList()
        exactPhrases.addAll(phrases)

        // Remove phrases from query for further processing
        var remainingQuery = query.replace(phraseRegex, " ")

        // Split by spaces and process tokens
        val tokens = remainingQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        tokens.forEach { token ->
            when {
                token.startsWith("-") -> {
                    val term = token.substring(1)
                    if (term.startsWith("tag:")) {
                        excludeTags.add(term.substring(4))
                    } else {
                        excludeTerms.add(term.lowercase())
                    }
                }
                token.startsWith("tag:") -> tags.add(token.substring(4).lowercase())
                token.startsWith("author:") -> authorQuery = token.substring(7)
                token.startsWith("status:") -> {
                    statusFilter = when (token.substring(7).lowercase()) {
                        "ongoing" -> MangaStatusFilter.ONGOING
                        "completed" -> MangaStatusFilter.COMPLETED
                        else -> MangaStatusFilter.UNKNOWN
                    }
                }
                else -> includeTerms.add(token.lowercase())
            }
        }

        return SearchCriteria(
            includeTerms = includeTerms,
            excludeTerms = excludeTerms,
            exactPhrases = exactPhrases,
            tags = tags,
            excludeTags = excludeTags,
            authorQuery = authorQuery,
            statusFilter = statusFilter
        )
    }

    private fun matchesCriteria(manga: Manga, criteria: SearchCriteria): Boolean {
        val searchableText = buildString {
            append(manga.title.lowercase())
            append(" ")
            append(manga.author?.lowercase() ?: "")
            append(" ")
            append(manga.artist?.lowercase() ?: "")
            append(" ")
            append(manga.description?.lowercase() ?: "")
            append(" ")
            append(manga.genre.joinToString(" ").lowercase())
        }

        // Check exclude terms first (fast fail)
        criteria.excludeTerms.forEach { term ->
            if (searchableText.contains(term)) return false
        }

        // Check exclude tags
        criteria.excludeTags.forEach { tag ->
            if (manga.genre.any { it.lowercase().contains(tag) }) return false
        }

        // Check required tags
        criteria.tags.forEach { tag ->
            if (!manga.genre.any { it.lowercase().contains(tag) }) return false
        }

        // Check author query
        criteria.authorQuery?.let { author ->
            val authorMatch = manga.author?.lowercase()?.contains(author.lowercase()) == true ||
                    manga.artist?.lowercase()?.contains(author.lowercase()) == true
            if (!authorMatch) return false
        }

        // Check status filter
        criteria.statusFilter?.let { status ->
            val matchesStatus = when (status) {
                MangaStatusFilter.ONGOING -> manga.status == app.otakureader.domain.model.MangaStatus.ONGOING
                MangaStatusFilter.COMPLETED -> manga.status == app.otakureader.domain.model.MangaStatus.COMPLETED
                MangaStatusFilter.UNKNOWN -> manga.status == app.otakureader.domain.model.MangaStatus.UNKNOWN
            }
            if (!matchesStatus) return false
        }

        // Check exact phrases
        criteria.exactPhrases.forEach { phrase ->
            if (!searchableText.contains(phrase.lowercase())) return false
        }

        // Check include terms
        criteria.includeTerms.forEach { term ->
            if (!searchableText.contains(term)) return false
        }

        return true
    }
}
