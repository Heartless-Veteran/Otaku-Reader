package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opds_servers",
    indices = [
        Index(value = ["url"], unique = true)
    ]
)
data class OpdsServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val username: String = "",
    val password: String = ""
)
