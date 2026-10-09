package org.staacks.alpharemote.ui.about

import android.content.Intent
import androidx.core.net.toUri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.staacks.alpharemote.BuildConfig
import org.staacks.alpharemote.R
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.staacks.alpharemote.ui.appearance.AppSkin
import org.staacks.alpharemote.ui.appearance.AppearanceViewModel
import org.staacks.alpharemote.ui.appearance.RuntimeSelfieTheme

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.about_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val appearance = ViewModelProvider(requireActivity())[AppearanceViewModel::class.java]
            setContent {
                val skin by appearance.skin.collectAsStateWithLifecycle()
                RuntimeSelfieTheme(appearance) {
                    AboutScreen(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, ::showLicense, ::openURL,
                        skin = skin ?: AppSkin.SIMPLE, skinLoaded = skin != null, onSkinChange = appearance::update)
                }
            }
        }

    fun showLicense() {
        if (childFragmentManager.findFragmentByTag("license") == null)
            LicenseDialogFragment().show(childFragmentManager, "license")
    }

    fun openURL(target: String) { startActivity(Intent(Intent.ACTION_VIEW, target.toUri())) }
}
