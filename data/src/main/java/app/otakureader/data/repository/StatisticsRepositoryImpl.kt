package app.otakureader.data.repository

import app.otakureader.core.database.dao.ReadingHistoryDao
import app.otakureader.core.database.dao.ReadingStreakDao
import app.otakureader.core.database.entity.ReadingStreakEntity
import app.otakureader.domain.model.ReadingStats
import app.otakureader.domain.model.ReadingGoal
import app.otakureader.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val readingHistoryDao: ReadingHistoryDao,
    private val readingStreakDao: ReadingStreakDao,
) : StatisticsRepository {

    override fun getReadingStats(): Flow<ReadingStats> = flow {
        val history = readingHistoryDao.getAllHistory()
        val today = LocalDate.now()
        
        val readingDays = history
            .map { Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()
        
        val currentStreak = calculateCurrentStreak(readingDays, today)
        val bestStreak = calculateBestStreak(readingDays)
        val totalDaysRead = readingDays.size
        val totalChaptersRead = history.size
        val totalReadingTimeMs = history.sumOf { it.readDurationMs }
        
        val activityByDay = (0..89).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            history.count { 
                Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate() == date 
            }
        }.reversed()
        
        emit(ReadingStats(
            totalChaptersRead = totalChaptersRead,
            totalReadingTimeMs = totalReadingTimeMs,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalDaysRead = totalDaysRead,
            readingActivityByDay = activityByDay,
            averageChaptersPerDay = if (totalDaysRead > 0) totalChaptersRead.toFloat() / totalDaysRead else 0f,
            averageReadingTimePerChapterMs = if (totalChaptersRead > 0) totalReadingTimeMs / totalChaptersRead else 0L,
            longestSingleSessionMs = history.maxOfOrNull { it.readDurationMs } ?: 0L
        ))
    }
    
    override fun getReadingGoalProgress(dailyGoal: Int, weeklyGoal: Int): Flow<ReadingGoal> = flow {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        
        val history = readingHistoryDao.getAllHistory()
        val todayCount = history.count {
            Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate() == today
        }
        val todayMinutes = history.filter {
            Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate() == today
        }.sumOf { it.readDurationMs } / 60000
        
        val weekCount = history.count {
            val date = Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(weekStart) && !date.isAfter(today)
        }
        val weekMinutes = history.filter {
            val date = Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(weekStart) && !date.isAfter(today)
        }.sumOf { it.readDurationMs } / 60000
        
        val readingDays = history
            .map { Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()
        
        val currentStreak = calculateCurrentStreak(readingDays, today)
        val bestStreak = calculateBestStreak(readingDays)
        
        emit(ReadingGoal(
            targetChaptersPerDay = dailyGoal,
            targetMinutesPerDay = weeklyGoal / 7,
            currentStreak = currentStreak,
            longestStreak = bestStreak,
            totalChaptersThisWeek = weekCount,
            totalMinutesThisWeek = weekMinutes.toInt()
        ))
    }
    
    private fun calculateCurrentStreak(readingDays: List<LocalDate>, today: LocalDate): Int {
        if (readingDays.isEmpty()) return 0
        if (!readingDays.contains(today) && !readingDays.contains(today.minusDays(1))) return 0
        
        var streak = 0
        var checkDate = if (readingDays.contains(today)) today else today.minusDays(1)
        
        while (readingDays.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        
        return streak
    }
    
    private fun calculateBestStreak(readingDays: List<LocalDate>): Int {
        if (readingDays.isEmpty()) return 0
        
        var bestStreak = 1
        var currentStreak = 1
        
        for (i in 1 until readingDays.size) {
            val prev = readingDays[i - 1]
            val curr = readingDays[i]
            
            if (ChronoUnit.DAYS.between(prev, curr) == 1L) {
                currentStreak++
                bestStreak = maxOf(bestStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return bestStreak
    }
}
