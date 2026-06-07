package app.otakureader.feature.browse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.common.mvi.UiEffect
import app.otakureader.core.common.mvi.UiEvent
import app.otakureader.core.common.mvi.UiState
import app.otakureader.core.extension.domain.model.Extension
import app.otakureader.core.extension.domain.model.InstallStatus
import app.otakureader.core.extension.domain.repository.ExtensionRepoRepository
import app.otakureader.core.extension.domain.repository.ExtensionRepository
import app.otakureader.core.extension.installer.ExtensionInstaller
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.domain.repository.ExtensionManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

data class ExtensionsState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val installedExtensions: List<Extension> = emptyList(),
    val availableExtensions: List<Extension> = emptyList(),
    val extensionsWithUpdates: List<Extension> = emptyList(),
    val updateCount: Int = 0,
    val repositories: List<String> = emptyList(),
    val error: String? = null,
    val showNsfw: Boolean = false,
    val sortMode: SortMode = SortMode.NAME,
    val isUpdatingAll: Boolean = false,
    val selectedTab: Int = 0,
    val showUnverifiedInstallDialog: Boolean = false,
    val pendingUnverifiedExtension: Extension? = null,
    val hasUnverifiedExtensions: Boolean = false,
    /**
     * Package names of installed extensions whose current signer hash differs from the
     * first-seen hash recorded at install time. A non-empty set means the signing certificate
     * changed after the extension was first installed, which the UI should flag.
     */
    val signerMismatchedPackages: Set<String> = emptySet(),
) : UiState

enum class SortMode {
    NAME,
    RECENTLY_ADDED,
    LANGUAGE
}

sealed interface ExtensionsEvent : UiEvent {
    data object Refresh : ExtensionsEvent
    data class OnSearchQueryChange(val query: String) : ExtensionsEvent
    data class InstallExtension(val extension: Extension) : ExtensionsEvent
    data class UninstallExtension(val extension: Extension) : ExtensionsEvent
    data class UpdateExtension(val extension: Extension) : ExtensionsEvent
    data class ToggleExtensionEnabled(val extension: Extension, val enabled: Boolean) : ExtensionsEvent
    data class AddRepository(val url: String) : ExtensionsEvent
    data class RemoveRepository(val url: String) : ExtensionsEvent
    data object UpdateAllExtensions : ExtensionsEvent
    data class ToggleNsfw(val show: Boolean) : ExtensionsEvent
    data class SetSortMode(val mode: SortMode) : ExtensionsEvent
    data class SelectTab(val tab: Int) : ExtensionsEvent
    data object DismissUnverifiedDialog : ExtensionsEvent
    data object ConfirmUnverifiedInstall : ExtensionsEvent
}

sealed interface ExtensionsEffect : UiEffect {
    data class ShowSnackbar(val message: String) : ExtensionsEffect
    data class ShowError(val message: String) : ExtensionsEffect
}

