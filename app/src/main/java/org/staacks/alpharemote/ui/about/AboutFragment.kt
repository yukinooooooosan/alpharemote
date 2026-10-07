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
import org.staacks.alpharemote.ui.selfie.SelfieTheme

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.about_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SelfieTheme { AboutScreen(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, ::showLicense, ::openURL) } }
        }

    fun showLicense() {
        if (childFragmentManager.findFragmentByTag("license") == null)
            LicenseDialogFragment().show(childFragmentManager, "license")
    }

    fun openURL(target: String) { startActivity(Intent(Intent.ACTION_VIEW, target.toUri())) }
}
