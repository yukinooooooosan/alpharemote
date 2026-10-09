package org.staacks.alpharemote.ui.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.staacks.alpharemote.ui.selfie.SelfieTheme

/** Runtime bridge only. IDE previews use SelfieTheme with an explicit sample skin. */
@Composable
fun RuntimeSelfieTheme(viewModel: AppearanceViewModel, content: @Composable () -> Unit) {
    val skin by viewModel.skin.collectAsStateWithLifecycle()
    SelfieTheme(skin = skin ?: AppSkin.SIMPLE, content = content)
}
