package org.staacks.alpharemote.ui.selfie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.staacks.alpharemote.MainActivity
import org.staacks.alpharemote.R
import org.staacks.alpharemote.camera.CameraStateReady
import org.staacks.alpharemote.selfie.SelfieSettings
import org.staacks.alpharemote.service.AlphaRemoteService
import org.staacks.alpharemote.service.ServiceRunning

/** Runtime adapter only. Previews render SelfieScreen directly and never construct this fragment. */
class SelfieFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.selfie_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewModel = ViewModelProvider(requireActivity())[SelfieViewModel::class.java]
        (view as ComposeView).setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val session by AlphaRemoteService.selfieState.collectAsStateWithLifecycle()
            val service by AlphaRemoteService.serviceState.collectAsStateWithLifecycle()
            SelfieTheme {
                SelfieScreen(
                    state = SelfieUiState(
                        settings = settings ?: SelfieSettings(),
                        settingsLoaded = settings != null,
                        connected = (service as? ServiceRunning)?.cameraState is CameraStateReady,
                        session = session,
                    ),
                    onAction = { handleAction(it, viewModel) },
                )
            }
        }
    }

    private fun handleAction(action: SelfieUiAction, viewModel: SelfieViewModel) {
        when (action) {
            SelfieUiAction.OpenConnection -> (requireActivity() as MainActivity).navigateTo(R.id.navigation_settings)
            SelfieUiAction.StartStop -> {
                if (AlphaRemoteService.selfieState.value.running) {
                    AlphaRemoteService.stopSelfie(requireContext())
                } else {
                    val settings = viewModel.settings.value ?: return
                    if (!AlphaRemoteService.startSelfie(requireContext(), settings)) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(R.string.selfie_connection_hint)
                            .setPositiveButton(R.string.selfie_settings) { _, _ ->
                                (requireActivity() as MainActivity).navigateTo(R.id.navigation_settings)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
            }
            is SelfieUiAction.ChangeSettings -> {
                if (!AlphaRemoteService.selfieState.value.running) viewModel.update(action.settings)
            }
        }
    }
}
