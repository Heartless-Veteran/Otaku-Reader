package app.otakureader.feature.library

import app.otakureader.domain.model.Manga

data class MergeDuplicatesState(
    val isLoading: Boolean = true,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val isMerging: Boolean = false,
    val error: String? = null,
    /**
     * Maps [Manga.sourceId] to a human-readable source name.
     * Populated from the extension registry so duplicate group cards can show
     * "MangaDex" instead of a raw source ID.
     */
    val sourceNames: Map<Long, String> = emptyMap(),
    /** Maps each manga ID to the set of IDs it is currently linked with as alternatives. */
    val linkedAlternatives: Map<Long, Set<Long>> = emptyMap(),
)

/** A set of library manga entries sharing the same normalised title. */
data class DuplicateGroup(
    /** All manga in this group (same title, different sources or entries). */
    val entries: List<Manga>,
    /** The ID of the entry chosen to survive the merge (defaulting to the one with the most chapters). */
    val primaryId: Long = entries.maxByOrNull { it.unreadCount + it.totalChapters }?.id
        ?: entries.first().id,
) {
    /** True when entries in this group come from more than one source. */
    val isCrossSource: Boolean get() = entries.map { it.sourceId }.distinct().size > 1
}

sealed class MergeDuplicatesEvent {
    data object LoadDuplicates : MergeDuplicatesEvent()
    /** User picked a different primary for a group. */
    data class SelectPrimary(val groupIndex: Int, val primaryId: Long) : MergeDuplicatesEvent()
    /** Merge a single group: keep [primaryId], delete the rest. */
    data class MergeGroup(val group: DuplicateGroup) : MergeDuplicatesEvent()
    /** Merge every group with its current primary selection. */
    data object MergeAll : MergeDuplicatesEvent()
    /** Persist a bidirectional alternative-source link between two manga. */
    data class LinkAsAlternative(val primaryId: Long, val altId: Long) : MergeDuplicatesEvent()
    /** Copy missing chapter numbers from [altId] into [primaryId]. */
    data class FillMissingChapters(val primaryId: Long, val altId: Long) : MergeDuplicatesEvent()
    /** Remove the alternative-source link between two manga. */
    data class UnlinkAlternative(val primaryId: Long, val altId: Long) : MergeDuplicatesEvent()
}

sealed class MergeDuplicatesEffect {
    data class ShowSnackbar(val message: String) : MergeDuplicatesEffect()
    data object NavigateBack : MergeDuplicatesEffect()
}
