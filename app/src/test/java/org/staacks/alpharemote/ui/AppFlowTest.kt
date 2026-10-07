package org.staacks.alpharemote.ui

import android.os.Looper
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.widget.TextView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.findNavController
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowDialog
import org.staacks.alpharemote.MainActivity
import org.staacks.alpharemote.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "ja-w393dp-h800dp")
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun tabTapsRestoreManualExpansionAndSelectedTabAfterRecreation() {
        compose.onNodeWithTag("nav_${R.id.navigation_manual}").performClick().assertIsSelected()
        compose.onNodeWithTag("faq_single").performScrollTo().performClick()
        compose.onNodeWithTag("faq_single_body").assertExists()
        compose.onNodeWithTag("nav_${R.id.navigation_about}").performClick().assertIsSelected()
        compose.onNodeWithTag("nav_${R.id.navigation_manual}").performClick().assertIsSelected()
        compose.onNodeWithTag("faq_single_body").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("nav_${R.id.navigation_manual}").assertIsSelected()
        compose.onNodeWithTag("faq_single_body").assertExists()
        compose.onNodeWithTag("nav_${R.id.navigation_selfie}").performClick().assertIsSelected()
        compose.onNodeWithTag("selfie_screen").assertExists()
        compose.runOnIdle {
            assertEquals(R.id.navigation_selfie,
                compose.activity.findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id)
        }
    }

    @Test fun aboutLicenseButtonOpensExactBundledGplOffline() {
        compose.onNodeWithTag("nav_${R.id.navigation_about}").performClick()
        compose.onNodeWithTag("license_button").performScrollTo().performClick()
        compose.runOnIdle {
            val dialog = ShadowDialog.getLatestDialog()
            val expected = compose.activity.assets.open("GPL-3.0.txt").bufferedReader().use { it.readText() }
            assertEquals(expected, dialog.findViewById<TextView>(R.id.license_text).text.toString())
            dialog.dismiss()
        }
    }

    @Test fun pairingButtonStillUsesExistingCameraPreparationAndCancel() {
        compose.runOnIdle {
            shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT)
            shadowOf(BluetoothAdapter.getDefaultAdapter()).setState(BluetoothAdapter.STATE_ON)
        }
        compose.onNodeWithTag("nav_${R.id.navigation_settings}").performClick()
        compose.onNodeWithTag("pair_camera").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(dialog)
            assertEquals(compose.activity.getString(R.string.settings_camera_add_info),
                dialog.findViewById<TextView>(android.R.id.message).text.toString())
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(dialog.isShowing)
            assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
        }
        compose.onNodeWithTag("connection_screen").assertExists()
    }
}
