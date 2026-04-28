package app.otakureader.core.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI pattern.
 *
 * @param S UI state type (immutable data class)
 * @param E UI event type (sealed interface of user actions)
 * @param F UI effect type (sealed interface of one-shot side effects)
 *
 * Usage:
 * ```
 * class LibraryViewModel @Inject constructor(
 *     private val getLibraryManga: GetLibraryMangaUseCase
 * ) : BaseMviViewModel<LibraryUiState, LibraryUiEvent, LibraryUiEffect>(
 *     initialState = LibraryUiState()
 * ) {
 *     override fun processEvent(event: LibraryUiEvent) {
 *         when (event) {
 *             is LibraryUiEvent.Refresh -> loadManga(force = event.force)
 *             is LibraryUiEvent.Search -> search(event.query)
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseMviViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect: Flow<F> = _effect.receiveAsFlow()

    /**
     * Dispatch an event to be processed by the ViewModel.
     * Thread-safe — can be called from any dispatcher.
     */
    fun onEvent(event: E) {
        processEvent(event)
    }

    /**
     * Override this to handle all [E] events.
     * Use [setState] to update UI state and [sendEffect] for one-shot effects.
     */
    protected abstract fun processEvent(event: E)

    /**
     * Update state atomically using the current state.
     * Safe to call from any coroutine context.
     */
    protected fun setState(transform: S.() -> S) {
        _state.update(transform)
    }

    /**
     * Send a one-shot effect to the UI layer.
     * Effects are consumed exactly once (e.g., navigation, snackbars, toasts).
     */
    protected fun sendEffect(effect: F) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    /**
     * Launch a coroutine tied to the ViewModel lifecycle.
     * Automatically cancelled when the ViewModel is cleared.
     */
    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
        }
    }

    /**
     * Current snapshot of the state. Use for synchronous reads only.
     * Prefer collecting [state] for reactive UI.
     */
    protected val currentState: S
        get() = _state.value
}
