@file:Suppress("MaxLineLength")
package app.otakureader.data.repository

import app.otakureader.core.database.dao.MangaDao
import app.otakureader.core.database.dao.ReadingHistoryDao
import app.otakureader.domain.model.ReadingStats
import app.otakureader.domain.model.ReadingGoal
import app.otakureader.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val mangaDao: MangaDao,
) : StatisticsRepository {

    override fun getReadingStats(): Flow<ReadingStats> = combine(
        readingHistoryDao.observeHistory(),
        mangaDao.countFavorites(),
        mangaDao.getFavoriteMangaGenres(),
    ) { history, libraryCount, favoriteGenreValues ->
        val today = LocalDate.now()
        
        val readingDays = history
            .map { Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()
        
        val currentStreak = calculateCurrentStreak(readingDays, today)
        val bestStreak = calculateBestStreak(readingDays)
        val totalChaptersRead = history.size
        val totalReadingTimeMs = history.sumOf { it.readDurationMs }
        
        val activityByDay = (0..29).associate { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val count = history.count { 
                Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate() == date 
            }
            date.toString() to count
        }
        
        ReadingStats(
            totalMangaInLibrary = libraryCount,
            totalChaptersRead = totalChaptersRead,
            totalReadingTimeMs = totalReadingTimeMs,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            genreDistribution = buildGenreDistribution(favoriteGenreValues),
            readingActivityByDay = activityByDay
        )
    }
    
    @Suppress("MaxLineLength")
    override fun getReadingGoalProgress(dailyGoal: Int, weeklyGoal: Int): Flow<ReadingGoal> = readingHistoryDao.observeHistory().map { history ->
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        
        val todayCount = history.count {
            Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate() == today
        }
        val weekCount = history.count {
            val date = Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(weekStart) && !date.isAfter(today)
        }
        
        val readingDays = history
            .map { Instant.ofEpochMilli(it.readAt).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()
        
        val currentStreak = calculateCurrentStreak(readingDays, today)
        val bestStreak = calculateBestStreak(readingDays)
        
        ReadingGoal(
            dailyGoal = dailyGoal,
            dailyProgress = todayCount,
            weeklyGoal = weeklyGoal,
            weeklyProgress = weekCount
        )
    }
    
    private fun buildGenreDistribution(rawGenreValues: List<String>): Map<String, Int> = rawGenreValues
        .asSequence()
        .flatMap { rawGenreValue ->
            rawGenreValue
                .split(',', ';', '|')
                .asSequence()
        }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
        .associate { it.key to it.value }

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
