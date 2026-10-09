package org.staacks.alpharemote.ui.appearance

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.staacks.alpharemote.MainActivity
import org.staacks.alpharemote.R
import org.staacks.alpharemote.SettingsStore
import org.staacks.alpharemote.service.AlphaRemoteService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "ja-w393dp-h800dp")
class AppearanceFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun choosingSkinPersistsAcrossTabsAndActivityRecreationWithoutCameraActions() {
        val viewModel = ViewModelProvider(compose.activity)[AppearanceViewModel::class.java]
        compose.waitUntil(5000) { viewModel.skin.value != null }
        val before = AlphaRemoteService.selfieState.value
        compose.onNodeWithTag("nav_${R.id.navigation_about}").performClick()
        compose.onNodeWithTag("skin_1").performScrollTo().assertIsEnabled().performClick().assertIsSelected()
        compose.waitUntil(5000) { runBlocking { SettingsStore(compose.activity).skin.first() } == AppSkin.KAWAII }
        compose.onNodeWithTag("nav_${R.id.navigation_manual}").performClick()
        compose.onNodeWithTag("manual_screen").assertExists()
        compose.onNodeWithTag("nav_${R.id.navigation_settings}").performClick()
        compose.onNodeWithTag("connection_screen").assertExists()
        compose.onNodeWithTag("nav_${R.id.navigation_selfie}").performClick()
        compose.onNodeWithTag("selfie_screen").assertExists()
        compose.onNodeWithTag("nav_${R.id.navigation_about}").performClick()
        compose.onNodeWithTag("skin_1").performScrollTo().assertIsSelected()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("skin_1").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("skin_0").performClick().assertIsSelected()
        compose.waitUntil(5000) { runBlocking { SettingsStore(compose.activity).skin.first() } == AppSkin.SIMPLE }
        compose.runOnIdle { assertEquals(before, AlphaRemoteService.selfieState.value) }
    }

    @Test fun rapidSkinChangesSaveTheLastSelection() {
        val viewModel = ViewModelProvider(compose.activity)[AppearanceViewModel::class.java]
        compose.waitUntil(5000) { viewModel.skin.value != null }
        compose.runOnIdle {
            viewModel.update(AppSkin.KAWAII)
            viewModel.update(AppSkin.SIMPLE)
            viewModel.update(AppSkin.KAWAII)
        }
        compose.waitUntil(5000) { runBlocking { SettingsStore(compose.activity).skin.first() } == AppSkin.KAWAII }
        compose.runOnIdle { assertEquals(AppSkin.KAWAII, viewModel.skin.value) }
        compose.onNodeWithTag("nav_${R.id.navigation_about}").performClick()
        compose.onNodeWithTag("skin_1").assertIsSelected()
    }
}
