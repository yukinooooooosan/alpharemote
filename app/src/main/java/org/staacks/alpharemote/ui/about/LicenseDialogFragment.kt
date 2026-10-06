package org.staacks.alpharemote.ui.about

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.staacks.alpharemote.R
import org.staacks.alpharemote.databinding.DialogLicenseBinding

/** Bundled license remains readable without a network connection. */
class LicenseDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val screen = DialogLicenseBinding.inflate(layoutInflater)
        screen.licenseText.text = requireContext().assets.open("GPL-3.0.txt").bufferedReader().use { it.readText() }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.selfie_about_license_button)
            .setView(screen.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }
}
