package app.otakureader.domain.model

enum class CategoryUpdateFrequency(val value: Int) {
    NEVER(0),
    DAILY(1),
    EVERY_3_DAYS(2),
    WEEKLY(3);

    companion object {
        fun fromInt(value: Int): CategoryUpdateFrequency =
            entries.find { it.value == value } ?: DAILY
    }
}
