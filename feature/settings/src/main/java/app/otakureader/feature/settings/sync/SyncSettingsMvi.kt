package app.otakureader.feature.settings.sync

/**
 * MVI state for the Reader Sync settings screen.
 *
 * All fields are immutable; use [copy] in the ViewModel reducer to produce
 * updated state.
 */
data class SyncSettingsState(
    val serverUrl: String = "",
    val bearerToken: String = "",
    val deviceId: String = "",
    val queueSize: Int = 0,
    val isSyncing: Boolean = false,
    val lastSyncResult: String? = null,
)

/** User actions dispatched to [SyncSettingsViewModel]. */
sealed interface SyncSettingsEvent {
    data class SetServerUrl(val url: String) : SyncSettingsEvent
    data class SetBearerToken(val token: String) : SyncSettingsEvent
    data object SaveSettings : SyncSettingsEvent
    data object SyncNow : SyncSettingsEvent
    data object TestConnection : SyncSettingsEvent
}

/** One-shot side-effects emitted by [SyncSettingsViewModel]. */
sealed interface SyncSettingsEffect {
    data class ShowSnackbar(val message: String) : SyncSettingsEffect
}
