package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * FTS4 virtual table for fast full-text library search (#997/Phase5).
 *
 * Uses a content table (`manga`) so the data is not duplicated — the virtual
 * table holds only the FTS index. Sync triggers (in MIGRATION_33_34) keep the
 * index up-to-date whenever rows are inserted, updated, or deleted in `manga`.
 */
@Fts4(
    contentEntity = MangaEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
)
@Entity(tableName = "manga_fts")
data class MangaFtsEntity(
    val title: String,
    val author: String?,
    val artist: String?,
)
