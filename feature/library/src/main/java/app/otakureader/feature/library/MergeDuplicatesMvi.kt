package app.otakureader.feature.library

import app.otakureader.domain.model.Manga

data class MergeDuplicatesState(
    val isLoading: Boolean = true,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val isMerging: Boolean = false,
    val error: String? = null,
)

/** A set of library manga entries sharing the same normalised title. */
data class DuplicateGroup(
    /** All manga in this group (same title, different sources or entries). */
    val entries: List<Manga>,
    /** The ID of the entry chosen to survive the merge (defaulting to the one with the most chapters). */
    val primaryId: Long = entries.maxByOrNull { it.unreadCount + it.totalChapters }?.id
        ?: entries.first().id,
)

sealed class MergeDuplicatesEvent {
    data object LoadDuplicates : MergeDuplicatesEvent()
    /** User picked a different primary for a group. */
    data class SelectPrimary(val groupIndex: Int, val primaryId: Long) : MergeDuplicatesEvent()
    /** Merge a single group: keep [primaryId], delete the rest. */
    data class MergeGroup(val group: DuplicateGroup) : MergeDuplicatesEvent()
    /** Merge every group with its current primary selection. */
    data object MergeAll : MergeDuplicatesEvent()
}

sealed class MergeDuplicatesEffect {
    data class ShowSnackbar(val message: String) : MergeDuplicatesEffect()
    data object NavigateBack : MergeDuplicatesEffect()
}
