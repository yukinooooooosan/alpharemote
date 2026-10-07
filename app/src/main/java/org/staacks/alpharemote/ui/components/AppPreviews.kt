package org.staacks.alpharemote.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.staacks.alpharemote.ui.about.AboutScreen
import org.staacks.alpharemote.ui.manual.ManualScreen
import org.staacks.alpharemote.ui.selfie.SelfieTheme
import org.staacks.alpharemote.ui.settings.ConnectionScreen
import org.staacks.alpharemote.ui.settings.SettingsViewModel.SettingsUICameraState
import org.staacks.alpharemote.ui.settings.SettingsViewModel.SettingsUIState

object ConnectionPreviewSamples {
    val ready = SettingsUIState(SettingsUICameraState.CONNECTED, null, "ILCE-6700", true, true, true, true, true)
    val samples = listOf(
        "未登録" to ready.copy(cameraState = SettingsUICameraState.NOT_ASSOCIATED, cameraName = null),
        "接続済み" to ready,
        "登録済み・未接続" to ready.copy(cameraState = SettingsUICameraState.OFFLINE),
        "権限待ち" to ready.copy(cameraState = SettingsUICameraState.OFFLINE, bluetoothPermissionGranted = false, notificationPermissionGranted = false),
        "Bluetooth OFF" to ready.copy(cameraState = SettingsUICameraState.NOT_ASSOCIATED, bluetoothEnabled = false),
        "ペアリング未完了" to ready.copy(cameraState = SettingsUICameraState.NOT_BONDED),
        "リモコン設定OFF" to ready.copy(cameraState = SettingsUICameraState.REMOTE_DISABLED),
        "接続エラー" to ready.copy(cameraState = SettingsUICameraState.ERROR, cameraError = "カメラに接続できませんでした。"),
    )
}

class ConnectionPreviewProvider : PreviewParameterProvider<SettingsUIState> {
    override val values get() = ConnectionPreviewSamples.samples.asSequence().map { it.second }
    override fun getDisplayName(index: Int) = ConnectionPreviewSamples.samples.getOrNull(index)?.first
}

@Preview(name = "通常", group = "カメラ接続", widthDp = 393, heightDp = 800, apiLevel = 36, locale = "ja", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "ダーク", group = "カメラ接続", widthDp = 393, heightDp = 800, apiLevel = 36, locale = "ja", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ConnectionPreview(@PreviewParameter(ConnectionPreviewProvider::class) state: SettingsUIState) {
    SelfieTheme { ConnectionScreen(state, onAction = {}) }
}

@Preview(name = "通常", group = "取説", widthDp = 393, heightDp = 800, apiLevel = 36, locale = "ja", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "ダーク", group = "取説", widthDp = 393, heightDp = 800, apiLevel = 36, locale = "ja", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ManualPreview() { SelfieTheme { ManualScreen() } }

@Preview(name = "通常", group = "アプリ情報", widthDp = 393, heightDp = 800, apiLevel = 36, locale = "ja", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "ダーク", group = "アプリ情報", widthDp = 393, heightDp = 800, apiLevel = 36, locale = "ja", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AboutPreview() { SelfieTheme { AboutScreen("0.1.5", 6, onLicense = {}, onOpenSource = {}) } }
