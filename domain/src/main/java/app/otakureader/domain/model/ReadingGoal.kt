package app.otakureader.domain.model

import androidx.compose.runtime.Immutable

/** Progress toward daily and weekly reading goals, including current streak. */
@Immutable
data class ReadingGoal(
    val dailyGoal: Int = 0,
    val dailyProgress: Int = 0,
    val weeklyGoal: Int = 0,
    val weeklyProgress: Int = 0,
    val currentStreak: Int = 0,
)
