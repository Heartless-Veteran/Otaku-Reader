package app.otakureader.feature.browse

import app.otakureader.sourceapi.Filter
import app.otakureader.sourceapi.FilterList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serializes the user's active browse [FilterList] selections to a string and re-applies them
 * onto a freshly fetched filter list (filters are matched positionally, which is stable for a
 * given source/extension version).
 *
 * Only filter *state* is persisted — the structure always comes from the source's
 * `getFilterList()`, so this never modifies the Tachiyomi-compatible [Filter] contracts.
 */
internal object BrowseFilterStatePersistence {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private sealed interface Node {
        @Serializable @SerialName("none") data object None : Node
        @Serializable @SerialName("int") data class IntState(val value: Int) : Node
        @Serializable @SerialName("str") data class StrState(val value: String) : Node
        @Serializable @SerialName("bool") data class BoolState(val value: Boolean) : Node
        @Serializable @SerialName("sort") data class SortState(val index: Int?, val ascending: Boolean) : Node
        @Serializable @SerialName("group") data class GroupState(val children: List<Node>) : Node
    }

    /** Encodes filter states to a string, or null when nothing is worth persisting. */
    fun encode(filters: FilterList): String? {
        if (filters.filters.isEmpty()) return null
        val nodes = filters.filters.map { it.toNode() }
        return json.encodeToString(nodes)
    }

    /** Applies previously [encode]d states onto [filters] in place, matching by position. */
    fun apply(filters: FilterList, encoded: String) {
        val nodes = runCatching { json.decodeFromString<List<Node>>(encoded) }.getOrNull() ?: return
        applyNodes(filters.filters, nodes)
    }

    private fun applyNodes(filters: List<Filter<*>>, nodes: List<Node>) {
        filters.forEachIndexed { index, filter ->
            val node = nodes.getOrNull(index) ?: return@forEachIndexed
            filter.applyNode(node)
        }
    }

    private fun Filter<*>.toNode(): Node = when (this) {
        is Filter.Select<*> -> Node.IntState(state)
        is Filter.TriState -> Node.IntState(state)
        is Filter.Text -> Node.StrState(state)
        is Filter.CheckBox -> Node.BoolState(state)
        is Filter.Sort -> Node.SortState(state?.index, state?.ascending ?: true)
        is Filter.Group<*> -> Node.GroupState(state.filterIsInstance<Filter<*>>().map { it.toNode() })
        else -> Node.None
    }

    private fun Filter<*>.applyNode(node: Node) {
        when (this) {
            is Filter.Select<*> -> if (node is Node.IntState) state = node.value
            is Filter.TriState -> if (node is Node.IntState) state = node.value
            is Filter.Text -> if (node is Node.StrState) state = node.value
            is Filter.CheckBox -> if (node is Node.BoolState) state = node.value
            is Filter.Sort -> if (node is Node.SortState) {
                state = node.index?.let { Filter.Sort.Selection(it, node.ascending) }
            }
            is Filter.Group<*> -> if (node is Node.GroupState) {
                applyNodes(state.filterIsInstance<Filter<*>>(), node.children)
            }
            else -> Unit
        }
    }
}
