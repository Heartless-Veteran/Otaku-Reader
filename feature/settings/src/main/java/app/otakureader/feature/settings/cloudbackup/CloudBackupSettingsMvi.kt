package app.otakureader.feature.settings.cloudbackup

sealed interface CloudBackupDestination {
    data object None : CloudBackupDestination
    data object WebDav : CloudBackupDestination
}

data class CloudBackupSettingsState(
    val destination: CloudBackupDestination = CloudBackupDestination.None,
    val webDavUrl: String = "",
    val webDavUsername: String = "",
    val webDavPassword: String = "",
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null,
)

sealed interface CloudBackupSettingsEvent {
    data class SetDestination(val destination: CloudBackupDestination) : CloudBackupSettingsEvent
    data class SetWebDavUrl(val url: String) : CloudBackupSettingsEvent
    data class SetWebDavUsername(val username: String) : CloudBackupSettingsEvent
    data class SetWebDavPassword(val password: String) : CloudBackupSettingsEvent
    data object SaveCredentials : CloudBackupSettingsEvent
    data object TestConnection : CloudBackupSettingsEvent
    data object ClearCredentials : CloudBackupSettingsEvent
}

sealed interface CloudBackupSettingsEffect {
    data class ShowSnackbar(val message: String) : CloudBackupSettingsEffect
}