@HiltViewModel
class ExtensionsViewModel @Inject constructor(
    private val extensionRepository: ExtensionRepository,
    private val extensionInstaller: ExtensionInstaller,
    private val extensionRepoRepository: ExtensionRepoRepository,
    private val extensionManagementRepository: ExtensionManagementRepository,
    private val generalPreferences: GeneralPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortMode = MutableStateFlow(SortMode.NAME)

    private val _state = MutableStateFlow(ExtensionsState())
    val state = combine(
        _state,
        _searchQuery,
        generalPreferences.showNsfwContent,
        _sortMode
    ) { state, query, showNsfw, sortMode ->
        state.copy(
            searchQuery = query,
            showNsfw = showNsfw,
            sortMode = sortMode,
            installedExtensions = applyFiltersAndSort(state.installedExtensions, query, showNsfw, sortMode),
            availableExtensions = applyFiltersAndSort(state.availableExtensions, query, showNsfw, sortMode),
            extensionsWithUpdates = applyFiltersAndSort(state.extensionsWithUpdates, query, showNsfw, sortMode)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExtensionsState(),
    )

    private fun applyFiltersAndSort(
        extensions: List<Extension>,
        query: String,
        showNsfw: Boolean,
        sortMode: SortMode
    ): List<Extension> {
        var result = extensions
        
        // Filter NSFW
        if (!showNsfw) {
            result = result.filter { !it.isNsfw }
        }
        
        // Filter by search query
        if (query.isNotBlank()) {
            result = result.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.sources.any { s -> s.name.contains(query, ignoreCase = true) }
            }
        }
        
        // Sort
        result = when (sortMode) {
            SortMode.NAME -> result.sortedBy { it.name }
            SortMode.RECENTLY_ADDED -> result.sortedWith(
                compareByDescending<Extension> { it.installDate }.thenBy { it.name }
            )
            SortMode.LANGUAGE -> result.sortedWith(
                compareBy<Extension> { it.lang }.thenBy { it.name }
            )
        }
        
        return result
    }

    private val _effect = Channel<ExtensionsEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            @Suppress("DEPRECATION")
            extensionRepoRepository.ensureDefaultRepository()
            loadExtensions()
            observeRepositories()
            refreshExtensions()
        }
    }

    fun onEvent(event: ExtensionsEvent) {
        when (event) {
            is ExtensionsEvent.Refresh -> refreshExtensions()
            is ExtensionsEvent.OnSearchQueryChange -> _searchQuery.value = event.query
            is ExtensionsEvent.InstallExtension -> installExtension(event.extension)
            is ExtensionsEvent.UninstallExtension -> uninstallExtension(event.extension)
            is ExtensionsEvent.UpdateExtension -> updateExtension(event.extension)
            is ExtensionsEvent.ToggleExtensionEnabled -> toggleExtension(event.extension, event.enabled)
            is ExtensionsEvent.AddRepository -> addRepository(event.url)
            is ExtensionsEvent.RemoveRepository -> removeRepository(event.url)
            is ExtensionsEvent.UpdateAllExtensions -> updateAllExtensions()
            is ExtensionsEvent.ToggleNsfw -> viewModelScope.launch {
                generalPreferences.setShowNsfwContent(event.show)
            }
            is ExtensionsEvent.SetSortMode -> _sortMode.value = event.mode
            is ExtensionsEvent.SelectTab -> _state.update { it.copy(selectedTab = event.tab) }
            is ExtensionsEvent.DismissUnverifiedDialog -> dismissUnverifiedDialog()
            is ExtensionsEvent.ConfirmUnverifiedInstall -> confirmUnverifiedInstall()
        }
    }

    private fun loadExtensions() {
        _state.update { it.copy(isLoading = true, error = null) }
        observeInstalledExtensions()
        observeAvailableExtensions()
        observeExtensionUpdates()
    }

    private fun observeInstalledExtensions() {
        viewModelScope.launch {
            try {
                extensionRepository.getInstalledExtensions()
                    .collect { extensions ->
                        val mismatches = checkSignerHashContinuity(extensions)
                        _state.update {
                            it.copy(
                                installedExtensions = extensions,
                                isLoading = false,
                                signerMismatchedPackages = mismatches,
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load extensions")
                }
            }
        }
    }

    private fun observeAvailableExtensions() {
        viewModelScope.launch {
            try {
                extensionRepository.getAvailableExtensions()
                    .collect { extensions ->
                        _state.update {
                            it.copy(
                                availableExtensions = extensions,
                                hasUnverifiedExtensions = extensions.any { it.signatureHash == null },
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
        }
    }

    private fun observeExtensionUpdates() {
        viewModelScope.launch {
            try {
                extensionRepository.getExtensionsWithUpdates()
                    .collect { extensions ->
                        _state.update {
                            it.copy(extensionsWithUpdates = extensions, updateCount = extensions.size)
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
        }
    }

    private fun observeRepositories() {
        viewModelScope.launch {
            extensionRepoRepository.getRepositories().collect { repos ->
                _state.update { it.copy(repositories = repos) }
            }
        }
        // All configured repositories are simultaneously active — no single active repo concept.
    }

    private fun refreshExtensions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val availableResult = extensionRepository.refreshAvailableExtensions()
                availableResult
                    .onSuccess {
                        extensionRepository.checkForUpdates()
                        _state.update { it.copy(isLoading = false) }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to refresh extensions: ${error.message ?: "Unknown error"}"
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to refresh extensions"
                    )
                }
            }
        }
    }

    private fun toggleExtension(extension: Extension, enabled: Boolean) {
        viewModelScope.launch {
            try {
                extensionRepository.setExtensionEnabled(extension.pkgName, enabled)
                extensionManagementRepository.refreshSources()
                    .onFailure { e -> _effect.send(ExtensionsEffect.ShowError("Failed to reload sources: ${e.message}")) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ExtensionsEffect.ShowError("Failed to update extension: ${e.message}"))
            }
        }
    }

    private fun addRepository(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            runCatching { extensionRepoRepository.addRepository(url.trim()) }
                .onFailure { _effect.send(ExtensionsEffect.ShowError("Invalid repository URL")) }
        }
    }

    private fun removeRepository(url: String) {
        viewModelScope.launch {
            // try/catch with explicit CancellationException rethrow instead of `runCatching`,
            // matching the pattern used by toggleExtension / doInstallExtension / uninstallExtension
            // in this VM. `runCatching` catches Throwable including CancellationException, which
            // would route normal coroutine cancellation through the error snackbar.
            try {
                extensionRepoRepository.removeRepository(url)
                _effect.send(ExtensionsEffect.ShowSnackbar("Repository removed"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ExtensionsEffect.ShowError("Couldn't remove repository: ${e.message}"))
            }
        }
    }

    private fun installExtension(extension: Extension) {
        if (extension.signatureHash == null) {
            _state.update {
                it.copy(
                    showUnverifiedInstallDialog = true,
                    pendingUnverifiedExtension = extension
                )
            }
            return
        }
        doInstallExtension(extension)
    }

    private fun doInstallExtension(extension: Extension) {
        viewModelScope.launch {
            // Show loading spinner immediately before the download starts
            _state.update { s ->
                s.copy(availableExtensions = s.availableExtensions.map {
                    if (it.pkgName == extension.pkgName) it.copy(status = InstallStatus.INSTALLING) else it
                })
            }
            try {
                val result = extensionInstaller.downloadAndInstall(extension)
                result.onSuccess {
                    // Refresh sources first so the source is available before the snackbar fires
                    extensionManagementRepository.refreshSources()
                        .onFailure { e -> _effect.send(ExtensionsEffect.ShowError("Failed to reload sources: ${e.message}")) }
                    _effect.send(ExtensionsEffect.ShowSnackbar("Extension installed: ${extension.name}"))
                }.onFailure { error ->
                    // Revert the optimistic status on failure
                    _state.update { s ->
                        s.copy(availableExtensions = s.availableExtensions.map {
                            if (it.pkgName == extension.pkgName) it.copy(status = InstallStatus.AVAILABLE) else it
                        })
                    }
                    _effect.send(ExtensionsEffect.ShowError("Failed to install: ${error.message}"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ExtensionsEffect.ShowError("Failed to install: ${e.message}"))
            }
        }
    }

    private fun confirmUnverifiedInstall() {
        val extension = _state.value.pendingUnverifiedExtension ?: return
        dismissUnverifiedDialog()
        doInstallExtension(extension)
    }

    private fun dismissUnverifiedDialog() {
        _state.update {
            it.copy(
                showUnverifiedInstallDialog = false,
                pendingUnverifiedExtension = null
            )
        }
    }

    private fun uninstallExtension(extension: Extension) {
        viewModelScope.launch {
            _state.update { s ->
                s.copy(installedExtensions = s.installedExtensions.map {
                    if (it.pkgName == extension.pkgName) it.copy(status = InstallStatus.UNINSTALLING) else it
                })
            }
            try {
                val result = extensionInstaller.uninstall(extension.pkgName)
                result.onSuccess {
                    extensionManagementRepository.refreshSources()
                        .onFailure { e -> _effect.send(ExtensionsEffect.ShowError("Failed to reload sources: ${e.message}")) }
                    _effect.send(ExtensionsEffect.ShowSnackbar("Extension uninstalled: ${extension.name}"))
                }.onFailure { error ->
                    _effect.send(ExtensionsEffect.ShowError("Failed to uninstall: ${error.message}"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ExtensionsEffect.ShowError("Failed to uninstall: ${e.message}"))
            }
        }
    }

    private fun updateExtension(extension: Extension) {
        viewModelScope.launch {
            _state.update { s ->
                val updatedStatus = { ext: Extension ->
                    if (ext.pkgName == extension.pkgName) ext.copy(status = InstallStatus.UPDATING) else ext
                }
                s.copy(
                    installedExtensions = s.installedExtensions.map(updatedStatus),
                    extensionsWithUpdates = s.extensionsWithUpdates.map(updatedStatus),
                )
            }
            try {
                val result = extensionInstaller.downloadAndInstall(extension)
                result.onSuccess {
                    extensionManagementRepository.refreshSources()
                        .onFailure { e -> _effect.send(ExtensionsEffect.ShowError("Failed to reload sources: ${e.message}")) }
                    _effect.send(ExtensionsEffect.ShowSnackbar("Extension updated: ${extension.name}"))
                }.onFailure { error ->
                    _effect.send(ExtensionsEffect.ShowError("Failed to update: ${error.message}"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ExtensionsEffect.ShowError("Failed to update: ${e.message}"))
            }
        }
    }

    private fun updateAllExtensions() {
        viewModelScope.launch {
            val updates = _state.value.extensionsWithUpdates
            if (updates.isEmpty()) return@launch

            _state.update { it.copy(isUpdatingAll = true) }
            var successCount = 0
            var failCount = 0

            updates.forEach { extension ->
                val result = runCatching { extensionInstaller.downloadAndInstall(extension) }
                    .getOrElse { Result.failure(it) }
                if (result.isSuccess) successCount++ else failCount++
            }

            _state.update { it.copy(isUpdatingAll = false) }
            extensionManagementRepository.refreshSources()
                .onFailure { e -> _effect.send(ExtensionsEffect.ShowError("Failed to reload sources: ${e.message}")) }

            when {
                failCount == 0 -> _effect.send(ExtensionsEffect.ShowSnackbar("All $successCount extensions updated"))
                successCount == 0 -> _effect.send(ExtensionsEffect.ShowError("All updates failed"))
                else -> _effect.send(ExtensionsEffect.ShowSnackbar("$successCount updated, $failCount failed"))
            }
        }
    }

    /**
     * For every installed extension that has a non-empty [Extension.signatureHash]:
     * - If no first-seen hash is stored yet, records the current hash (first install).
     * - If a hash was already stored and it differs from the current hash, logs a warning
     *   and adds the package name to the returned mismatch set so the UI can flag it.
     *
     * This implements the "signer hash continuity" check described in #1049:
     * a changed signing certificate after first install is a security-sensitive event.
     *
     * @return the set of package names whose current signer hash differs from first-seen.
     */
    private suspend fun checkSignerHashContinuity(extensions: List<Extension>): Set<String> {
        val firstSeenHashes = generalPreferences.extensionFirstSeenHashes.first()
        val mismatches = mutableSetOf<String>()

        extensions.forEach { extension ->
            val currentHash = extension.signatureHash
            if (currentHash.isNullOrEmpty()) return@forEach // unsigned extension — skip

            val knownHash = firstSeenHashes[extension.pkgName]
            when {
                knownHash == null -> {
                    // First time we see this extension — record its hash for future comparisons.
                    generalPreferences.recordExtensionFirstSeenHash(extension.pkgName, currentHash)
                }
                knownHash != currentHash -> {
                    // Hash changed since first install — this is worth flagging.
                    Log.w(
                        TAG,
                        "Signer hash changed for ${extension.pkgName}: " +
                            "first-seen=$knownHash current=$currentHash"
                    )
                    mismatches.add(extension.pkgName)
                }
                // else: hash matches first-seen — all good
            }
        }

        return mismatches
    }

    companion object {
        private const val TAG = "ExtensionsViewModel"
    }
}
