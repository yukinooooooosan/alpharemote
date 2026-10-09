package org.staacks.alpharemote.ui.appearance

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.staacks.alpharemote.SettingsStore
import org.staacks.alpharemote.settings
import org.staacks.alpharemote.selfie.SelfieSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class AppearanceStoreTest {
    @Test fun existingInstallAndUnknownFutureSkinKeepSimpleDefault() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        context.settings.edit { it.remove(stringPreferencesKey("uiSkin")) }
        assertEquals(AppSkin.SIMPLE, SettingsStore(context).skin.first())
        context.settings.edit { it[stringPreferencesKey("uiSkin")] = "future-skin" }
        assertEquals(AppSkin.SIMPLE, SettingsStore(context).skin.first())
    }

    @Test fun skinRoundTripPreservesCameraAndShootingPreferences() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val store = SettingsStore(context)
        val shooting = SelfieSettings(countdownSeconds = 10, holdMillis = 500, cycles = 0, countdownSound = false)
        store.setSelfieSettings(shooting)
        store.setCameraId("ILCE-6700", "00:11:22:33:44:55")
        store.setSkin(AppSkin.KAWAII)
        val restored = SettingsStore(context)
        assertEquals(AppSkin.KAWAII, restored.skin.first())
        assertEquals(shooting, restored.selfieSettings.first())
        assertEquals("00:11:22:33:44:55" to "ILCE-6700", restored.getCameraId())
        restored.setSkin(AppSkin.SIMPLE)
        assertEquals(AppSkin.SIMPLE, SettingsStore(context).skin.first())
        assertEquals(shooting, store.selfieSettings.first())
    }
}
