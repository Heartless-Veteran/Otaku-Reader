package app.otakureader.domain.usecase.downloads

import app.otakureader.domain.model.ReindexResult
import app.otakureader.domain.repository.DownloadRepository
import javax.inject.Inject

class ReindexDownloadsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(): ReindexResult = downloadRepository.reindexDownloads()
}
