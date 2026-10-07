package org.staacks.alpharemote.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import org.staacks.alpharemote.R
import org.staacks.alpharemote.ui.components.*
import org.staacks.alpharemote.ui.settings.SettingsViewModel.SettingsUICameraState
import org.staacks.alpharemote.ui.settings.SettingsViewModel.SettingsUIAction
import org.staacks.alpharemote.ui.settings.SettingsViewModel.SettingsUIState

/** Rendering only: pairing, permission launchers and system checks stay in SettingsFragment. */
@Composable
fun ConnectionScreen(state: SettingsUIState, onAction: (SettingsUIAction) -> Unit) {
    SelfiePage("connection_screen") {
        SelfiePageTitle(stringResource(R.string.selfie_connection_title), stringResource(R.string.ui_connection_subtitle))
        SelfieSection {
            Text(stringResource(if (state.cameraState == SettingsUICameraState.CONNECTED) R.string.selfie_connected else R.string.selfie_offline),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            state.cameraName?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
            val status = when (state.cameraState) {
                SettingsUICameraState.CONNECTED -> stringResource(R.string.ui_camera_ready)
                SettingsUICameraState.OFFLINE -> stringResource(R.string.settings_camera_offline, state.cameraName ?: stringResource(R.string.settings_camera_unknown_name))
                SettingsUICameraState.NOT_ASSOCIATED -> stringResource(R.string.settings_camera_not_associated)
                SettingsUICameraState.NOT_BONDED -> stringResource(R.string.settings_camera_not_bonded)
                SettingsUICameraState.REMOTE_DISABLED -> stringResource(R.string.settings_camera_remote_disabled)
                SettingsUICameraState.ERROR -> stringResource(R.string.settings_camera_error, state.cameraError.orEmpty())
            }
            Text(status, Modifier.testTag("camera_status"), style = MaterialTheme.typography.bodyMedium)
            if (state.cameraState != SettingsUICameraState.ERROR) state.cameraError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (!state.bluetoothEnabled) SelfieNote(stringResource(R.string.settings_bluetooth_disabled))
            if (state.cameraState == SettingsUICameraState.NOT_ASSOCIATED) {
                SelfiePrimaryButton(stringResource(R.string.settings_camera_add), { onAction(SettingsUIAction.PAIR) },
                    Modifier.testTag("pair_camera"), enabled = state.bluetoothEnabled)
            } else {
                OutlinedButton({ onAction(SettingsUIAction.UNPAIR) }, Modifier.fillMaxWidth().testTag("unpair_camera")) {
                    Text(stringResource(R.string.settings_camera_remove))
                }
            }
        }
        SelfieSection(stringResource(R.string.ui_permissions)) {
            PermissionRow(R.string.ui_bluetooth_permission, state.bluetoothPermissionGranted, "bluetooth", R.string.settings_bluetooth_permission_button,
                R.string.settings_missing_bluetooth_permission_warning) { onAction(SettingsUIAction.REQUEST_BLUETOOTH_PERMISSION) }
            PermissionRow(R.string.ui_notification_permission, state.notificationPermissionGranted, "notification", R.string.settings_notification_permission_button,
                R.string.settings_missing_notification_permission_warning) { onAction(SettingsUIAction.REQUEST_NOTIFICATION_PERMISSION) }
        }
        SelfieSection(stringResource(R.string.ui_camera_setup)) {
            SelfieNote(stringResource(R.string.selfie_connection_guide))
            if (!state.locationServiceEnabled) SelfieNote(stringResource(R.string.settings_location_service_disabled))
            if (!state.bleScanningEnabled) SelfieNote(stringResource(R.string.settings_ble_scanning_disabled))
            TextButton({ onAction(SettingsUIAction.HELP_CONNECTION) }, Modifier.fillMaxWidth().testTag("connection_help")) {
                Text(stringResource(R.string.help_settings_connection_troubleshooting_title))
            }
        }
    }
}

@Composable
private fun PermissionRow(title: Int, granted: Boolean, tag: String, button: Int, explanation: Int, onRequest: () -> Unit) {
    Column {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        if (granted) SelfieNote(stringResource(R.string.ui_permission_granted)) else {
            SelfieNote(stringResource(explanation))
            TextButton(onRequest, Modifier.testTag("permission_$tag")) { Text(stringResource(button)) }
        }
    }
}
