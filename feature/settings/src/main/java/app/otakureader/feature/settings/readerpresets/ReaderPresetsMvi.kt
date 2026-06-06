package app.otakureader.feature.settings.readerpresets

import app.otakureader.core.preferences.ReaderPreset

data class ReaderPresetsState(
    val presets: List<ReaderPreset> = emptyList(),
    val showSaveDialog: Boolean = false,
    val saveDialogName: String = "",
)

sealed interface ReaderPresetsEvent {
    data object ShowSaveDialog : ReaderPresetsEvent
    data object HideSaveDialog : ReaderPresetsEvent
    data class UpdateSaveName(val name: String) : ReaderPresetsEvent
    data object ConfirmSave : ReaderPresetsEvent
    data class Apply(val preset: ReaderPreset) : ReaderPresetsEvent
    data class Delete(val id: String) : ReaderPresetsEvent
}

sealed interface ReaderPresetsEffect {
    data class ShowSnackbar(val message: String) : ReaderPresetsEffect
}
