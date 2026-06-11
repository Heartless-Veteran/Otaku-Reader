package app.otakureader.domain.usecase.downloads

import app.otakureader.domain.model.OrphanScanResult
import app.otakureader.domain.repository.DownloadRepository
import javax.inject.Inject

class DeleteOrphanedDownloadsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(): OrphanScanResult = downloadRepository.deleteOrphanedDownloads()
}
