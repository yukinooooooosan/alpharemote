package org.staacks.alpharemote.ui.selfie

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.staacks.alpharemote.selfie.SelfiePhase
import org.staacks.alpharemote.selfie.SelfieSettings
import org.staacks.alpharemote.selfie.SelfieState
import org.staacks.alpharemote.service.AlphaRemoteService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "ja-w393dp-h800dp")
class SelfieScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allSampleStatesRenderInLightMode() = checkSamples(dark = false)
    @Test fun allSampleStatesRenderInDarkMode() = checkSamples(dark = true)

    private fun checkSamples(dark: Boolean) {
        val state = mutableStateOf(SelfieUiState())
        compose.setContent { SelfieTheme(dark) { SelfieScreen(state.value, onAction = {}) } }
        val expectedPhases = listOf("05", "05", "03", "ピント合わせ中", "SHOT", "AF ×", "5 / 5", "STOP", "OFFLINE", "05", "05", "3 / 5")
        SelfiePreviewSamples.samples.forEachIndexed { index, sample ->
            compose.runOnIdle { state.value = sample.state }
            compose.onNodeWithTag("phase_display").assertIsDisplayed().assertTextEquals(expectedPhases[index])
            compose.onNodeWithTag("connection_status", useUnmergedTree = true)
                .assertTextEquals(if (sample.state.connected) "カメラ接続中" else "カメラ未接続")
            compose.onNodeWithTag("start_stop").assertIsDisplayed()
                .assertTextEquals(if (sample.state.session.running) "STOP" else "START")
            compose.onNodeWithTag("flow").assertExists()
            if (sample.state.session.running) {
                compose.onNodeWithTag("options").assertDoesNotExist()
                compose.onNodeWithTag("camera_settings").assertIsNotEnabled()
                compose.onNodeWithTag("results").assertExists()
            } else {
                compose.onNodeWithTag("options").assertExists()
                compose.onNodeWithTag("camera_settings").assertIsEnabled()
            }
        }
        compose.onNodeWithTag("phase_hint").assertTextEquals("5回中 3回撮影 · 2回スキップ")
    }

    @Test fun idleHintDependsOnConnection() {
        val state = mutableStateOf(SelfieUiState())
        compose.setContent { SelfieTheme { SelfieScreen(state.value, onAction = {}) } }
        compose.onNodeWithTag("phase_hint").assertTextEquals("カメラを接続して、撮影を始めましょう。")
        compose.runOnIdle { state.value = SelfieUiState(connected = true, settings = SelfieSettings(countdownSeconds = 10)) }
        compose.onNodeWithTag("phase_hint").assertTextEquals("STARTのあと、スマホを置いて撮影位置へ")
        compose.onNodeWithTag("phase_display").assertTextEquals("10")
    }

    @Test fun settingsAndButtonsEmitEventsWithoutOperatingCamera() {
        val state = mutableStateOf(SelfieUiState())
        val events = mutableListOf<SelfieUiAction>()
        compose.setContent {
            SelfieTheme { SelfieScreen(state.value, onAction = { action ->
                events.add(action)
                if (action is SelfieUiAction.ChangeSettings) state.value = state.value.copy(settings = action.settings)
            }) }
        }
        compose.onNodeWithTag("camera_settings").performClick()
        compose.onNodeWithTag("start_stop").performClick()
        compose.onNodeWithTag("delay_10").performScrollTo().performClick()
        compose.onNodeWithTag("hold_100").performScrollTo().performClick()
        compose.onNodeWithTag("cycles_0").performScrollTo().performClick()
        compose.onNodeWithTag("sound_0").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(SelfieUiAction.OpenConnection, events[0])
            assertEquals(SelfieUiAction.StartStop, events[1])
            assertEquals(listOf(10, 10, 10, 10), events.drop(2).map { (it as SelfieUiAction.ChangeSettings).settings.countdownSeconds })
            assertEquals(SelfieSettings(countdownSeconds = 10, holdMillis = 100, cycles = 0, countdownSound = false),
                (events.last() as SelfieUiAction.ChangeSettings).settings)
            assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
        }
        compose.onNodeWithTag("cycles_0").assertIsSelected()
        compose.onNodeWithTag("cycles_5").assertIsNotSelected()
        compose.onNodeWithTag("phase_display").assertTextEquals("10")
    }

    @Test fun settingsLoadingDisablesStartButNeverDisablesStop() {
        val state = mutableStateOf(SelfieUiState(settingsLoaded = false))
        compose.setContent { SelfieTheme { SelfieScreen(state.value, onAction = {}) } }
        compose.onNodeWithTag("start_stop").assertIsNotEnabled()
        compose.onNodeWithTag("delay_3").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("sound_1").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(session = SelfieState(phase = SelfiePhase.FOCUSING)) }
        compose.onNodeWithTag("start_stop").assertIsEnabled().assertTextEquals("STOP")
    }

    @Test fun disconnectRestoresSettingsAndRequiresNewStart() {
        val state = SelfieUiState(session = SelfieState(phase = SelfiePhase.ERROR, disconnected = true, attempts = 2))
        compose.setContent { SelfieTheme { SelfieScreen(state, onAction = {}) } }
        compose.onNodeWithTag("phase_display").assertTextEquals("OFFLINE")
        compose.onNodeWithTag("start_stop").assertTextEquals("START")
        compose.onNodeWithTag("options").assertExists()
    }

    @Test
    @Config(qualifiers = "ja-w320dp-h480dp")
    fun stopStaysVisibleWithLargeTextOnSmallScreen() {
        RuntimeEnvironment.setFontScale(1.5f)
        compose.setContent { SelfieTheme { SelfieScreen(SelfiePreviewSamples.samples[2].state, onAction = {}) } }
        compose.onNodeWithTag("start_stop").assertIsDisplayed().assertIsEnabled().assertTextEquals("STOP")
        compose.onNodeWithTag("phase_display").assertIsDisplayed()
        compose.onNodeWithTag("options").assertDoesNotExist()
    }

    @Test fun actualPreviewClicksCannotStartOrStopServiceOrSaveSettings() {
        val state = mutableStateOf(SelfieUiState())
        val sessionBefore = AlphaRemoteService.selfieState.value
        val serviceBefore = AlphaRemoteService.serviceState.value
        compose.setContent { SelfiePreview(state.value) }
        compose.onNodeWithTag("start_stop").performClick()
        compose.onNodeWithTag("camera_settings").performClick()
        compose.onNodeWithTag("delay_3").performScrollTo().performClick()
        compose.onNodeWithTag("delay_5").assertIsSelected()
        compose.onNodeWithTag("sound_0").performScrollTo().performClick()
        compose.onNodeWithTag("sound_1").assertIsSelected()
        compose.runOnIdle { state.value = SelfiePreviewSamples.samples[3].state }
        compose.onNodeWithTag("start_stop").performClick().assertTextEquals("STOP")
        compose.runOnIdle {
            assertEquals(sessionBefore, AlphaRemoteService.selfieState.value)
            assertEquals(serviceBefore, AlphaRemoteService.serviceState.value)
            assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
        }
    }
}
