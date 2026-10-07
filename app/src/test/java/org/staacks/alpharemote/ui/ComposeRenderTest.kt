package org.staacks.alpharemote.ui

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.staacks.alpharemote.ui.about.AboutScreen
import org.staacks.alpharemote.ui.components.ConnectionPreviewSamples
import org.staacks.alpharemote.ui.manual.ManualScreen
import org.staacks.alpharemote.ui.selfie.SelfiePreviewSamples
import org.staacks.alpharemote.ui.selfie.SelfieScreen
import org.staacks.alpharemote.ui.selfie.SelfieTheme
import org.staacks.alpharemote.ui.settings.ConnectionScreen
import java.io.File

/** Native Compose captures for visual inspection, without a camera or emulator.
 * Generated images stay under app/build; preview callbacks never invoke runtime adapters.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "ja-w393dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComposeRenderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureFourPagesAndRunningStatesInBothThemes() {
        val page = mutableStateOf("selfie")
        val dark = mutableStateOf(false)
        val sample = mutableStateOf(SelfiePreviewSamples.samples[1].state)
        compose.setContent {
            SelfieTheme(dark.value) {
                when (page.value) {
                    "manual" -> ManualScreen()
                    "about" -> AboutScreen("0.1.5", 6, {}, {})
                    "connection" -> ConnectionScreen(ConnectionPreviewSamples.ready, {})
                    else -> SelfieScreen(sample.value, {})
                }
            }
        }
        val directory = File("build/outputs/ui-previews").apply { mkdirs() }
        listOf(false, true).forEach { mode ->
            listOf("selfie", "connection", "manual", "about", "countdown", "focusing", "shooting").forEach { name ->
                compose.runOnIdle {
                    dark.value = mode
                    page.value = name
                    sample.value = SelfiePreviewSamples.samples[when (name) {
                        "countdown" -> 2; "focusing" -> 3; "shooting" -> 4; else -> 1
                    }].state
                }
                val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
                assertTrue(bitmap.width > 0 && bitmap.height > 0)
                val colors = mutableSetOf<Int>()
                for (y in 0 until bitmap.height step 8) for (x in 0 until bitmap.width step 8) colors.add(bitmap.getPixel(x, y))
                assertTrue("$name capture must contain rendered content", colors.size > 10)
                File(directory, "$name-${if (mode) "dark" else "light"}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
