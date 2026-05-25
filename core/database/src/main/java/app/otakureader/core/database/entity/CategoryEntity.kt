package app.otakureader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val order: Int = 0,
    val flags: Int = 0,
    @ColumnInfo(name = "update_frequency")
    val updateFrequency: Int = 1,
    @ColumnInfo(name = "lock_type")
    val lockType: String? = null,
) {
    companion object {
        const val FLAG_HIDDEN = 1 shl 0
        const val FLAG_NSFW = 1 shl 1
        const val FLAG_LOCKED = 1 shl 2

        fun isHidden(flags: Int): Boolean = flags and FLAG_HIDDEN != 0
        fun isNsfw(flags: Int): Boolean = flags and FLAG_NSFW != 0
        fun isLocked(flags: Int): Boolean = flags and FLAG_LOCKED != 0
    }

    val isHidden: Boolean get() = flags and FLAG_HIDDEN != 0
    val isNsfw: Boolean get() = flags and FLAG_NSFW != 0
    val isLocked: Boolean get() = flags and FLAG_LOCKED != 0
}
