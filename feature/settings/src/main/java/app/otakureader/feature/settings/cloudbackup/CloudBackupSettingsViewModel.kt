package app.otakureader.feature.settings.cloudbackup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.BackupPreferences
import app.otakureader.core.preferences.CloudBackupCredentialsStore
import app.otakureader.core.preferences.CloudBackupUploader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudBackupSettingsViewModel @Inject constructor(
    private val backupPreferences: BackupPreferences,
    private val credentialsStore: CloudBackupCredentialsStore,
    private val cloudBackupUploader: CloudBackupUploader,
) : ViewModel() {

    private val _state = MutableStateFlow(CloudBackupSettingsState())
    val state: StateFlow<CloudBackupSettingsState> = _state.asStateFlow()

    private val _effect = Channel<CloudBackupSettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            val destination = backupPreferences.cloudDestination.first()
            val creds = credentialsStore.getWebDavCredentials()
            _state.update {
                it.copy(
                    destination = if (destination == "WEBDAV") CloudBackupDestination.WebDav else CloudBackupDestination.None,
                    webDavUrl = creds?.url ?: "",
                    webDavUsername = creds?.username ?: "",
                    // Password is not loaded back — user must re-enter to change
                )
            }
        }
    }

    fun onEvent(event: CloudBackupSettingsEvent) {
        when (event) {
            is CloudBackupSettingsEvent.SetDestination -> {
                _state.update { it.copy(destination = event.destination, connectionTestResult = null) }
                viewModelScope.launch {
                    val key = when (event.destination) {
                        is CloudBackupDestination.WebDav -> "WEBDAV"
                        else -> "NONE"
                    }
                    backupPreferences.setCloudDestination(key)
                }
            }
            is CloudBackupSettingsEvent.SetWebDavUrl ->
                _state.update { it.copy(webDavUrl = event.url) }
            is CloudBackupSettingsEvent.SetWebDavUsername ->
                _state.update { it.copy(webDavUsername = event.username) }
            is CloudBackupSettingsEvent.SetWebDavPassword ->
                _state.update { it.copy(webDavPassword = event.password) }
            is CloudBackupSettingsEvent.SaveCredentials -> saveCredentials()
            is CloudBackupSettingsEvent.TestConnection -> testConnection()
            is CloudBackupSettingsEvent.ClearCredentials -> {
                credentialsStore.clearWebDavCredentials()
                _state.update {
                    it.copy(webDavUrl = "", webDavUsername = "", webDavPassword = "", connectionTestResult = null)
                }
            }
        }
    }

    private fun saveCredentials() {
        val s = _state.value
        credentialsStore.saveWebDavCredentials(
            url = s.webDavUrl.trim(),
            username = s.webDavUsername.trim(),
            password = s.webDavPassword,
        )
        viewModelScope.launch {
            _effect.send(CloudBackupSettingsEffect.ShowSnackbar("Credentials saved"))
        }
    }

    private fun testConnection() {
        val s = _state.value
        _state.update { it.copy(isTestingConnection = true, connectionTestResult = null) }
        cloudBackupUploader.configure(s.webDavUrl.trim(), s.webDavUsername.trim(), s.webDavPassword)
        viewModelScope.launch {
            val result = cloudBackupUploader.testConnection()
            _state.update {
                it.copy(
                    isTestingConnection = false,
                    connectionTestResult = if (result.isSuccess) {
                        "Connected successfully"
                    } else {
                        result.exceptionOrNull()?.message ?: "Connection failed"
                    },
                )
            }
        }
    }
}
