package app.otakureader.data.tracking.repository

import app.otakureader.core.database.entity.TrackEntryEntity
import app.otakureader.domain.model.TrackEntry
import app.otakureader.domain.model.TrackStatus

internal fun TrackEntryEntity.toDomain() = TrackEntry(
    remoteId = remoteId,
    mangaId = mangaId,
    trackerId = trackerId,
    title = title,
    remoteUrl = remoteUrl,
    status = TrackStatus.fromOrdinal(status),
    lastChapterRead = lastChapterRead,
    totalChapters = totalChapters,
    score = score,
    startDate = startDate,
    finishDate = finishDate,
)

internal fun TrackEntry.toEntity() = TrackEntryEntity(
    mangaId = mangaId,
    trackerId = trackerId,
    remoteId = remoteId,
    remoteUrl = remoteUrl,
    title = title,
    status = status.ordinal,
    lastChapterRead = lastChapterRead,
    totalChapters = totalChapters,
    score = score,
    startDate = startDate,
    finishDate = finishDate,
)
