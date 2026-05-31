package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val definitionKey: String,
    val unlockedAt: Long = 0L,
    val progress: Int = 0,
    val target: Int = 0
)
