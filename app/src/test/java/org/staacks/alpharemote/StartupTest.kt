package org.staacks.alpharemote

import android.widget.TextView
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    private fun checkLauncherScreen() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            assertNotNull(activity.findViewById<TextView>(R.id.phase_display))
            assertNotNull(activity.findViewById<TextView>(R.id.start_stop))
        }
    }
}
