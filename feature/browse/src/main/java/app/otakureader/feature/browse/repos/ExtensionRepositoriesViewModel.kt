package app.otakureader.feature.browse.repos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.extension.domain.repository.ExtensionRepoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

@HiltViewModel
class ExtensionRepositoriesViewModel @Inject constructor(
    private val extensionRepoRepository: ExtensionRepoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExtensionRepositoriesState())
    val state: StateFlow<ExtensionRepositoriesState> = _state.asStateFlow()

    private val _effect = Channel<ExtensionRepositoriesEffect>(Channel.BUFFERED)
    val effect: Flow<ExtensionRepositoriesEffect> = _effect.receiveAsFlow()

    init {
        extensionRepoRepository.getRepositories()
            .onEach { urls ->
                _state.update {
                    it.copy(
                        repositories = urls.map { url ->
                            RepositoryItem(url = url, isDefault = url == ExtensionRepoRepository.DEFAULT_REPO_URL)
                        },
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ExtensionRepositoriesEvent) {
        when (event) {
            is ExtensionRepositoriesEvent.AddRepository -> addRepository(event.url)
            is ExtensionRepositoriesEvent.RemoveRepository -> removeRepository(event.url)
            is ExtensionRepositoriesEvent.ValidateUrl -> {
                _state.update { it.copy(urlValidationError = validate(event.url)) }
            }
        }
    }

    private fun addRepository(url: String) {
        val trimmed = url.trim()
        val err = validate(trimmed, requireNonEmpty = true)
        if (err != null) {
            _state.update { it.copy(urlValidationError = err) }
            return
        }
        viewModelScope.launch {
            try {
                extensionRepoRepository.addRepository(trimmed)
                _state.update { it.copy(urlValidationError = null) }
                _effect.send(ExtensionRepositoriesEffect.ShowSnackbar("Repository added"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ExtensionRepositoriesEffect.ShowSnackbar("Failed to add: ${e.message}"))
            }
        }
    }

    private fun removeRepository(url: String) {
        viewModelScope.launch {
            try {
                extensionRepoRepository.removeRepository(url)
                _effect.send(ExtensionRepositoriesEffect.ShowSnackbar("Repository removed"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ExtensionRepositoriesEffect.ShowSnackbar("Failed to remove: ${e.message}"))
            }
        }
    }

    /**
     * Validate the candidate URL. Returns an error string or null when acceptable.
     * Empty input returns null unless [requireNonEmpty] is set (so live validation while typing
     * doesn't shout at the user before they've finished).
     */
    private fun validate(url: String, requireNonEmpty: Boolean = false): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return if (requireNonEmpty) "URL cannot be empty" else null
        val parsed = trimmed.toHttpUrlOrNull()
            ?: return "Not a valid http(s) URL"
        if (parsed.scheme != "https" && parsed.scheme != "http") {
            return "URL must start with http:// or https://"
        }
        // Repos point at a directory; the loader appends index.min.json/index.json itself.
        // We just check the URL doesn't itself end with one of those filenames.
        val path = parsed.encodedPath
        if (path.endsWith("/index.json") || path.endsWith("/index.min.json")) {
            return "Drop the trailing /index.json — point to the directory"
        }
        return null
    }
}
