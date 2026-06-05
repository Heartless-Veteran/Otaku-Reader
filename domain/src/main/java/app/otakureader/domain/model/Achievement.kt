package app.otakureader.domain.model

data class Achievement(
    val id: Long,
    val definition: AchievementDefinition,
    val unlockedAt: Long,   // 0 = locked
    val progress: Int,
    val target: Int
) {
    val isUnlocked: Boolean get() = unlockedAt > 0
}
