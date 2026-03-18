package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val order: Int = 0,
    val flags: Int = 0
) {
    companion object {
        // Bit flags for category options
        const val FLAG_HIDDEN = 1 shl 0  // Category is hidden from main library view
        const val FLAG_NSFW = 1 shl 1    // Category contains NSFW content

        fun isHidden(flags: Int): Boolean = flags and FLAG_HIDDEN != 0
        fun isNsfw(flags: Int): Boolean = flags and FLAG_NSFW != 0
    }

    val isHidden: Boolean get() = flags and FLAG_HIDDEN != 0
    val isNsfw: Boolean get() = flags and FLAG_NSFW != 0
}
