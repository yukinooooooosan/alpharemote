package org.staacks.alpharemote.ui.selfie

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.staacks.alpharemote.selfie.SelfiePhase
import org.staacks.alpharemote.selfie.SelfieSettings
import org.staacks.alpharemote.selfie.SelfieShotResult
import org.staacks.alpharemote.selfie.SelfieShotResult.SHOT
import org.staacks.alpharemote.selfie.SelfieShotResult.SKIPPED
import org.staacks.alpharemote.selfie.SelfieShowState
import org.staacks.alpharemote.selfie.SelfieState
import org.staacks.alpharemote.ui.appearance.AppSkin

/** Immutable kawaii fixtures for IDE previews and headless tests. No service, BLE, voice or timers. */
object KawaiiPreviewSamples {
    data class Sample(val name: String, val state: SelfieUiState, val show: SelfieShowState)

    private val kawaii = SelfieSettings.KAWAII
    private fun session(phase: SelfiePhase, remaining: Int = 0, attempts: Int = 0, shots: Int = 0, skipped: Int = 0, disconnected: Boolean = false) =
        SelfieState(phase = phase, remainingSeconds = remaining, attempts = attempts, shots = shots, skipped = skipped,
            targetCycles = kawaii.cycles, disconnected = disconnected)
    private fun show(pose: Int, praise: Int, results: List<SelfieShotResult>) =
        SelfieShowState(active = true, poseId = pose, praiseId = praise, results = results)

    val samples = listOf(
        Sample("未接続", SelfieUiState(), SelfieShowState()),
        Sample("待機中", SelfieUiState(connected = true), SelfieShowState()),
        Sample("1回目カウント", SelfieUiState(connected = true, session = session(SelfiePhase.COUNTDOWN, remaining = 6)),
            show(pose = 3, praise = 0, results = emptyList())),
        Sample("2回目カウント", SelfieUiState(connected = true, session = session(SelfiePhase.COUNTDOWN, remaining = 3, attempts = 1, shots = 1)),
            show(pose = 11, praise = 1, results = listOf(SHOT))),
        Sample("AF中", SelfieUiState(connected = true, session = session(SelfiePhase.FOCUSING, attempts = 1, shots = 1)),
            show(pose = 11, praise = 1, results = listOf(SHOT))),
        Sample("撮影直後・褒め言葉", SelfieUiState(connected = true, session = session(SelfiePhase.COUNTDOWN, remaining = 5, attempts = 2, shots = 2)),
            show(pose = 16, praise = 9, results = listOf(SHOT, SHOT))),
        Sample("AF失敗", SelfieUiState(connected = true, session = session(SelfiePhase.ERROR, attempts = 2, shots = 2)),
            show(pose = 16, praise = 9, results = listOf(SHOT, SHOT))),
        Sample("終了", SelfieUiState(connected = true, session = session(SelfiePhase.COMPLETED, attempts = 5, shots = 4, skipped = 1)),
            show(pose = 29, praise = 6, results = listOf(SHOT, SHOT, SKIPPED, SHOT, SHOT))),
        Sample("停止", SelfieUiState(connected = true, session = session(SelfiePhase.STOPPED, attempts = 2, shots = 2)),
            show(pose = 16, praise = 9, results = listOf(SHOT, SHOT))),
        Sample("切断", SelfieUiState(session = session(SelfiePhase.ERROR, attempts = 1, shots = 1, disconnected = true)),
            show(pose = 11, praise = 1, results = listOf(SHOT))),
    )
}

class KawaiiPreviewProvider : PreviewParameterProvider<KawaiiPreviewSamples.Sample> {
    override val values: Sequence<KawaiiPreviewSamples.Sample> get() = KawaiiPreviewSamples.samples.asSequence()
    override fun getDisplayName(index: Int): String? = KawaiiPreviewSamples.samples.getOrNull(index)?.name
}

@Preview(name = "kawaii", group = "SELFIE・kawaii", widthDp = 393, heightDp = 800, apiLevel = 36, locale = "ja")
@Composable
fun KawaiiSelfiePreview(@PreviewParameter(KawaiiPreviewProvider::class) sample: KawaiiPreviewSamples.Sample) {
    // Deliberately no runtime adapter: clicks in interactive preview are no-ops.
    SelfieTheme(skin = AppSkin.KAWAII) { KawaiiSelfieScreen(sample.state, sample.show, onAction = {}) }
}

@Preview(name = "小画面・大きな文字・kawaii", group = "SELFIE・kawaii", widthDp = 320, heightDp = 480,
    fontScale = 1.5f, apiLevel = 36, locale = "ja")
@Composable
fun KawaiiSelfieAccessiblePreview() {
    KawaiiSelfiePreview(KawaiiPreviewSamples.samples[3])
}
