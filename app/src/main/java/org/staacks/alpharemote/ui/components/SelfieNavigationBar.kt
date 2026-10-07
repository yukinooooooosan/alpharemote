package org.staacks.alpharemote.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.staacks.alpharemote.R

@Composable
fun SelfieNavigationBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        listOf(
            Triple(R.id.navigation_selfie, R.drawable.ic_camera_black_24dp, R.string.selfie_title),
            Triple(R.id.navigation_settings, R.drawable.ic_settings_black_24dp, R.string.selfie_connection_title),
            Triple(R.id.navigation_manual, R.drawable.baseline_text_snippet_24, R.string.manual_title),
            Triple(R.id.navigation_about, R.drawable.ic_about_black_24dp, R.string.selfie_about_title),
        ).forEach { (id, icon, label) ->
            NavigationBarItem(selected == id, { onSelect(id) },
                icon = { Icon(painterResource(icon), contentDescription = null) },
                label = { Text(stringResource(label)) }, modifier = Modifier.testTag("nav_$id"))
        }
    }
}
