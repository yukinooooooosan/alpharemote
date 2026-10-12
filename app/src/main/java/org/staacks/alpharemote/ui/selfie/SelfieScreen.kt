package org.staacks.alpharemote.ui.selfie

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.staacks.alpharemote.R
import org.staacks.alpharemote.selfie.SelfiePhase
import org.staacks.alpharemote.selfie.SelfieSettings
import org.staacks.alpharemote.selfie.SelfieShotResult

/**
 * Simple mode: a camera-viewfinder screen. Stateless — no BLE, service, timers or persistence.
 * Always dark, like a camera's own display, whatever the system theme. Timings shown here only follow the
 * published session (and the user's settings); they never drive the camera.
 */
@Composable
fun SelfieScreen(state: SelfieUiState, onAction: (SelfieUiAction) -> Unit, modifier: Modifier = Modifier) {
    val session = state.session
    val running = session.running
    Column(modifier.fillMaxSize().background(Finder.Bg).testTag("selfie_screen")
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header(state.connected, enabled = !running) { onAction(SelfieUiAction.OpenConnection) }
        if (running) {
            // While shooting nothing scrolls: the viewfinder takes the space and STOP stays pinned.
            Viewfinder(state, Modifier.fillMaxWidth().weight(1f))
            FlowBar(state)
            ResultStrip(state)
            FinderButton(stringResource(R.string.selfie_stop), stopping = true) { onAction(SelfieUiAction.StartStop) }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Viewfinder(state, Modifier.fillMaxWidth().height(250.dp))
                FlowBar(state)
                if (session.phase != SelfiePhase.IDLE) ResultStrip(state)
                Settings(state, onAction)
            }
            FinderButton(stringResource(R.string.selfie_start), enabled = state.settingsLoaded) { onAction(SelfieUiAction.StartStop) }
        }
    }
}

private object Finder {
    val Bg = Color(0xFF0D0E0F)
    val Panel = Color(0xFF151618)
    val Ink = Color(0xFFEDEDE8)
    val Sub = Color(0xFFB9B9B0)
    val Muted = Color(0xFF9A9A92)
    val Dim = Color(0xFF6E7072)
    val Line = Color(0xFF2C2E30)
    val Amber = Color(0xFFFFB020)
    val Green = Color(0xFF4ADE80)
    val Red = Color(0xFFFF3B30)
    val Fail = Color(0xFFFF4D4D)
    val Display = FontFamily(Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold), Font(R.font.barlow_condensed_bold, FontWeight.Bold))
    val Mono = FontFamily(Font(R.font.ibm_plex_mono_medium, FontWeight.Medium), Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold))
}

/** Display sizes in dp: the huge numerals keep their size regardless of the font scale, so nothing overflows. */
@Composable
private fun dpSp(value: Dp): TextUnit = with(LocalDensity.current) { value.toSp() }

private fun pad(n: Int) = if (n in 0..9) "0$n" else n.toString()

