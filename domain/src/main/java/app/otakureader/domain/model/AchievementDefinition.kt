package app.otakureader.domain.model

enum class AchievementDefinition(val target: Int) {
    FIRST_CHAPTER(target = 1),
    READ_100_CHAPTERS(target = 100),
    READ_1000_CHAPTERS(target = 1000),
    COMPLETE_10_MANGA(target = 10),
    SEVEN_DAY_STREAK(target = 7),
    THIRTY_DAY_STREAK(target = 30)
}
