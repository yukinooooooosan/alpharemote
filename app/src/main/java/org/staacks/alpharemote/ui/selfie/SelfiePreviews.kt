package org.staacks.alpharemote.ui.selfie

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.staacks.alpharemote.selfie.SelfiePhase
import org.staacks.alpharemote.selfie.SelfieState

/** Immutable fixtures shared by IDE previews and headless rendering tests. */
object SelfiePreviewSamples {
    data class Sample(val name: String, val state: SelfieUiState)
    val samples = listOf(
        Sample("未接続", SelfieUiState()),
        Sample("接続済み・待機中", SelfieUiState(connected = true)),
        Sample("カウントダウン中", SelfieUiState(connected = true,
            session = SelfieState(phase = SelfiePhase.COUNTDOWN, remainingSeconds = 3, attempts = 1, shots = 1))),
        Sample("AF中", SelfieUiState(connected = true,
            session = SelfieState(phase = SelfiePhase.FOCUSING, attempts = 1, shots = 1))),
        Sample("撮影中", SelfieUiState(connected = true,
            session = SelfieState(phase = SelfiePhase.SHOOTING, attempts = 1, shots = 1))),
        Sample("AF失敗", SelfieUiState(connected = true,
            session = SelfieState(phase = SelfiePhase.ERROR, attempts = 1, shots = 1))),
        Sample("撮影終了", SelfieUiState(connected = true,
            session = SelfieState(phase = SelfiePhase.COMPLETED, attempts = 5, shots = 5))),
    )
}

class SelfiePreviewProvider : PreviewParameterProvider<SelfieUiState> {
    override val values: Sequence<SelfieUiState> get() = SelfiePreviewSamples.samples.asSequence().map { it.state }
    override fun getDisplayName(index: Int): String? = SelfiePreviewSamples.samples.getOrNull(index)?.name
}

@Preview(name = "通常", group = "SELFIE", widthDp = 393, heightDp = 800, apiLevel = 36,
    locale = "ja", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "ダーク", group = "SELFIE", widthDp = 393, heightDp = 800, apiLevel = 36,
    locale = "ja", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun SelfiePreview(@PreviewParameter(SelfiePreviewProvider::class) state: SelfieUiState) {
    SelfieTheme {
        // Deliberately no runtime adapter, ViewModel, service, BLE, storage or timer.
        // Clicking START/STOP or a setting in interactive preview is a no-op.
        SelfieScreen(state = state, onAction = {})
    }
}