@Composable
private fun Header(connected: Boolean, enabled: Boolean, onOpenConnection: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        val name = stringResource(R.string.app_name).removeSuffix(" S")
        Text(buildAnnotatedString {
            append(name)
            withStyle(SpanStyle(color = Finder.Amber)) { append(" S") }
        }, color = Finder.Ink, fontFamily = Finder.Display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        val shape = RoundedCornerShape(50)
        Row(Modifier.heightIn(min = 40.dp).clip(shape).border(BorderStroke(1.dp, Finder.Line), shape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = stringResource(R.string.selfie_settings), onClick = onOpenConnection)
            .testTag("camera_settings").padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(8.dp).background(if (connected) Finder.Green else Finder.Dim, CircleShape))
            Text(stringResource(if (connected) R.string.finder_camera_connected else R.string.finder_camera_offline),
                Modifier.testTag("connection_status"), color = Finder.Sub, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Viewfinder(state: SelfieUiState, modifier: Modifier) {
    val session = state.session
    val settings = state.settings
    val running = session.running
    val total = if (settings.cycles == SelfieSettings.INFINITE) "∞" else settings.cycles.toString()
    val failed = session.phase == SelfiePhase.ERROR && !session.disconnected
    val bracket = when {
        session.phase == SelfiePhase.FOCUSING -> Finder.Green
        failed -> Finder.Fail
        else -> Finder.Ink.copy(alpha = .55f)
    }
    val osd = if (running) listOf(
        stringResource(R.string.finder_osd_try, session.attempts + 1, total),
        stringResource(R.string.finder_osd_skip, session.skipped),
        when (session.phase) {
            SelfiePhase.COUNTDOWN -> stringResource(R.string.finder_osd_wait)
            SelfiePhase.FOCUSING -> stringResource(R.string.finder_af)
            SelfiePhase.SHOOTING -> stringResource(R.string.finder_shot)
            else -> stringResource(R.string.finder_af_fail)
        },
        stringResource(R.string.finder_osd_shots, session.shots),
    ) else listOf(
        stringResource(R.string.finder_osd_delay, settings.countdownSeconds),
        stringResource(R.string.finder_osd_cycles, total),
        stringResource(R.string.finder_osd_hold, holdOsd(settings.holdMillis)),
        stringResource(if (settings.countdownSound) R.string.finder_osd_sound_on else R.string.finder_osd_sound_off),
    )
    BoxWithConstraints(modifier.clip(RoundedCornerShape(18.dp)).background(Finder.Panel).drawBehind {
        val inset = 12.dp.toPx()
        val len = 26.dp.toPx()
        val w = size.width
        val h = size.height
        val corners = Path().apply {
            moveTo(inset, inset + len); lineTo(inset, inset); lineTo(inset + len, inset)
            moveTo(w - inset - len, inset); lineTo(w - inset, inset); lineTo(w - inset, inset + len)
            moveTo(inset, h - inset - len); lineTo(inset, h - inset); lineTo(inset + len, h - inset)
            moveTo(w - inset - len, h - inset); lineTo(w - inset, h - inset); lineTo(w - inset, h - inset - len)
        }
        drawPath(corners, bracket, style = Stroke(3.dp.toPx(), cap = StrokeCap.Square))
    }, contentAlignment = Alignment.Center) {
        Osd(osd[0], Modifier.align(Alignment.TopStart).padding(start = 46.dp, top = 14.dp))
        Osd(osd[1], Modifier.align(Alignment.TopEnd).padding(end = 46.dp, top = 14.dp))
        Osd(osd[2], Modifier.align(Alignment.BottomStart).padding(start = 46.dp, bottom = 14.dp))
        Osd(osd[3], Modifier.align(Alignment.BottomEnd).padding(end = 46.dp, bottom = 14.dp))
        // Huge numerals shrink with the viewfinder on small screens instead of being clipped.
        val scale = (maxHeight / 460.dp).coerceIn(0.45f, 1f)
        Column(Modifier.padding(horizontal = 20.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PhaseContent(state, scale)
        }
        // A single shot lasts 100 ms and an AF failure 400 ms; these overlays keep each result readable for a moment.
        ResultFlash(session.shots, Finder.Ink, stringResource(R.string.finder_shot), Finder.Red)
        ResultFlash(session.skipped, Finder.Fail, stringResource(R.string.finder_af_fail), null)
    }
}

@Composable
private fun holdOsd(holdMillis: Int): String =
    if (holdMillis == SelfieSettings.SINGLE_SHOT_MILLIS) stringResource(R.string.finder_osd_single)
    else "${holdMillis / 1000f}".removeSuffix(".0") + "s"

@Composable
private fun Osd(text: String, modifier: Modifier) {
    Text(text, modifier, color = Finder.Muted, fontFamily = Finder.Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
}

@Composable
private fun PhaseContent(state: SelfieUiState, scale: Float) {
    val session = state.session
    val settings = state.settings
    val loop = rememberInfiniteTransition(label = "finder")
    when (session.phase) {
        SelfiePhase.IDLE -> {
            BigText(pad(settings.countdownSeconds), dpSp(120.dp * (scale / 0.55f).coerceAtMost(1f)))
            Hint(stringResource(if (state.connected) R.string.finder_idle_hint else R.string.ui_not_connected_hint))
        }
        SelfiePhase.COUNTDOWN -> {
            val tick = remember { Animatable(1f) }
            LaunchedEffect(session.remainingSeconds, session.attempts) {
                tick.snapTo(0f)
                tick.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
            }
            Text(stringResource(R.string.selfie_next_shot), color = Finder.Muted, fontFamily = Finder.Mono,
                fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 3.sp)
            BigText(pad(session.remainingSeconds), dpSp(200.dp * scale),
                color = if (session.remainingSeconds <= 3) Finder.Amber else Finder.Ink,
                modifier = Modifier.graphicsLayer { translationY = (1f - tick.value) * -12.dp.toPx(); alpha = tick.value })
            Hint(stringResource(if (session.attempts == 0) R.string.finder_hint_first else R.string.finder_hint_next))
        }
        SelfiePhase.FOCUSING -> {
            val pulse by loop.animateFloat(1.25f, 0.92f, infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "af")
            Box(Modifier.padding(bottom = 18.dp).size(110.dp * scale.coerceAtLeast(0.6f)).graphicsLayer { scaleX = pulse; scaleY = pulse }
                .border(BorderStroke(3.dp, Finder.Green), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.finder_af), color = Finder.Green, fontFamily = Finder.Mono, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
            }
            Text(stringResource(R.string.finder_focusing), Modifier.testTag("phase_display"), color = Finder.Ink,
                fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Hint(stringResource(R.string.selfie_focus_hint))
        }
        SelfiePhase.SHOOTING -> {
            val blink by loop.animateFloat(1f, 0.15f, infiniteRepeatable(tween(250), RepeatMode.Reverse), label = "rec")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(20.dp).graphicsLayer { alpha = blink }.background(Finder.Red, CircleShape))
                BigText(stringResource(R.string.finder_shot), dpSp(84.dp * scale.coerceAtLeast(0.7f)), weight = FontWeight.Bold, letterSpacing = 4.sp)
            }
            Hint(stringResource(R.string.selfie_shoot_hint))
        }
        SelfiePhase.ERROR -> if (session.disconnected) {
            BigText(stringResource(R.string.finder_offline), dpSp(72.dp * scale.coerceAtLeast(0.7f)), color = Finder.Fail, weight = FontWeight.Bold, letterSpacing = 3.sp)
            Hint(stringResource(R.string.selfie_disconnected))
        } else {
            BigText(stringResource(R.string.finder_af_fail), dpSp(88.dp * scale.coerceAtLeast(0.7f)), color = Finder.Fail, weight = FontWeight.Bold, letterSpacing = 3.sp)
            Text(stringResource(R.string.finder_af_failed), color = Finder.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Hint(stringResource(R.string.ui_af_failed_hint))
        }
        SelfiePhase.COMPLETED -> {
            Text(stringResource(R.string.finder_complete), color = Finder.Green, fontFamily = Finder.Mono,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 3.sp)
            BigText("${session.shots} / ${session.attempts}", dpSp(96.dp * scale.coerceAtLeast(0.7f)))
            val skips = if (session.skipped > 0) " · " + stringResource(R.string.finder_done_skips, session.skipped) else ""
            Hint(stringResource(R.string.finder_done, session.attempts, session.shots) + skips)
        }
        SelfiePhase.STOPPED -> {
            BigText(stringResource(R.string.finder_stop_title), dpSp(80.dp * scale.coerceAtLeast(0.7f)), weight = FontWeight.Bold, letterSpacing = 4.sp)
            Hint(stringResource(R.string.finder_stopped, session.shots))
        }
    }
}

@Composable
private fun BigText(text: String, size: TextUnit, modifier: Modifier = Modifier, color: Color = Finder.Ink,
                    weight: FontWeight = FontWeight.SemiBold, letterSpacing: TextUnit = 1.sp) {
    Text(text, modifier.testTag("phase_display"), color = color, fontFamily = Finder.Display, fontWeight = weight,
        fontSize = size, lineHeight = size, letterSpacing = letterSpacing, maxLines = 1, textAlign = TextAlign.Center)
}

@Composable
private fun Hint(text: String) {
    Text(text, Modifier.padding(top = 8.dp).testTag("phase_hint"), color = Finder.Sub, fontSize = 14.sp, textAlign = TextAlign.Center)
}

/** Shows [label] over the viewfinder for a moment whenever [count] grows (a shot taken or an attempt skipped). */
@Composable
private fun BoxScope.ResultFlash(count: Int, color: Color, label: String, dot: Color?) {
    val alpha = remember { Animatable(0f) }
    var seen by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count > seen) {
            alpha.snapTo(1f)
            alpha.animateTo(0f, tween(600, delayMillis = 500))
        }
        seen = count
    }
    if (alpha.value > 0f) {
        Box(Modifier.matchParentSize().graphicsLayer { this.alpha = alpha.value }.background(Finder.Panel).clearAndSetSemantics {},
            contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dot != null) Box(Modifier.size(20.dp).background(dot, CircleShape))
                Text(label, color = color, fontFamily = Finder.Display, fontWeight = FontWeight.Bold, fontSize = dpSp(80.dp), letterSpacing = 4.sp, maxLines = 1)
            }
        }
    }
}

/** 待機 › AF › 撮影: the active step fills over its real duration (countdown seconds, hold time). */
@Composable
private fun FlowBar(state: SelfieUiState) {
    val session = state.session
    val settings = state.settings
    val phase = session.phase
    val running = session.running
    val waitTotal = if (session.attempts == 0) settings.firstCountdown else settings.countdownSeconds
    val wait = remember { Animatable(0f) }
    LaunchedEffect(phase, session.attempts) {
        if (phase == SelfiePhase.COUNTDOWN) {
            val elapsed = (waitTotal - session.remainingSeconds).coerceIn(0, waitTotal)
            wait.snapTo(elapsed / waitTotal.toFloat())
            wait.animateTo(1f, tween((waitTotal - elapsed) * 1000, easing = LinearEasing))
        }
    }
    val shot = remember { Animatable(0f) }
    LaunchedEffect(phase, session.attempts) {
        if (phase == SelfiePhase.SHOOTING) {
            shot.snapTo(0f)
            shot.animateTo(1f, tween(settings.holdMillis, easing = LinearEasing))
        }
    }
    val glow by rememberInfiniteTransition(label = "flow").animateFloat(1f, 0.45f,
        infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "af_glow")
    val failed = phase == SelfiePhase.ERROR && !session.disconnected
    val stage = when (phase) {
        SelfiePhase.COUNTDOWN -> 0
        SelfiePhase.FOCUSING, SelfiePhase.ERROR -> 1
        SelfiePhase.SHOOTING -> 2
        else -> -1
    }
    val done = { step: Int -> (running && stage > step) || phase == SelfiePhase.COMPLETED }
    val holdLabel = stringResource(when (settings.holdMillis) {
        SelfieSettings.SINGLE_SHOT_MILLIS -> R.string.selfie_single
        500 -> R.string.selfie_hold_half
        1000 -> R.string.selfie_hold_one
        else -> R.string.selfie_hold_two
    })
    val flowLabel = stringResource(R.string.finder_flow)
    Row(Modifier.fillMaxWidth().testTag("flow").semantics(mergeDescendants = true) { contentDescription = flowLabel },
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowCell(stringResource(R.string.finder_flow_wait, waitTotal), Finder.Amber, 2f,
            active = running && stage == 0, done = done(0), progress = { wait.value })
        FlowArrow()
        FlowCell(if (failed) stringResource(R.string.finder_af_fail) else stringResource(R.string.finder_flow_af),
            if (failed) Finder.Fail else Finder.Green, 1f,
            active = running && stage == 1, done = done(1), progress = { if (failed) 1f else glow })
        FlowArrow()
        FlowCell(stringResource(R.string.finder_flow_shot, holdLabel), Finder.Red, 1.3f,
            active = running && stage == 2, done = done(2), progress = { shot.value })
    }
}

@Composable
private fun RowScope.FlowCell(label: String, color: Color, weight: Float, active: Boolean, done: Boolean, progress: () -> Float) {
    Column(Modifier.weight(weight), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = if (active) color else if (done) Finder.Ink else Finder.Dim, fontFamily = Finder.Mono,
            fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 1.sp, maxLines = 1)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Finder.Line).drawBehind {
            val fraction = when {
                active -> progress().coerceIn(0f, 1f)
                done -> 1f
                else -> 0f
            }
            val fill = if (active) color.copy(alpha = if (color == Finder.Green) progress().coerceIn(0f, 1f) else 1f)
                else Finder.Ink.copy(alpha = .55f)
            if (fraction > 0f) drawRect(fill, size = size.copy(width = size.width * (if (color == Finder.Green && active) 1f else fraction)))
        })
    }
}

