package app.otakureader.feature.settings.delegate

import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.data.tracking.TrackManager
import app.otakureader.data.updater.AppUpdateChecker
import app.otakureader.feature.settings.SettingsEffect
import app.otakureader.feature.settings.SettingsEvent
import app.otakureader.feature.settings.SettingsState
import app.otakureader.feature.settings.TrackerInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerSyncSettingsDelegate @Inject constructor(
    private val trackManager: TrackManager,
    private val appUpdateChecker: AppUpdateChecker,
    private val generalPreferences: GeneralPreferences,
) {

    private var updateState: ((SettingsState) -> SettingsState) -> Unit = {}

    fun startObserving(
        scope: CoroutineScope,
        updateState: ((SettingsState) -> SettingsState) -> Unit,
    ) {
        this.updateState = updateState
        refreshTrackers()

        // App update preferences
        scope.launch {
            combine(
                generalPreferences.appUpdateCheckEnabled,
                generalPreferences.lastAppUpdateCheck,
            ) { enabled, lastCheck ->
                updateState { current ->
                    current.copy(
                        appUpdateCheckEnabled = enabled,
                        lastAppUpdateCheck = lastCheck,
                    )
                }
            }.collect { }
        }
    }

    fun refreshTrackers() {
        updateState { it.copy(tracking = it.tracking.copy(trackers = trackManager.all.map { t -> TrackerInfo(t.id, t.name, t.isLoggedIn) })) }
    }

    suspend fun handleEvent(
        event: SettingsEvent,
        sendEffect: suspend (SettingsEffect) -> Unit,
    ): Boolean = when (event) {
        is SettingsEvent.LoginTracker -> { loginTracker(event.trackerId, event.username, event.password, sendEffect); true }
        is SettingsEvent.LogoutTracker -> { logoutTracker(event.trackerId, sendEffect); true }
        is SettingsEvent.SetAppUpdateCheckEnabled -> { generalPreferences.setAppUpdateCheckEnabled(event.enabled); true }
        SettingsEvent.CheckForAppUpdate -> { handleCheckForAppUpdate(sendEffect); true }
        else -> false
    }

    private suspend fun loginTracker(
        trackerId: Int,
        username: String,
        password: String,
        sendEffect: suspend (SettingsEffect) -> Unit,
    ) {
        updateState { it.copy(tracking = it.tracking.copy(trackingLoginInProgress = true)) }
        try {
            val tracker = trackManager.get(trackerId)
            if (tracker != null) {
                val success = tracker.login(username, password)
                if (success) {
                    refreshTrackers()
                    sendEffect(SettingsEffect.ShowSnackbar("Logged in to ${tracker.name}"))
                } else {
                    sendEffect(SettingsEffect.ShowSnackbar("Failed to login to ${tracker.name}"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sendEffect(SettingsEffect.ShowSnackbar("Error: ${e.message}"))
        } finally {
            updateState { it.copy(tracking = it.tracking.copy(trackingLoginInProgress = false)) }
        }
    }

    private suspend fun logoutTracker(trackerId: Int, sendEffect: suspend (SettingsEffect) -> Unit) {
        val tracker = trackManager.get(trackerId)
        tracker?.logout()
        refreshTrackers()
        sendEffect(SettingsEffect.ShowSnackbar("Logged out from ${tracker?.name}"))
    }

    private suspend fun handleCheckForAppUpdate(sendEffect: suspend (SettingsEffect) -> Unit) {
        val versionInfo = appUpdateChecker.checkForUpdate()
        if (versionInfo != null) {
            sendEffect(SettingsEffect.ShowSnackbar("Update available: ${versionInfo.versionName}"))
        } else {
            sendEffect(SettingsEffect.ShowSnackbar("App is up to date"))
        }
    }
}
