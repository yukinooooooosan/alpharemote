package org.staacks.alpharemote.ui.selfie

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.staacks.alpharemote.R
import org.staacks.alpharemote.ui.appearance.AppSkin
import org.staacks.alpharemote.selfie.SelfiePhase
import org.staacks.alpharemote.ui.components.*

/** Stateless UI. No BLE, service, timers or persistence, including interactive previews. */
@Composable
fun SelfieScreen(state: SelfieUiState, onAction: (SelfieUiAction) -> Unit, modifier: Modifier = Modifier) {
    val running = state.session.running
    Surface(modifier.fillMaxSize().testTag("selfie_screen"), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        if (LocalSelfieSkin.current == AppSkin.KAWAII) KawaiiAccent()
                    }
                    Text(stringResource(if (state.connected) R.string.selfie_connected else R.string.selfie_offline),
                        Modifier.padding(top = 4.dp).testTag("connection_status"), style = MaterialTheme.typography.labelMedium,
                        color = if (state.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!running) IconButton(onClick = { onAction(SelfieUiAction.OpenConnection) }, Modifier.testTag("camera_settings")) {
                    Icon(painterResource(R.drawable.ic_settings_black_24dp), stringResource(R.string.selfie_settings))
                }
            }
            if (running) {
                // The status may scroll with large fonts, but STOP always stays outside that scroll area.
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        PhaseDisplay(state)
                    }
                }
                SelfiePrimaryButton(stringResource(R.string.selfie_stop), { onAction(SelfieUiAction.StartStop) },
                    Modifier.padding(top = 16.dp).testTag("start_stop"), stopping = true)
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        PhaseDisplay(state)
                        SelfiePrimaryButton(stringResource(R.string.selfie_start), { onAction(SelfieUiAction.StartStop) },
                            Modifier.padding(top = 24.dp).testTag("start_stop"), enabled = state.settingsLoaded)
                    }
                    Column(Modifier.fillMaxWidth().testTag("options"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SelfieSegmentedControl(stringResource(R.string.selfie_delay), listOf(3 to stringResource(R.string.selfie_seconds_3),
                            5 to stringResource(R.string.selfie_seconds_5), 10 to stringResource(R.string.selfie_seconds_10)),
                            state.settings.countdownSeconds, state.settingsLoaded, "delay") {
                            onAction(SelfieUiAction.ChangeSettings(state.settings.copy(countdownSeconds = it)))
                        }
                        SelfieSegmentedControl(stringResource(R.string.selfie_capture), listOf(100 to stringResource(R.string.selfie_single),
                            500 to stringResource(R.string.selfie_hold_half), 1000 to stringResource(R.string.selfie_hold_one),
                            2000 to stringResource(R.string.selfie_hold_two)), state.settings.holdMillis, state.settingsLoaded, "hold") {
                            onAction(SelfieUiAction.ChangeSettings(state.settings.copy(holdMillis = it)))
                        }
                        SelfieSegmentedControl(stringResource(R.string.selfie_cycles), listOf(1 to stringResource(R.string.selfie_cycles_one),
                            5 to stringResource(R.string.selfie_cycles_five), 10 to stringResource(R.string.selfie_cycles_ten),
                            0 to stringResource(R.string.selfie_infinite)), state.settings.cycles, state.settingsLoaded, "cycles") {
                            onAction(SelfieUiAction.ChangeSettings(state.settings.copy(cycles = it)))
                        }
                        Row(Modifier.fillMaxWidth().toggleable(state.settings.countdownSound, enabled = state.settingsLoaded,
                            role = Role.Switch, onValueChange = { onAction(SelfieUiAction.ChangeSettings(state.settings.copy(countdownSound = it))) })
                            .testTag("countdown_sound").padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.selfie_sound), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(state.settings.countdownSound, onCheckedChange = null, enabled = state.settingsLoaded)
                        }
                        SelfieNote(stringResource(if (state.settings.holdMillis == 100) R.string.ui_single_note else R.string.ui_burst_note))
                        SelfieNote(stringResource(R.string.ui_sound_note))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseDisplay(state: SelfieUiState) {
    val session = state.session
    val phaseText = when (session.phase) {
        SelfiePhase.COUNTDOWN -> session.remainingSeconds.toString()
        SelfiePhase.FOCUSING -> stringResource(R.string.selfie_focusing)
        SelfiePhase.SHOOTING -> stringResource(R.string.selfie_shooting)
        SelfiePhase.ERROR -> if (session.disconnected) "—" else "AF ×"
        SelfiePhase.COMPLETED -> stringResource(R.string.ui_completed)
        SelfiePhase.STOPPED -> stringResource(R.string.ui_stopped)
        else -> stringResource(R.string.selfie_seconds_display, state.settings.countdownSeconds)
    }
    Text(phaseText, Modifier.fillMaxWidth().heightIn(min = 128.dp).wrapContentHeight().testTag("phase_display"),
        textAlign = TextAlign.Center, fontWeight = if (LocalSelfieSkin.current == AppSkin.KAWAII) FontWeight.ExtraBold else FontWeight.Light,
        fontSize = when { session.phase == SelfiePhase.COUNTDOWN -> 104.sp; session.phase == SelfiePhase.IDLE -> 64.sp; else -> 34.sp },
        color = when {
            session.phase == SelfiePhase.ERROR -> MaterialTheme.colorScheme.error
            LocalSelfieSkin.current == AppSkin.KAWAII -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onBackground
        })
    val hint = when (session.phase) {
        SelfiePhase.COUNTDOWN -> R.string.selfie_next_shot
        SelfiePhase.FOCUSING -> R.string.selfie_focus_hint
        SelfiePhase.SHOOTING -> R.string.selfie_shoot_hint
        SelfiePhase.ERROR -> if (session.disconnected) R.string.selfie_disconnected else R.string.selfie_af_failed
        SelfiePhase.COMPLETED -> R.string.selfie_done
        SelfiePhase.STOPPED -> R.string.selfie_stopped
        else -> if (state.connected) R.string.selfie_ready else R.string.ui_not_connected_hint
    }
    Text(stringResource(hint), Modifier.testTag("phase_hint"), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val progress = when {
        session.running -> stringResource(R.string.selfie_cycle_progress, session.attempts + 1,
            if (session.targetCycles == 0) "∞" else session.targetCycles.toString())
        session.attempts > 0 -> stringResource(R.string.selfie_progress, session.attempts, session.shots, session.skipped)
        else -> ""
    }
    if (progress.isNotEmpty()) Text(progress, Modifier.padding(top = 12.dp).testTag("progress"),
        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
}
