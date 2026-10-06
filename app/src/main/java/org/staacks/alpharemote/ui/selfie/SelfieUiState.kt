package org.staacks.alpharemote.ui.selfie

import org.staacks.alpharemote.selfie.SelfieSettings
import org.staacks.alpharemote.selfie.SelfieState

/** Plain input to the screen. No connection, storage or service access. */
data class SelfieUiState(
    val settings: SelfieSettings = SelfieSettings(),
    val settingsLoaded: Boolean = true,
    val connected: Boolean = false,
    val session: SelfieState = SelfieState(),
)

sealed interface SelfieUiAction {
    data object StartStop : SelfieUiAction
    data object OpenConnection : SelfieUiAction
    data class ChangeSettings(val settings: SelfieSettings) : SelfieUiAction
}
