package org.staacks.alpharemote.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import org.staacks.alpharemote.R
import org.staacks.alpharemote.ui.appearance.AppSkin
import org.staacks.alpharemote.ui.components.*

/** Version and callbacks are inputs, so previews never launch external activities. */
@Composable
fun AboutScreen(versionName: String, versionCode: Int, onLicense: () -> Unit, onOpenSource: (String) -> Unit,
                skin: AppSkin = AppSkin.SIMPLE, skinLoaded: Boolean = true, onSkinChange: (AppSkin) -> Unit = {}) {
    SelfiePage("about_screen") {
        SelfiePageTitle(stringResource(R.string.ui_preferences_title))
        SelfieSection(stringResource(R.string.ui_appearance)) {
            SelfieSegmentedControl(stringResource(R.string.ui_skin), listOf(
                AppSkin.SIMPLE.ordinal to stringResource(R.string.ui_skin_simple),
                AppSkin.KAWAII.ordinal to stringResource(R.string.ui_skin_kawaii)),
                selected = skin.ordinal, enabled = skinLoaded, tag = "skin") { value ->
                onSkinChange(AppSkin.entries[value])
            }
            SelfieNote(stringResource(R.string.ui_skin_hint))
        }
        SelfieSection {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            SelfieNote(stringResource(R.string.about_version_info, versionName, versionCode))
            Text(stringResource(R.string.selfie_fork_credit), style = MaterialTheme.typography.bodyMedium)
        }
        SelfieSection(stringResource(R.string.selfie_about_credit_title)) {
            Text(stringResource(R.string.selfie_about_credit), style = MaterialTheme.typography.bodyMedium)
        }
        SelfieSection(stringResource(R.string.ui_license_section)) {
            Text(stringResource(R.string.selfie_about_license), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onLicense, Modifier.fillMaxWidth().testTag("license_button")) {
                Text(stringResource(R.string.selfie_about_license_button))
            }
            val fork = stringResource(R.string.selfie_source_url)
            val upstream = stringResource(R.string.about_github_code_url)
            TextButton({ onOpenSource(fork) }, Modifier.fillMaxWidth().testTag("fork_source")) { Text(stringResource(R.string.selfie_source)) }
            TextButton({ onOpenSource(upstream) }, Modifier.fillMaxWidth().testTag("upstream_source")) { Text(stringResource(R.string.selfie_about_upstream)) }
        }
    }
}
