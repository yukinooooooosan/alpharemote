package org.staacks.alpharemote.ui.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.staacks.alpharemote.R
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.staacks.alpharemote.ui.appearance.AppSkin
import org.staacks.alpharemote.ui.appearance.AppearanceViewModel
import org.staacks.alpharemote.ui.appearance.RuntimeSelfieTheme

class ManualFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.manual_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val appearance = ViewModelProvider(requireActivity())[AppearanceViewModel::class.java]
            setContent {
                val skin by appearance.skin.collectAsStateWithLifecycle()
                // kawaii has its own "あそびかた" page for its fixed flow; Simple keeps the full manual.
                RuntimeSelfieTheme(appearance) { if (skin == AppSkin.KAWAII) KawaiiManualScreen() else ManualScreen() }
            }
        }
}
