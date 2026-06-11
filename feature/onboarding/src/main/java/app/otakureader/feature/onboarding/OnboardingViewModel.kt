package app.otakureader.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.GeneralPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the onboarding wizard's Appearance step.
 *
 * Exposes the persisted theme mode reactively so the selection survives process death
 * and the app re-themes live as the user taps an option.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val generalPreferences: GeneralPreferences,
) : ViewModel() {

    /** Theme mode: 0 = system default, 1 = light, 2 = dark. */
    val themeMode: StateFlow<Int> = generalPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { generalPreferences.setThemeMode(mode) }
    }
}
