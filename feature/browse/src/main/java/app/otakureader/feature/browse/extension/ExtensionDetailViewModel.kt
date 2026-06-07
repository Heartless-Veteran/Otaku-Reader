package app.otakureader.feature.browse.extension

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.otakureader.core.extension.domain.model.Extension
import app.otakureader.core.extension.domain.repository.ExtensionRepository
import app.otakureader.core.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the Extension Detail screen.
 *
 * @property extension The extension being viewed, or null while loading.
 * @property isLoading True while the initial load is in progress.
 * @property error Non-null when the load failed.
 */
data class ExtensionDetailState(
    val extension: Extension? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** User-triggered events for [ExtensionDetailViewModel]. */
sealed interface ExtensionDetailEvent {
    /** Toggle the trust status of the current extension. */
    data object ToggleTrust : ExtensionDetailEvent
}

/** One-shot side-effects emitted by [ExtensionDetailViewModel]. */
sealed interface ExtensionDetailEffect {
    data class ShowSnackbar(val message: String) : ExtensionDetailEffect
}

/**
 * ViewModel for [ExtensionDetailScreen].
 *
 * Loads a single [Extension] by package name (taken from the nav back-stack via
 * [SavedStateHandle]) and exposes it as a [StateFlow].  The trust toggle sets the
 * extension enabled/disabled — a proxy for the trust concept since the repository
 * models trust as the presence of a [Extension.signatureHash].
 */
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

    /** Reload the extension from the repository by its package name. */
    private fun loadExtension() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val ext = extensionRepository.getExtension(route.packageName)
            if (ext != null) {
                _state.update { it.copy(extension = ext, isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false, error = "Extension not found") }
            }
        }
    }

    fun onEvent(event: ExtensionDetailEvent) {
        when (event) {
            is ExtensionDetailEvent.ToggleTrust -> toggleTrust()
        }
    }

    /**
     * Toggles the enabled flag on the extension.
     *
     * The domain model uses [Extension.signatureHash] to express trust; in the UI we
     * let users mark a non-system extension as trusted by enabling it.  The repository
     * already exposes [ExtensionRepository.setExtensionEnabled] for this purpose.
     */
    private fun toggleTrust() {
        val ext = _state.value.extension ?: return
        viewModelScope.launch {
            val newEnabled = !ext.isEnabled
            extensionRepository.setExtensionEnabled(ext.pkgName, newEnabled)
            // Reload so the UI reflects the persisted change.
            loadExtension()
            val msg = if (newEnabled) "Extension trusted" else "Extension untrusted"
            _effect.send(ExtensionDetailEffect.ShowSnackbar(msg))
        }
    }
}
