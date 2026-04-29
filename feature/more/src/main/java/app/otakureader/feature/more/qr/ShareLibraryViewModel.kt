package app.otakureader.feature.more.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.model.ShareableManga
import app.otakureader.domain.repository.MangaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareLibraryViewModel @Inject constructor(
    private val mangaRepository: MangaRepository
) : ViewModel() {

    private val _library = MutableStateFlow<List<ShareableManga>>(emptyList())
    val library: StateFlow<List<ShareableManga>> = _library.asStateFlow()

    init {
        viewModelScope.launch {
            mangaRepository.getLibraryManga().collect { mangaList ->
                _library.value = mangaList.map { manga ->
                    ShareableManga(
                        title = manga.title,
                        url = manga.url,
                        thumbnailUrl = manga.thumbnailUrl,
                        status = manga.status?.name ?: MangaStatus.UNKNOWN.name
                    )
                }
            }
        }
    }
}
