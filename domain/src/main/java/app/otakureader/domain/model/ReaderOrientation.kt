package app.otakureader.domain.model

/**
 * Screen-orientation lock applied while the reader is open, mirroring Mihon/Komikku's
 * orientation options.
 *
 * Pure-domain enum: it carries no Android types. The feature layer maps each entry to the
 * corresponding `ActivityInfo.SCREEN_ORIENTATION_*` value when applying it to the activity.
 * Persisted by [ordinal], so entries must only be appended, never reordered.
 */
enum class ReaderOrientation {
    /** Follow the system / user default (no explicit lock). */
    DEFAULT,

    /** Free rotation regardless of the system auto-rotate setting. */
    FREE,

    /** Sensor portrait (allows 180° flip). */
    PORTRAIT,

    /** Sensor landscape (allows left/right). */
    LANDSCAPE,

    /** Locked to a single portrait orientation. */
    LOCKED_PORTRAIT,

    /** Locked to a single landscape orientation. */
    LOCKED_LANDSCAPE,

    /** Reverse (upside-down) portrait. */
    REVERSE_PORTRAIT,
    ;

    companion object {
        /** Sentinel ordinal that never matches an entry, so a null/absent preference resolves to [DEFAULT]. */
        private const val INVALID_ORDINAL = -1

        /** Resolve a persisted ordinal back to an entry, falling back to [DEFAULT]. */
        fun fromOrdinal(ordinal: Int?): ReaderOrientation =
            entries.getOrNull(ordinal ?: INVALID_ORDINAL) ?: DEFAULT
    }
}
