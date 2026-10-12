package org.staacks.alpharemote.ui.manual

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.staacks.alpharemote.ui.appearance.AppSkin
import org.staacks.alpharemote.ui.selfie.SelfieTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "ja-w393dp-h800dp")
class KawaiiManualScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stepsAndQuestionsRenderAndToggleOffline() {
        compose.setContent { SelfieTheme(skin = AppSkin.KAWAII) { KawaiiManualScreen() } }
        compose.onNodeWithTag("manual_screen").assertExists()
        (1..4).forEach { compose.onNodeWithTag("kawaii_step_$it").performScrollTo().assertIsDisplayed() }
        compose.onNodeWithTag("kawaii_faq_0_body").performScrollTo()
            .assertTextEquals("ピントが合わなかったので、その回はスキップしたよ。明るい場所で、顔をカメラに向けてみてね。スキップも1回に数えるよ。")
        compose.onNodeWithTag("kawaii_faq_4_body").assertDoesNotExist()
        compose.onNodeWithTag("kawaii_faq_4").performScrollTo().performClick()
        compose.onNodeWithTag("kawaii_faq_4_body").performScrollTo()
            .assertTextEquals("アプリが数えているのは、シャッターを押した回数だよ。撮れた写真はカメラで確認してね。")
        compose.onNodeWithTag("kawaii_faq_0").performScrollTo().performClick()
        compose.onNodeWithTag("kawaii_faq_0_body").assertDoesNotExist()
    }
}
