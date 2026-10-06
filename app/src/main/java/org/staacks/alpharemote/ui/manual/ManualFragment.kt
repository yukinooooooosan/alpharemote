package org.staacks.alpharemote.ui.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import org.staacks.alpharemote.databinding.FragmentManualBinding

class ManualFragment : Fragment() {
    private var binding: FragmentManualBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val screen = FragmentManualBinding.inflate(inflater, container, false)
        binding = screen
        ViewCompat.setOnApplyWindowInsetsListener(screen.manualContent) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        return screen.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
