package org.staacks.alpharemote.ui.selfie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.staacks.alpharemote.ui.appearance.AppSkin
import org.staacks.alpharemote.ui.appearance.AppearanceViewModel
import org.staacks.alpharemote.ui.appearance.RuntimeSelfieTheme
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
        val appearance = ViewModelProvider(requireActivity())[AppearanceViewModel::class.java]
        (view as ComposeView).setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val session by AlphaRemoteService.selfieState.collectAsStateWithLifecycle()
            val service by AlphaRemoteService.serviceState.collectAsStateWithLifecycle()
            val show by AlphaRemoteService.selfieShowState.collectAsStateWithLifecycle()
            val skin by appearance.skin.collectAsStateWithLifecycle()
            RuntimeSelfieTheme(appearance) {
                val state = SelfieUiState(
                    settings = settings ?: SelfieSettings(),
                    settingsLoaded = settings != null,
                    connected = (service as? ServiceRunning)?.cameraState is CameraStateReady,
                    session = session,
                    results = show.results,
                )
                // kawaii is a separate show screen with fixed settings; Simple keeps the configurable screen.
                if (skin == AppSkin.KAWAII) {
                    KawaiiSelfieScreen(state, show, onAction = { handleAction(it, viewModel, appearance) })
                } else {
                    SelfieScreen(state, onAction = { handleAction(it, viewModel, appearance) })
                }
            }
        }
    }

    private fun handleAction(action: SelfieUiAction, viewModel: SelfieViewModel, appearance: AppearanceViewModel) {
        when (action) {
            SelfieUiAction.OpenConnection -> (requireActivity() as MainActivity).navigateTo(R.id.navigation_settings)
            SelfieUiAction.StartStop -> {
                if (AlphaRemoteService.selfieState.value.running) {
                    AlphaRemoteService.stopSelfie(requireContext())
                } else {
                    val settings = if (appearance.skin.value == AppSkin.KAWAII) SelfieSettings.KAWAII
                        else viewModel.settings.value ?: return
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
