package app.otakureader.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "data_usage",
    primaryKeys = ["date", "category", "network"]
)
data class DataUsageEntity(
    val date: String,        // ISO date string yyyy-MM-dd
    val category: String,    // RequestCategory name
    val network: String,     // "WIFI" or "MOBILE"
    val bytes: Long = 0L
)
