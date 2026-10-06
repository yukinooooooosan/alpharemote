package org.staacks.alpharemote

import android.widget.TextView
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
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

    @Test fun threeTabsOpenWithConnectionScreen() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            val nav = activity.findViewById<BottomNavigationView>(R.id.nav_view)
            assertEquals(listOf(R.id.navigation_selfie, R.id.navigation_settings, R.id.navigation_about),
                (0 until nav.menu.size()).map { nav.menu.getItem(it).itemId })
            activity.navigateTo(R.id.navigation_settings)
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(activity.findViewById<TextView>(R.id.connection_title))
            assertEquals(R.id.navigation_settings,
                activity.findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id)
            activity.navigateTo(R.id.navigation_about)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(R.id.navigation_about,
                activity.findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id)
            activity.navigateTo(R.id.navigation_selfie)
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(activity.findViewById<TextView>(R.id.start_stop))
            val receivers = activity.packageManager.getPackageInfo(activity.packageName,
                PackageManager.GET_RECEIVERS).receivers.orEmpty()
            assertTrue(receivers.none { it.name.endsWith("CameraBroadcastReceiver") })
        }
    }

    @Test fun obsoleteNavigationRequestFallsBackToSelfie() {
        val intent = Intent().putExtra(MainActivity.NAVIGATE_TO_INTENT_EXTRA, -1)
        Robolectric.buildActivity(MainActivity::class.java, intent).use { controller ->
            val activity = controller.setup().get()
            assertNotNull(activity.findViewById<TextView>(R.id.start_stop))
        }
    }

    private fun checkLauncherScreen() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            assertNotNull(activity.findViewById<TextView>(R.id.phase_display))
            assertNotNull(activity.findViewById<TextView>(R.id.start_stop))
        }
    }
}
