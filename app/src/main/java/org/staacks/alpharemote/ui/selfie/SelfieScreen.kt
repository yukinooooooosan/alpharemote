package org.staacks.alpharemote.ui.selfie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import org.staacks.alpharemote.selfie.SelfiePhase

/** Stateless UI: only renders input and emits events. No BLE, service, timers or persistence. */
@Composable
fun SelfieScreen(state: SelfieUiState, onAction: (SelfieUiAction) -> Unit, modifier: Modifier = Modifier) {
    val session = state.session
    val running = session.running
    Surface(modifier = modifier.fillMaxSize().testTag("selfie_screen")) {
        Column(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.selfie_camera_label), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(if (state.connected) R.string.selfie_connected else R.string.selfie_offline),
                    modifier = Modifier.weight(1f).padding(start = 8.dp).testTag("connection_status"),
                    textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium,
                    color = if (state.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!running) {
                    IconButton(onClick = { onAction(SelfieUiAction.OpenConnection) }, modifier = Modifier.testTag("camera_settings")) {
                        Icon(painterResource(R.drawable.ic_settings_black_24dp), contentDescription = stringResource(R.string.selfie_settings))
                    }
                }
            }
            Spacer(Modifier.height(if (running) 56.dp else 24.dp))
            Text(stringResource(R.string.selfie_title), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, letterSpacing = 3.sp)
            val phaseText = when (session.phase) {
                SelfiePhase.COUNTDOWN -> session.remainingSeconds.toString()
                SelfiePhase.FOCUSING -> stringResource(R.string.selfie_focusing)
                SelfiePhase.SHOOTING -> stringResource(R.string.selfie_shooting)
                SelfiePhase.ERROR -> if (session.disconnected) "—" else "AF ×"
                else -> stringResource(R.string.selfie_seconds_display, state.settings.countdownSeconds)
            }
            Text(
                phaseText, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 128.dp)
                    .wrapContentHeight().testTag("phase_display"),
                textAlign = TextAlign.Center, fontWeight = FontWeight.Light,
                fontSize = when { session.phase == SelfiePhase.COUNTDOWN -> 96.sp; running -> 38.sp; else -> 64.sp },
                color = if (session.phase == SelfiePhase.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            val hint = when (session.phase) {
                SelfiePhase.COUNTDOWN -> R.string.selfie_next_shot
                SelfiePhase.FOCUSING -> R.string.selfie_focus_hint
                SelfiePhase.SHOOTING -> R.string.selfie_shoot_hint
                SelfiePhase.ERROR -> if (session.disconnected) R.string.selfie_disconnected else R.string.selfie_af_failed
                SelfiePhase.COMPLETED -> R.string.selfie_done
                SelfiePhase.STOPPED -> R.string.selfie_stopped
                else -> R.string.selfie_ready
            }
            Text(stringResource(hint), modifier = Modifier.testTag("phase_hint"), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val progress = when {
                running -> stringResource(R.string.selfie_cycle_progress, session.attempts + 1,
                    if (session.targetCycles == 0) "∞" else session.targetCycles.toString())
                session.attempts > 0 -> stringResource(R.string.selfie_progress, session.attempts, session.shots, session.skipped)
                else -> ""
            }
            if (progress.isNotEmpty()) {
                Text(progress, modifier = Modifier.padding(top = 8.dp).testTag("progress"), textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onAction(SelfieUiAction.StartStop) }, enabled = running || state.settingsLoaded,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 88.dp).testTag("start_stop"),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text(stringResource(if (running) R.string.selfie_stop else R.string.selfie_start), fontSize = 24.sp, letterSpacing = 2.sp)
            }
            if (!running) {
                Column(Modifier.fillMaxWidth().padding(top = 10.dp).testTag("options")) {
                    ChoiceRow(R.string.selfie_delay, listOf(3 to R.string.selfie_seconds_3, 5 to R.string.selfie_seconds_5, 10 to R.string.selfie_seconds_10),
                        state.settings.countdownSeconds, state.settingsLoaded, "delay") {
                        onAction(SelfieUiAction.ChangeSettings(state.settings.copy(countdownSeconds = it)))
                    }
                    ChoiceRow(R.string.selfie_capture, listOf(100 to R.string.selfie_single, 500 to R.string.selfie_hold_half, 1000 to R.string.selfie_hold_one, 2000 to R.string.selfie_hold_two),
                        state.settings.holdMillis, state.settingsLoaded, "hold") {
                        onAction(SelfieUiAction.ChangeSettings(state.settings.copy(holdMillis = it)))
                    }
                    ChoiceRow(R.string.selfie_cycles, listOf(1 to R.string.selfie_cycles_one, 5 to R.string.selfie_cycles_five, 10 to R.string.selfie_cycles_ten, 0 to R.string.selfie_infinite),
                        state.settings.cycles, state.settingsLoaded, "cycles") {
                        onAction(SelfieUiAction.ChangeSettings(state.settings.copy(cycles = it)))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            .toggleable(value = state.settings.countdownSound, enabled = state.settingsLoaded, role = Role.Switch,
                                onValueChange = { onAction(SelfieUiAction.ChangeSettings(state.settings.copy(countdownSound = it))) })
                            .testTag("countdown_sound").padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.selfie_sound), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = state.settings.countdownSound, onCheckedChange = null, enabled = state.settingsLoaded)
                    }
                    Text(stringResource(R.string.selfie_drive_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.selfie_sound_hint), modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: Int, choices: List<Pair<Int, Int>>, selected: Int, enabled: Boolean, tag: String, onSelected: (Int) -> Unit) {
    Text(stringResource(label), modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.forEach { (value, text) ->
            FilterChip(selected = selected == value, onClick = { onSelected(value) }, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("${tag}_$value"),
                label = { Text(stringResource(text), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) })
        }
    }
}
