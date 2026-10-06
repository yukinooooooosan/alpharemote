package org.staacks.alpharemote.ui.selfie

import android.widget.TextView
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.findNavController
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.staacks.alpharemote.MainActivity
import org.staacks.alpharemote.R

/** Exercises the runtime adapter, separate from the deliberately inert preview. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "ja-w393dp-h800dp")
class SelfieFragmentTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun disconnectedStartShowsConnectionGuideInsteadOfStartingService() {
        val viewModel = ViewModelProvider(compose.activity)[SelfieViewModel::class.java]
        compose.waitUntil(timeoutMillis = 5000) { viewModel.settings.value != null }
        compose.onNodeWithTag("start_stop").assertIsEnabled().performClick()
        compose.runOnIdle {
            val dialog = ShadowDialog.getLatestDialog()
            assertNotNull(dialog)
            assertEquals(compose.activity.getString(R.string.selfie_connection_hint),
                dialog.findViewById<TextView>(android.R.id.message).text.toString())
            assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
            dialog.dismiss()
        }
    }

    @Test fun connectionButtonNavigatesToExistingXmlScreen() {
        compose.onNodeWithTag("camera_settings").performClick()
        compose.runOnIdle {
            assertEquals(R.id.navigation_settings,
                compose.activity.findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id)
            assertNotNull(compose.activity.findViewById<TextView>(R.id.connection_title))
        }
    }
}
