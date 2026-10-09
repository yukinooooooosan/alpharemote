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
import org.staacks.alpharemote.ui.appearance.AppearanceViewModel
import org.staacks.alpharemote.ui.appearance.RuntimeSelfieTheme

class ManualFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.manual_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val appearance = ViewModelProvider(requireActivity())[AppearanceViewModel::class.java]
            setContent { RuntimeSelfieTheme(appearance) { ManualScreen() } }
        }
}
