package app.otakureader.data.backup.tachiyomi

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Protobuf schema for Mihon/Tachiyomi `.tachibk` / `.proto.gz` backups.
 *
 * Field numbers mirror Mihon's public backup models. Only the fields Otaku Reader consumes
 * are declared — kotlinx-serialization's ProtoBuf decoder skips unknown field numbers, so a
 * partial schema decodes real backups safely. All fields carry defaults so a missing field
 * never fails the decode.
 *
 * NOTE: tracking field numbers (especially the remote media id) have shifted across Tachiyomi
 * and Mihon versions; the importer guards tracking writes accordingly.
 */
@Serializable
internal data class MihonBackup(
    @ProtoNumber(1) val backupManga: List<MihonBackupManga> = emptyList(),
    @ProtoNumber(2) val backupCategories: List<MihonBackupCategory> = emptyList(),
)

@Serializable
internal data class MihonBackupManga(
    @ProtoNumber(1) val source: Long = 0,
    @ProtoNumber(2) val url: String = "",
    @ProtoNumber(3) val title: String = "",
    @ProtoNumber(4) val artist: String? = null,
    @ProtoNumber(5) val author: String? = null,
    @ProtoNumber(6) val description: String? = null,
    @ProtoNumber(7) val genre: List<String> = emptyList(),
    @ProtoNumber(8) val status: Int = 0,
    @ProtoNumber(9) val thumbnailUrl: String? = null,
    @ProtoNumber(13) val dateAdded: Long = 0,
    @ProtoNumber(15) val chapters: List<MihonBackupChapter> = emptyList(),
    // Category membership is stored as the list of category `order` values the manga belongs to.
    @ProtoNumber(16) val categories: List<Long> = emptyList(),
    @ProtoNumber(17) val tracking: List<MihonBackupTracking> = emptyList(),
    @ProtoNumber(18) val favorite: Boolean = true,
    @ProtoNumber(19) val chapterFlags: Int = 0,
    @ProtoNumber(21) val viewerFlags: Int = 0,
    @ProtoNumber(24) val lastModifiedAt: Long = 0,
)

@Serializable
internal data class MihonBackupChapter(
    @ProtoNumber(1) val url: String = "",
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val scanlator: String? = null,
    @ProtoNumber(4) val read: Boolean = false,
    @ProtoNumber(5) val bookmark: Boolean = false,
    @ProtoNumber(6) val lastPageRead: Long = 0,
    @ProtoNumber(7) val dateFetch: Long = 0,
    @ProtoNumber(8) val dateUpload: Long = 0,
    @ProtoNumber(9) val chapterNumber: Float = 0f,
    @ProtoNumber(10) val sourceOrder: Long = 0,
)

@Serializable
internal data class MihonBackupCategory(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val order: Long = 0,
    @ProtoNumber(100) val flags: Long = 0,
)

@Serializable
internal data class MihonBackupTracking(
    @ProtoNumber(1) val syncId: Int = 0,
    @ProtoNumber(2) val libraryId: Long = 0,
    @ProtoNumber(4) val trackingUrl: String = "",
    @ProtoNumber(5) val title: String = "",
    @ProtoNumber(6) val lastChapterRead: Float = 0f,
    @ProtoNumber(7) val totalChapters: Int = 0,
    @ProtoNumber(8) val score: Float = 0f,
    @ProtoNumber(9) val status: Int = 0,
    @ProtoNumber(10) val startedReadingDate: Long = 0,
    @ProtoNumber(11) val finishedReadingDate: Long = 0,
    @ProtoNumber(100) val mediaId: Long = 0,
) {
    /** Remote media id, preferring the newer field 100 over the legacy field 2. */
    val remoteId: Long get() = if (mediaId != 0L) mediaId else libraryId
}
