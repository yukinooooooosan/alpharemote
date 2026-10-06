package org.staacks.alpharemote

import android.widget.TextView
import android.widget.Button
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class StartupTest {
    @Test fun launcherActivityDisplaysSelfieScreen() {
        checkLauncherScreen()
    }

    @Test
    @Config(qualifiers = "night")
    fun launcherActivityDisplaysSelfieScreenInDarkMode() {
        checkLauncherScreen()
    }

    @Test fun fourTabsOpenWithConnectionScreen() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            val nav = activity.findViewById<BottomNavigationView>(R.id.nav_view)
            assertEquals(listOf(R.id.navigation_selfie, R.id.navigation_settings, R.id.navigation_manual, R.id.navigation_about),
                (0 until nav.menu.size()).map { nav.menu.getItem(it).itemId })
            activity.navigateTo(R.id.navigation_settings)
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(activity.findViewById<TextView>(R.id.connection_title))
            assertEquals(R.id.navigation_settings,
                activity.findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id)
            activity.navigateTo(R.id.navigation_manual)
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(activity.findViewById<TextView>(R.id.manual_title))
            assertEquals(R.id.navigation_manual,
                activity.findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id)
            activity.navigateTo(R.id.navigation_about)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(R.id.navigation_about,
                activity.findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id)
            checkLicenseDialog(activity)
            activity.navigateTo(R.id.navigation_selfie)
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(activity.findViewById<ComposeView>(R.id.selfie_compose_view))
            val receivers = activity.packageManager.getPackageInfo(activity.packageName,
                PackageManager.GET_RECEIVERS).receivers.orEmpty()
            assertTrue(receivers.none { it.name.endsWith("CameraBroadcastReceiver") })
        }
    }

    @Test
    @Config(qualifiers = "night")
    fun manualAndBundledLicenseOpenInDarkMode() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            activity.navigateTo(R.id.navigation_manual)
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(activity.findViewById<TextView>(R.id.manual_title))
            activity.navigateTo(R.id.navigation_about)
            shadowOf(Looper.getMainLooper()).idle()
            checkLicenseDialog(activity)
        }
    }

    private fun checkLicenseDialog(activity: MainActivity) {
        activity.findViewById<Button>(R.id.about_license_button).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        val dialog: Dialog = ShadowDialog.getLatestDialog()
        val text = dialog.findViewById<TextView>(R.id.license_text).text.toString()
        assertTrue(text.contains("GNU GENERAL PUBLIC LICENSE"))
        assertTrue(text.contains("END OF TERMS AND CONDITIONS"))
        val bundled = activity.assets.open("GPL-3.0.txt").bufferedReader().use { it.readText() }
        assertEquals(bundled, text)
        dialog.dismiss()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun obsoleteNavigationRequestFallsBackToSelfie() {
        val intent = Intent().putExtra(MainActivity.NAVIGATE_TO_INTENT_EXTRA, -1)
        Robolectric.buildActivity(MainActivity::class.java, intent).use { controller ->
            val activity = controller.setup().get()
            assertNotNull(activity.findViewById<ComposeView>(R.id.selfie_compose_view))
        }
    }

    private fun checkLauncherScreen() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            assertNotNull(activity.findViewById<ComposeView>(R.id.selfie_compose_view))
        }
    }
}
