package org.staacks.alpharemote.ui.selfie

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.staacks.alpharemote.ui.appearance.AppSkin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "ja-w393dp-h800dp")
class KawaiiSelfieScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun everySampleShowsItsSceneAndTheRightMainButton() {
        val sample = mutableStateOf(KawaiiPreviewSamples.samples.first())
        compose.setContent { SelfieTheme(skin = AppSkin.KAWAII) { KawaiiSelfieScreen(sample.value.state, sample.value.show, onAction = {}) } }
        val headlines = listOf("とるよ〜！", "とるよ〜！", "6", "3", "ピント\n合わせ中…", "ぱしゃっ", "あれれ？", "おつかれ\nさま〜！",
            "ストップ\nしたよ", "あれ、\nカメラが…")
        KawaiiPreviewSamples.samples.forEachIndexed { index, next ->
            compose.runOnIdle { sample.value = next }
            compose.onNodeWithTag("kawaii_headline").assertTextEquals(headlines[index])
            compose.onNodeWithTag("start_stop").assertIsDisplayed().assertIsEnabled()
                .assertTextEquals(if (next.state.session.running) "ストップ" else if (index < 2) "スタート" else "もう1回！")
        }
    }

    @Test fun poseAndPraiseComeFromTheSharedShowIds() {
        val sample = mutableStateOf(KawaiiPreviewSamples.samples[2])
        compose.setContent { SelfieTheme(skin = AppSkin.KAWAII) { KawaiiSelfieScreen(sample.value.state, sample.value.show, onAction = {}) } }
        compose.onNodeWithTag("kawaii_pose").assertTextEquals("ハート作って！")
        compose.runOnIdle { sample.value = KawaiiPreviewSamples.samples[5] }
        compose.onNodeWithTag("kawaii_praise").assertTextEquals("奇跡の1枚！")
        compose.runOnIdle { sample.value = KawaiiPreviewSamples.samples[7] }
        compose.onNodeWithTag("kawaii_result").assertTextEquals("5かい中 4かい パシャッ！")
    }

    @Test fun buttonsOnlyEmitEventsAndConnectionIsLockedWhileShooting() {
        val sample = mutableStateOf(KawaiiPreviewSamples.samples[1])
        val events = mutableListOf<SelfieUiAction>()
        compose.setContent { SelfieTheme(skin = AppSkin.KAWAII) { KawaiiSelfieScreen(sample.value.state, sample.value.show, onAction = { events += it }) } }
        compose.onNodeWithTag("camera_settings").performClick()
        compose.onNodeWithTag("start_stop").performClick()
        compose.runOnIdle { sample.value = KawaiiPreviewSamples.samples[3] }
        compose.onNodeWithTag("camera_settings").assertIsNotEnabled()
        compose.onNodeWithTag("start_stop").performClick()
        compose.runOnIdle {
            assertEquals(listOf(SelfieUiAction.OpenConnection, SelfieUiAction.StartStop, SelfieUiAction.StartStop), events)
            assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
        }
    }

    @Test
    @Config(qualifiers = "ja-w320dp-h480dp")
    fun stopStaysVisibleWithLargeTextOnSmallScreen() {
        RuntimeEnvironment.setFontScale(1.5f)
        compose.setContent { KawaiiSelfieAccessiblePreview() }
        compose.onNodeWithTag("start_stop").assertIsDisplayed().assertIsEnabled().assertTextEquals("ストップ")
    }
}
