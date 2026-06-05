package app.otakureader.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementTest {

    @Test
    fun `isUnlocked returns true when unlockedAt is positive`() {
        val achievement = Achievement(
            id = 1L,
            definition = AchievementDefinition.FIRST_CHAPTER,
            unlockedAt = 1000L,
            progress = 1,
            target = 1,
        )
        assertTrue(achievement.isUnlocked)
    }

    @Test
    fun `isUnlocked returns false when unlockedAt is zero`() {
        val achievement = Achievement(
            id = 1L,
            definition = AchievementDefinition.READ_100_CHAPTERS,
            unlockedAt = 0L,
            progress = 42,
            target = 100,
        )
        assertFalse(achievement.isUnlocked)
    }

    @Test
    fun `isUnlocked returns false when unlockedAt is negative`() {
        val achievement = Achievement(
            id = 1L,
            definition = AchievementDefinition.SEVEN_DAY_STREAK,
            unlockedAt = -1L,
            progress = 0,
            target = 7,
        )
        assertFalse(achievement.isUnlocked)
    }
}
