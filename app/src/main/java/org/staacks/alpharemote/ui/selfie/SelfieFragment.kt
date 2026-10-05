package org.staacks.alpharemote.ui.selfie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.staacks.alpharemote.MainActivity
import org.staacks.alpharemote.R
import org.staacks.alpharemote.camera.CameraStateReady
import org.staacks.alpharemote.databinding.FragmentSelfieBinding
import org.staacks.alpharemote.selfie.SelfiePhase
import org.staacks.alpharemote.selfie.SelfieSettings
import org.staacks.alpharemote.service.AlphaRemoteService
import org.staacks.alpharemote.service.ServiceRunning

class SelfieFragment : Fragment() {
    private var binding: FragmentSelfieBinding? = null
    private var settings = SelfieSettings()
    private var restoring = false
    private var settingsLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentSelfieBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = binding ?: return
        val viewModel = ViewModelProvider(requireActivity())[SelfieViewModel::class.java]
        settingsLoaded = false
        b.startStop.isEnabled = false
        b.cameraSettings.setOnClickListener { (activity as MainActivity).navigateTo(R.id.navigation_settings) }
        b.startStop.setOnClickListener {
            if (AlphaRemoteService.selfieState.value.running) {
                AlphaRemoteService.stopSelfie(requireContext())
            } else if (settingsLoaded && !AlphaRemoteService.startSelfie(requireContext(), settings)) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.selfie_connection_hint)
                    .setPositiveButton(R.string.selfie_settings) { _, _ ->
                        (activity as MainActivity).navigateTo(R.id.navigation_settings)
                    }.setNegativeButton(android.R.string.cancel, null).show()
            }
        }

        fun changed(value: SelfieSettings) {
            if (restoring || !settingsLoaded) return
            settings = value
            render()
            viewModel.update(value)
        }
        b.countdownChoices.addOnButtonCheckedListener { _, id, checked ->
            if (checked) changed(settings.copy(countdownSeconds = when (id) {
                R.id.delay_3 -> 3; R.id.delay_10 -> 10; else -> 5
            }))
        }
        b.holdChoices.addOnButtonCheckedListener { _, id, checked ->
            if (checked) changed(settings.copy(holdMillis = when (id) {
                R.id.hold_single -> 100; R.id.hold_half -> 500; R.id.hold_two -> 2000; else -> 1000
            }))
        }
        b.cycleChoices.addOnButtonCheckedListener { _, id, checked ->
            if (checked) changed(settings.copy(cycles = when (id) {
                R.id.cycles_one -> 1; R.id.cycles_ten -> 10; R.id.cycles_infinite -> 0; else -> 5
            }))
        }
        b.countdownSound.setOnCheckedChangeListener { _, checked -> changed(settings.copy(countdownSound = checked)) }
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = 0)
            insets
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.settings.collect { saved ->
                        if (saved == null) return@collect
                        settings = saved
                        restoring = true
                        b.countdownChoices.check(when (saved.countdownSeconds) { 3 -> R.id.delay_3; 10 -> R.id.delay_10; else -> R.id.delay_5 })
                        b.holdChoices.check(when (saved.holdMillis) { 100 -> R.id.hold_single; 500 -> R.id.hold_half; 2000 -> R.id.hold_two; else -> R.id.hold_one })
                        b.cycleChoices.check(when (saved.cycles) { 1 -> R.id.cycles_one; 10 -> R.id.cycles_ten; 0 -> R.id.cycles_infinite; else -> R.id.cycles_five })
                        b.countdownSound.isChecked = saved.countdownSound
                        restoring = false
                        settingsLoaded = true
                        render()
                    }
                }
                launch { AlphaRemoteService.selfieState.collect { render() } }
                launch { AlphaRemoteService.serviceState.collect { render() } }
            }
        }
    }

    private fun render() {
        val b = binding ?: return
        val state = AlphaRemoteService.selfieState.value
        val ready = (AlphaRemoteService.serviceState.value as? ServiceRunning)?.cameraState is CameraStateReady
        b.connectionStatus.setText(if (ready) R.string.selfie_connected else R.string.selfie_offline)
        b.options.isVisible = !state.running
        b.cameraSettings.isVisible = !state.running
        b.startStop.isEnabled = state.running || settingsLoaded
        b.startStop.setText(if (state.running) R.string.selfie_stop else R.string.selfie_start)
        b.phaseDisplay.textSize = if (state.phase == SelfiePhase.COUNTDOWN) 96f else if (state.running) 38f else 64f
        b.phaseDisplay.text = when (state.phase) {
            SelfiePhase.COUNTDOWN -> state.remainingSeconds.toString()
            SelfiePhase.FOCUSING -> getString(R.string.selfie_focusing)
            SelfiePhase.SHOOTING -> getString(R.string.selfie_shooting)
            SelfiePhase.ERROR -> if (state.disconnected) "—" else "AF ×"
            else -> getString(R.string.selfie_seconds_display, settings.countdownSeconds)
        }
        b.phaseHint.text = when (state.phase) {
            SelfiePhase.COUNTDOWN -> getString(R.string.selfie_next_shot)
            SelfiePhase.FOCUSING -> getString(R.string.selfie_focus_hint)
            SelfiePhase.SHOOTING -> getString(R.string.selfie_shoot_hint)
            SelfiePhase.ERROR -> getString(if (state.disconnected) R.string.selfie_disconnected else R.string.selfie_af_failed)
            SelfiePhase.COMPLETED -> getString(R.string.selfie_done)
            SelfiePhase.STOPPED -> getString(R.string.selfie_stopped)
            else -> getString(R.string.selfie_ready)
        }
        b.progress.text = if (state.running) {
            getString(R.string.selfie_cycle_progress, state.attempts + 1,
                if (state.targetCycles == 0) "∞" else state.targetCycles.toString())
        } else if (state.attempts > 0) {
            getString(R.string.selfie_progress, state.attempts, state.shots, state.skipped)
        } else ""
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
