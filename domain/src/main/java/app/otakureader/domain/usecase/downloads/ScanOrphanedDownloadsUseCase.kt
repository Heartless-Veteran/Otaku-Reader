package app.otakureader.domain.usecase.downloads

import app.otakureader.domain.model.OrphanScanResult
import app.otakureader.domain.repository.DownloadRepository
import javax.inject.Inject

class ScanOrphanedDownloadsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(): OrphanScanResult = downloadRepository.scanOrphanedDownloads()
}
