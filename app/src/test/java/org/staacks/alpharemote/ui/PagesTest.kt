package org.staacks.alpharemote.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.staacks.alpharemote.ui.about.AboutScreen
import org.staacks.alpharemote.ui.components.ConnectionPreviewSamples
import org.staacks.alpharemote.ui.components.ConnectionPreview
import org.staacks.alpharemote.ui.manual.ManualScreen
import org.staacks.alpharemote.ui.selfie.SelfieTheme
import org.staacks.alpharemote.ui.settings.ConnectionScreen
import org.staacks.alpharemote.ui.settings.SettingsViewModel.SettingsUIAction
import org.staacks.alpharemote.ui.settings.SettingsViewModel.SettingsUICameraState
import org.staacks.alpharemote.service.AlphaRemoteService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "ja-w393dp-h800dp")
class PagesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun connectionStatesAndActionsRemainSeparateFromCameraRuntime() {
        val state = mutableStateOf(ConnectionPreviewSamples.ready)
        val events = mutableListOf<SettingsUIAction>()
        compose.setContent { SelfieTheme { ConnectionScreen(state.value, events::add) } }
        compose.onNodeWithTag("camera_status").assertTextEquals("撮影の準備ができています。")
        compose.onNodeWithTag("unpair_camera").performScrollTo().performClick()
        compose.runOnIdle { state.value = state.value.copy(cameraState = SettingsUICameraState.NOT_ASSOCIATED,
            bluetoothPermissionGranted = false, notificationPermissionGranted = false) }
        compose.onNodeWithTag("pair_camera").performScrollTo().performClick()
        compose.onNodeWithTag("permission_bluetooth").performScrollTo().performClick()
        compose.onNodeWithTag("permission_notification").performScrollTo().performClick()
        compose.onNodeWithTag("connection_help").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf(SettingsUIAction.UNPAIR, SettingsUIAction.PAIR, SettingsUIAction.REQUEST_BLUETOOTH_PERMISSION,
                SettingsUIAction.REQUEST_NOTIFICATION_PERMISSION, SettingsUIAction.HELP_CONNECTION), events)
            state.value = state.value.copy(bluetoothEnabled = false)
            assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
        }
        compose.onNodeWithTag("pair_camera").performScrollTo().assertIsNotEnabled()
    }

    @Test fun connectionSamplesRenderInBothThemes() {
        val state = mutableStateOf(ConnectionPreviewSamples.ready)
        val dark = mutableStateOf(false)
        compose.setContent { SelfieTheme(dark.value) { ConnectionScreen(state.value, onAction = {}) } }
        listOf(false, true).forEach { mode ->
            ConnectionPreviewSamples.samples.forEach { (_, sample) ->
                compose.runOnIdle { state.value = sample; dark.value = mode }
                compose.onNodeWithTag("camera_status").performScrollTo().assertIsDisplayed()
                compose.onNodeWithTag(if (sample.cameraState == SettingsUICameraState.NOT_ASSOCIATED) "pair_camera" else "unpair_camera")
                    .performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test fun manualExplainsLateFocusAndFaqsExpandInBothThemes() {
        val dark = mutableStateOf(false)
        compose.setContent { SelfieTheme(dark.value) { ManualScreen() } }
        compose.onNodeWithTag("focus_flow").performScrollTo().assertTextEquals("5 → 4 → 3 → 2 → 1 → AF → SHOT")
        listOf(false, true).forEach { mode ->
            compose.runOnIdle { dark.value = mode }
            listOf("single", "burst", "failure", "infinite", "connection", "disconnect", "camera", "sound").forEach { key ->
                compose.onNodeWithTag("faq_$key").performScrollTo().performClick()
                compose.onNodeWithTag("faq_${key}_body").assertExists()
                compose.onNodeWithTag("faq_$key").performScrollTo().performClick()
                compose.waitForIdle()
                compose.onNodeWithTag("faq_${key}_body").assertDoesNotExist()
            }
        }
    }

    @Test fun aboutPreservesCreditsLicenseAndSourceLinks() {
        val dark = mutableStateOf(false)
        var licenses = 0
        val urls = mutableListOf<String>()
        compose.setContent { SelfieTheme(dark.value) { AboutScreen("0.1.5", 6, { licenses++ }, urls::add) } }
        listOf(false, true).forEach { mode ->
            compose.runOnIdle { dark.value = mode }
            compose.onNodeWithText("原作：α-Remote © Sebastian Staacks", substring = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("license_button").performScrollTo().performClick()
            compose.onNodeWithTag("fork_source").performScrollTo().performClick()
            compose.onNodeWithTag("upstream_source").performScrollTo().performClick()
        }
        compose.runOnIdle {
            assertEquals(2, licenses)
            assertEquals(listOf("https://github.com/yukinooooooosan/alpharemote/tree/feature/selfie-mode", "https://github.com/Staacks/alpharemote",
                "https://github.com/yukinooooooosan/alpharemote/tree/feature/selfie-mode", "https://github.com/Staacks/alpharemote"), urls)
        }
    }

    @Test fun connectionPreviewNeverStartsPairingOrService() {
        val before = AlphaRemoteService.serviceState.value
        compose.setContent { ConnectionPreview(ConnectionPreviewSamples.samples[0].second) }
        compose.onNodeWithTag("pair_camera").performScrollTo().performClick()
        compose.onNodeWithTag("connection_help").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(before, AlphaRemoteService.serviceState.value)
            assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
        }
    }
}