@Composable
private fun FlowArrow() {
    Text("›", Modifier.padding(bottom = 1.dp), color = Finder.Dim, fontFamily = Finder.Mono, fontSize = 14.sp)
}

@Composable
private fun ResultStrip(state: SelfieUiState) {
    val session = state.session
    val target = state.settings.cycles
    val shown = state.results.takeLast(12)
    val pending = if (session.running && target != SelfieSettings.INFINITE) (target - state.results.size).coerceAtLeast(0) else 0
    Row(Modifier.fillMaxWidth().testTag("results"), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        shown.forEach { result ->
            if (result == SelfieShotResult.SHOT) {
                Box(Modifier.size(22.dp).background(Finder.Ink, RoundedCornerShape(4.dp)))
            } else {
                Box(Modifier.size(22.dp).border(BorderStroke(1.dp, Finder.Fail), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Text("×", color = Finder.Fail, fontFamily = Finder.Mono, fontSize = 13.sp)
                }
            }
        }
        repeat(pending.coerceAtMost(12)) { i ->
            Box(Modifier.size(22.dp).border(BorderStroke(1.dp, if (i == 0) Finder.Amber else Finder.Line), RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
private fun Settings(state: SelfieUiState, onAction: (SelfieUiAction) -> Unit) {
    val s = state.settings
    val enabled = state.settingsLoaded
    val change = { next: SelfieSettings -> onAction(SelfieUiAction.ChangeSettings(next)) }
    Column(Modifier.fillMaxWidth().testTag("options"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingRow(stringResource(R.string.selfie_delay), "delay", s.countdownSeconds, enabled, listOf(
            3 to stringResource(R.string.selfie_seconds_3), 5 to stringResource(R.string.selfie_seconds_5),
            10 to stringResource(R.string.selfie_seconds_10))) { change(s.copy(countdownSeconds = it)) }
        SettingRow(stringResource(R.string.selfie_capture), "hold", s.holdMillis, enabled, listOf(
            100 to stringResource(R.string.selfie_single), 500 to stringResource(R.string.selfie_hold_half),
            1000 to stringResource(R.string.selfie_hold_one), 2000 to stringResource(R.string.selfie_hold_two))) { change(s.copy(holdMillis = it)) }
        SettingRow(stringResource(R.string.selfie_cycles), "cycles", s.cycles, enabled, listOf(
            1 to stringResource(R.string.selfie_cycles_one), 5 to stringResource(R.string.selfie_cycles_five),
            10 to stringResource(R.string.selfie_cycles_ten), 0 to stringResource(R.string.selfie_infinite))) { change(s.copy(cycles = it)) }
        SettingRow(stringResource(R.string.selfie_sound), "sound", if (s.countdownSound) 1 else 0, enabled, listOf(
            1 to stringResource(R.string.finder_sound_on), 0 to stringResource(R.string.finder_sound_off))) { change(s.copy(countdownSound = it == 1)) }
        Text(stringResource(if (s.holdMillis == SelfieSettings.SINGLE_SHOT_MILLIS) R.string.ui_single_note else R.string.ui_burst_note) +
            "　" + stringResource(R.string.ui_sound_note), color = Finder.Muted, fontSize = 12.sp)
    }
}

/** One selection group with full-size targets and selected semantics for TalkBack. */
@Composable
private fun SettingRow(label: String, tag: String, selected: Int, enabled: Boolean, choices: List<Pair<Int, String>>, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.width(72.dp), color = Finder.Muted, fontSize = 13.sp)
        Row(Modifier.weight(1f).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            choices.forEach { (value, text) ->
                val checked = value == selected
                val shape = RoundedCornerShape(10.dp)
                Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(shape)
                    .background(if (checked) Finder.Ink else Color.Transparent, shape)
                    .border(BorderStroke(1.dp, if (checked) Finder.Ink else Finder.Line), shape)
                    .selectable(selected = checked, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(value) })
                    .testTag("${tag}_$value"), contentAlignment = Alignment.Center) {
                    Text(text, color = when {
                        !enabled -> Finder.Dim
                        checked -> Finder.Bg
                        else -> Color(0xFFCFCFC8)
                    }, fontFamily = Finder.Mono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FinderButton(label: String, enabled: Boolean = true, stopping: Boolean = false, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag("start_stop"), enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (stopping) Finder.Ink else Finder.Amber, contentColor = Finder.Bg,
            disabledContainerColor = Finder.Line, disabledContentColor = Finder.Dim)) {
        if (stopping) Box(Modifier.padding(end = 12.dp).size(18.dp).background(Finder.Bg, RoundedCornerShape(3.dp)))
        Text(label, fontFamily = Finder.Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 4.sp, maxLines = 1)
    }
}
