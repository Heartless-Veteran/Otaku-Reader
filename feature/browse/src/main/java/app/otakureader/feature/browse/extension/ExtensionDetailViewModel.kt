package app.otakureader.feature.browse.extension

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.otakureader.core.extension.domain.model.Extension
import app.otakureader.core.extension.domain.repository.ExtensionRepository
import app.otakureader.core.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExtensionDetailState(
    val extension: Extension? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface ExtensionDetailEvent {
    data object ToggleTrust : ExtensionDetailEvent
    data object Retry : ExtensionDetailEvent
}

sealed interface ExtensionDetailEffect {
    data class ShowSnackbar(val message: String) : ExtensionDetailEffect
}

@HiltViewModel
class ExtensionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val extensionRepository: ExtensionRepository,
) : ViewModel() {

    private val route: Route.ExtensionDetail = savedStateHandle.toRoute()

    private val _state = MutableStateFlow(ExtensionDetailState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExtensionDetailState(),
    )

    private val _effect = Channel<ExtensionDetailEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadExtension()
    }

    private fun loadExtension() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val ext = extensionRepository.getExtension(route.packageName)
                if (ext != null) {
                    _state.update { it.copy(extension = ext, isLoading = false) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Extension not found") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load extension") }
            }
        }
    }

    fun onEvent(event: ExtensionDetailEvent) {
        when (event) {
            is ExtensionDetailEvent.ToggleTrust -> toggleTrust()
            is ExtensionDetailEvent.Retry -> loadExtension()
        }
    }

    private fun toggleTrust() {
        val ext = _state.value.extension ?: return
        viewModelScope.launch {
            try {
                val result = if (ext.isTrusted) {
                    extensionRepository.revokeExtensionTrust(ext.pkgName)
                } else {
                    extensionRepository.trustExtension(ext.pkgName)
                }
                result.fold(
                    onSuccess = {
                        loadExtension()
                        val msg = if (!ext.isTrusted) "Extension trusted" else "Extension trust revoked"
                        _effect.send(ExtensionDetailEffect.ShowSnackbar(msg))
                    },
                    onFailure = { e ->
                        _effect.send(ExtensionDetailEffect.ShowSnackbar(e.message ?: "Trust operation failed"))
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ExtensionDetailEffect.ShowSnackbar(e.message ?: "Trust operation failed"))
            }
        }
    }
}
