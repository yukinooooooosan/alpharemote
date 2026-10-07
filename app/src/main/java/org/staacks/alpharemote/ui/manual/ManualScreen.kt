package org.staacks.alpharemote.ui.manual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.staacks.alpharemote.R
import org.staacks.alpharemote.ui.components.*

@Composable
fun ManualScreen() {
    SelfiePage("manual_screen") {
        SelfiePageTitle(stringResource(R.string.manual_title), stringResource(R.string.ui_quick_start))
        SelfieSection {
            QuickStep(1, R.string.ui_step_connect, R.string.ui_step_connect_body)
            QuickStep(2, R.string.ui_step_settings, R.string.ui_step_settings_body)
            QuickStep(3, R.string.ui_step_start, R.string.ui_step_start_body)
            QuickStep(4, R.string.ui_step_shoot, R.string.ui_step_shoot_body)
        }
        SelfieSection(stringResource(R.string.ui_focus_title)) {
            Text(stringResource(R.string.ui_focus_flow), style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("focus_flow"))
            Text(stringResource(R.string.ui_focus_body), style = MaterialTheme.typography.bodyMedium)
        }
        SelfieSection(stringResource(R.string.ui_faq_capture)) {
            Faq("single", R.string.ui_faq_single, R.string.ui_faq_single_body)
            Faq("burst", R.string.ui_faq_burst, R.string.ui_faq_burst_body)
            Faq("failure", R.string.ui_faq_failure, R.string.manual_focus_body)
            Faq("infinite", R.string.ui_faq_infinite, R.string.ui_faq_infinite_body)
        }
        SelfieSection(stringResource(R.string.ui_faq_connection)) {
            Faq("connection", R.string.help_settings_connection_troubleshooting_title, R.string.help_settings_connection_troubleshooting_text)
            Faq("disconnect", R.string.ui_faq_disconnect, R.string.manual_stop_body)
            Faq("camera", R.string.ui_faq_camera, R.string.ui_faq_camera_body)
        }
        SelfieSection(stringResource(R.string.ui_faq_sound)) {
            Faq("sound", R.string.selfie_sound, R.string.manual_sound_body)
        }
    }
}

@Composable
private fun QuickStep(number: Int, title: Int, body: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Text(number.toString().padStart(2, '0'), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            SelfieNote(stringResource(body))
        }
    }
}

@Composable
private fun Faq(key: String, title: Int, body: Int) {
    var expanded by rememberSaveable(key) { mutableStateOf(false) }
    val actionLabel = stringResource(if (expanded) R.string.ui_collapse else R.string.ui_expand)
    val description = stringResource(if (expanded) R.string.ui_expanded else R.string.ui_collapsed)
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("faq_$key")
            .semantics { stateDescription = description }
            .clickable(role = Role.Button, onClickLabel = actionLabel) { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(title), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(expanded) {
            Text(stringResource(body), Modifier.padding(top = 8.dp, bottom = 8.dp).testTag("faq_${key}_body"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
