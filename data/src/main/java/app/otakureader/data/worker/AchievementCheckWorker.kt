package app.otakureader.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.otakureader.domain.model.AchievementDefinition
import app.otakureader.domain.repository.AchievementRepository
import app.otakureader.domain.repository.StatisticsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AchievementCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val statisticsRepository: StatisticsRepository,
    private val achievementRepository: AchievementRepository,
    private val goalCompletionNotifier: GoalCompletionNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        achievementRepository.initializeDefaults()
        val stats = statisticsRepository.getReadingStats().first()
        val all = achievementRepository.observeAll().first()

        val newlyUnlocked = mutableListOf<AchievementDefinition>()

        for (achievement in all) {
            if (achievement.isUnlocked) continue
            val def = achievement.definition
            val progress = when (def) {
                AchievementDefinition.FIRST_CHAPTER,
                AchievementDefinition.READ_100_CHAPTERS,
                AchievementDefinition.READ_1000_CHAPTERS -> stats.totalChaptersRead
                AchievementDefinition.COMPLETE_10_MANGA -> stats.totalMangaInLibrary
                AchievementDefinition.SEVEN_DAY_STREAK,
                AchievementDefinition.THIRTY_DAY_STREAK -> stats.currentStreak
            }
            achievementRepository.updateProgress(def, progress)
            if (progress >= def.target) {
                achievementRepository.unlock(def)
                newlyUnlocked.add(def)
            }
        }

        if (newlyUnlocked.isNotEmpty()) {
            goalCompletionNotifier.notifyAchievementUnlocked(newlyUnlocked.first())
        }

        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<AchievementCheckWorker>().build())
        }
    }
}
