package org.staacks.alpharemote.ui.selfie

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.staacks.alpharemote.R
import org.staacks.alpharemote.selfie.SelfiePhase
import org.staacks.alpharemote.selfie.SelfieSettings
import org.staacks.alpharemote.selfie.SelfieShotResult
import org.staacks.alpharemote.selfie.SelfieShowState
import kotlin.math.PI
import kotlin.math.sin

/**
 * kawaii mode: a purikura-style show. The phone stands beside the camera facing the subject 2–3 m away.
 * Stateless like [SelfieScreen]: no BLE, service, timers or persistence. Camera timing never depends on it.
 * Always bright and loud by design, in light and dark system themes alike.
 */
@Composable
fun KawaiiSelfieScreen(state: SelfieUiState, show: SelfieShowState, onAction: (SelfieUiAction) -> Unit,
                       modifier: Modifier = Modifier) {
    val session = state.session
    val running = session.running
    val scene = kawaiiScene(state, show)
    val loop = rememberInfiniteTransition(label = "kawaii")
    val background by animateColorAsState(sceneColor(scene, session.remainingSeconds + session.attempts),
        tween(350), label = "kawaii_background")

    Box(modifier.fillMaxSize().testTag("selfie_screen").drawBehind { polkaDots(background) }) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            StitchedFrame()
            Sparkles(loop)
            Ribbon(loop, Modifier.align(Alignment.TopCenter).padding(top = 22.dp))
            Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 76.dp, bottom = 20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    CameraPill(state.connected, enabled = !running) { onAction(SelfieUiAction.OpenConnection) }
                    if (running) CounterSticker(session.attempts + 1, session.targetCycles)
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // Each scene restarts its entry animation; CELEBRATE spans SHOOTING and the first countdown second.
                    key(scene) { SceneContent(scene, state, show, loop) }
                }
                if (running || scene == KawaiiScene.DONE) Hearts(show.results, session.targetCycles, session.attempts, running, loop)
                if (running) {
                    KawaiiStopButton { onAction(SelfieUiAction.StartStop) }
                } else {
                    KawaiiStartButton(stringResource(if (scene == KawaiiScene.IDLE) R.string.kawaii_start else R.string.kawaii_again), loop) {
                        onAction(SelfieUiAction.StartStop)
                    }
                }
            }
        }
        if (scene == KawaiiScene.DONE) Confetti(loop)
        // One flash per shot: SHOOTING and the following countdown second share the same round number.
        val round = session.attempts + if (session.phase == SelfiePhase.SHOOTING) 1 else 0
        if (scene == KawaiiScene.CELEBRATE) key(round) { Flash() }
    }
}

internal enum class KawaiiScene { IDLE, COUNTDOWN, FOCUS, CELEBRATE, FAIL, DONE, STOPPED, DISCONNECTED }

internal fun kawaiiScene(state: SelfieUiState, show: SelfieShowState): KawaiiScene {
    val session = state.session
    val firstSecondAfterCycle = session.attempts > 0 && session.remainingSeconds == SelfieSettings.KAWAII.countdownSeconds
    return when (session.phase) {
        SelfiePhase.IDLE -> KawaiiScene.IDLE
        SelfiePhase.COUNTDOWN -> when {
            firstSecondAfterCycle && show.lastResult == SelfieShotResult.SHOT -> KawaiiScene.CELEBRATE
            firstSecondAfterCycle && show.lastResult == SelfieShotResult.SKIPPED -> KawaiiScene.FAIL
            else -> KawaiiScene.COUNTDOWN
        }
        SelfiePhase.FOCUSING -> KawaiiScene.FOCUS
        SelfiePhase.SHOOTING -> KawaiiScene.CELEBRATE
        SelfiePhase.ERROR -> if (session.disconnected) KawaiiScene.DISCONNECTED else KawaiiScene.FAIL
        SelfiePhase.COMPLETED -> KawaiiScene.DONE
        SelfiePhase.STOPPED -> KawaiiScene.STOPPED
    }
}

private object Kawaii {
    val Pink = Color(0xFFFF2E93)
    val Plum = Color(0xFF3B1030)
    val DeepPlum = Color(0xFF7A1F5C)
    val Yellow = Color(0xFFFFE14D)
    val Sky = Color(0xFF7CC8F5)
    val Purple = Color(0xFF8B5CF6)
    val Lilac = Color(0xFF6B4A8E)
    val Teal = Color(0xFF0E9F8E)
    val Green = Color(0xFF22B573)
    val Missed = Color(0xFFB9AACB)
    val Countdown = listOf(Color(0xFFFFD3EA), Color(0xFFFFF1A8), Color(0xFFC8F5E1), Color(0xFFE3D6FF), Color(0xFFCBE9FF), Color(0xFFFFE0C2))
    val Pop = FontFamily(Font(R.font.mochiy_pop_one))
    val Heart: Path = PathParser().parsePathString(
        "M12 21C12 21 2 15 2 8.5 2 5.4 4.4 3 7.3 3c1.9 0 3.6 1 4.7 2.6C13.1 4 14.8 3 16.7 3 19.6 3 22 5.4 22 8.5 22 15 12 21 12 21z").toPath()
    val Drop: Path = PathParser().parsePathString("M12 2C12 2 3 14 3 20a9 9 0 0 0 18 0C21 14 12 2 12 2z").toPath()
}

private fun sceneColor(scene: KawaiiScene, tick: Int): Color = when (scene) {
    KawaiiScene.IDLE -> Color(0xFFFFD3EA)
    KawaiiScene.COUNTDOWN -> Kawaii.Countdown[tick.mod(Kawaii.Countdown.size)]
    KawaiiScene.FOCUS -> Color(0xFFCBE9FF)
    KawaiiScene.CELEBRATE -> Color(0xFFFFF6B8)
    KawaiiScene.FAIL -> Color(0xFFE2DCEC)
    KawaiiScene.DONE -> Color(0xFFFFE0F0)
    KawaiiScene.STOPPED, KawaiiScene.DISCONNECTED -> Color(0xFFEFE6EE)
}

/** Display sizes in dp, so 2–3 m legibility holds and huge text never overflows with large font scales. */
@Composable
private fun dpSp(value: Dp): TextUnit = with(LocalDensity.current) { value.toSp() }

@Composable
private fun InfiniteTransition.wave(label: String, from: Float, to: Float, millis: Int): State<Float> =
    animateFloatLoop(this, label, from, to, millis, RepeatMode.Reverse, FastOutSlowInEasing)

@Composable
private fun animateFloatLoop(loop: InfiniteTransition, label: String, from: Float, to: Float, millis: Int,
                             mode: RepeatMode, easing: Easing): State<Float> =
    loop.animateFloat(from, to, infiniteRepeatable(tween(millis, easing = easing), mode), label = label)

@Composable
private fun SceneContent(scene: KawaiiScene, state: SelfieUiState, show: SelfieShowState, loop: InfiniteTransition) {
    when (scene) {
        KawaiiScene.IDLE -> IdleScene(state.connected, loop)
        KawaiiScene.COUNTDOWN -> CountdownScene(state, show, loop)
        KawaiiScene.FOCUS -> FocusScene(loop)
        KawaiiScene.CELEBRATE -> CelebrateScene(state, show, loop)
        KawaiiScene.FAIL -> FailScene(loop)
        KawaiiScene.DONE -> DoneScene(state, loop)
        KawaiiScene.STOPPED -> MessageScene(stringResource(R.string.kawaii_stopped), stringResource(R.string.kawaii_stopped_sub), Kawaii.Plum)
        KawaiiScene.DISCONNECTED -> MessageScene(stringResource(R.string.kawaii_disconnected), stringResource(R.string.selfie_disconnected), Kawaii.Lilac)
    }
}

@Composable
private fun IdleScene(connected: Boolean, loop: InfiniteTransition) {
    val bob = rememberBob(loop)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StickerText(stringResource(R.string.kawaii_title), dpSp(58.dp), Kawaii.Pink, tag = "kawaii_headline", stroke = 13.dp,
            modifier = Modifier.graphicsLayer { translationY = bob.value * 8.dp.toPx(); rotationZ = bob.value * -4f })
        Pill(stringResource(if (connected) R.string.kawaii_place_phone else R.string.kawaii_connect_first))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip(stringResource(R.string.kawaii_chip_count), Kawaii.Pink, -4f)
            Chip(stringResource(R.string.kawaii_chip_single), Kawaii.Purple, 3f)
        }
        Chip(stringResource(R.string.kawaii_chip_first), Kawaii.Teal, -2f)
        Row(Modifier.padding(top = 4.dp).graphicsLayer { rotationZ = 1.5f }
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(BorderStroke(3.dp, Kawaii.Plum), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(28.dp).background(Kawaii.Green, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(16.dp)) {
                    val p = Path().apply { moveTo(size.width * .1f, size.height * .55f); lineTo(size.width * .4f, size.height * .85f); lineTo(size.width * .92f, size.height * .2f) }
                    drawPath(p, Color.White, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Column {
                Text(stringResource(R.string.kawaii_check_single), color = Kawaii.Plum, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(stringResource(R.string.kawaii_check_single_sub), color = Kawaii.Plum, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CountdownScene(state: SelfieUiState, show: SelfieShowState, loop: InfiniteTransition) {
    val session = state.session
    val first = session.attempts == 0
    val poses = stringArrayResource(R.array.kawaii_poses)
    val pose = poses.getOrNull(show.poseId - 1).orEmpty()
    val bob = rememberBob(loop)
    val pop = remember { Animatable(1f) }
    LaunchedEffect(session.remainingSeconds, session.attempts) {
        pop.snapTo(0f)
        pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 380f))
    }
    val sign = if ((session.remainingSeconds + session.attempts) % 2 == 0) 1f else -1f
    val digits = session.remainingSeconds.toString()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (pose.isNotEmpty()) {
            StickerCard(Modifier.graphicsLayer { translationY = bob.value * 6.dp.toPx(); rotationZ = bob.value * -2f }) {
                Text(stringResource(if (first) R.string.kawaii_pose_lead_first else R.string.kawaii_pose_lead),
                    color = Kawaii.Plum, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(pose, Modifier.testTag("kawaii_pose"), color = Kawaii.Pink, fontFamily = Kawaii.Pop,
                    fontSize = dpSp(32.dp), textAlign = TextAlign.Center, lineHeight = dpSp(38.dp))
            }
        }
        StickerText(digits, dpSp(if (digits.length > 1) 170.dp else 240.dp), Kawaii.Pink, tag = "kawaii_headline",
            stroke = 16.dp, lineHeight = 1f,
            modifier = Modifier.graphicsLayer {
                val v = pop.value
                val s = 2.4f - 1.4f * v
                scaleX = s; scaleY = s
                rotationZ = (1f - v) * 16f * sign
                alpha = v.coerceIn(0f, 1f)
            })
        Pill(stringResource(if (first) R.string.kawaii_caption_first else R.string.kawaii_caption))
    }
}

@Composable
private fun FocusScene(loop: InfiniteTransition) {
    val bracket = loop.wave("bracket", 1.15f, 0.9f, 400)
    val pulse = loop.wave("focus_pulse", 1f, 1.12f, 250)
    val shake = animateFloatLoop(loop, "shake", 0f, 1f, 1200, RepeatMode.Restart, LinearEasing)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = bracket.value; scaleY = bracket.value }) {
                val a = size.width * 0.08f
                val l = size.width * 0.3f
                val b = size.width - a
                val corners = Path().apply {
                    moveTo(a, a + l); lineTo(a, a); lineTo(a + l, a)
                    moveTo(b - l, a); lineTo(b, a); lineTo(b, a + l)
                    moveTo(b, b - l); lineTo(b, b); lineTo(b - l, b)
                    moveTo(a + l, b); lineTo(a, b); lineTo(a, b - l)
                }
                drawPath(corners, Kawaii.Plum, style = Stroke(13.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(corners, Color.White, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            HeartIcon(84.dp, Kawaii.Pink, Color.White, Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value })
        }
        StickerText(stringResource(R.string.kawaii_focusing), dpSp(42.dp), Kawaii.Plum, tag = "kawaii_headline", stroke = 11.dp, shadow = null)
        StickerText(stringResource(R.string.kawaii_hold_still), dpSp(28.dp), Kawaii.Pink, stroke = 8.dp, shadow = null,
            modifier = Modifier.graphicsLayer { translationX = sin(shake.value * 2f * PI.toFloat() * 3f) * 8.dp.toPx() })
    }
}

@Composable
private fun CelebrateScene(state: SelfieUiState, show: SelfieShowState, loop: InfiniteTransition) {
    val praises = stringArrayResource(R.array.kawaii_praises)
    // During SHOOTING the praise for this shot is not picked yet; it appears with the next countdown.
    val praise = if (state.session.phase == SelfiePhase.COUNTDOWN) praises.getOrNull(show.praiseId - 1).orEmpty() else ""
    val stamp = remember { Animatable(0f) }
    LaunchedEffect(Unit) { stamp.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 420f)) }
    val bob = rememberBob(loop)
    val twinkle = loop.wave("burst", 0.55f, 1.2f, 300)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(320.dp, 220.dp)) {
                val spots = listOf(.06f to .12f, .92f to .1f, .1f to .86f, .95f to .8f, .38f to .02f, .62f to .97f, .22f to .5f, .82f to .5f)
                spots.forEachIndexed { i, (x, y) ->
                    val r = (10 + (i % 3) * 5).dp.toPx() * (if (i % 2 == 0) twinkle.value else 1.75f - twinkle.value)
                    star(Offset(size.width * x, size.height * y), r, if (i % 2 == 0) Kawaii.Yellow else Kawaii.Pink)
                }
            }
            Row(Modifier.graphicsLayer {
                    val v = stamp.value
                    val s = 3.2f - 2.2f * v
                    scaleX = s; scaleY = s
                    rotationZ = -34f + 22f * v
                    alpha = v.coerceIn(0f, 1f)
                }
                .background(Kawaii.DeepPlum, RoundedCornerShape(30.dp))
                .padding(bottom = 8.dp)
                .background(Color.White, RoundedCornerShape(30.dp))
                .border(BorderStroke(7.dp, Kawaii.Pink), RoundedCornerShape(30.dp))
                .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.kawaii_shutter), Modifier.testTag("kawaii_headline"), color = Kawaii.Pink,
                    fontFamily = Kawaii.Pop, fontSize = dpSp(50.dp), lineHeight = dpSp(56.dp), maxLines = 1, softWrap = false)
                HeartIcon(46.dp, Kawaii.Pink, null)
            }
        }
        if (praise.isNotEmpty()) {
            StickerText(praise, dpSp(38.dp), Kawaii.Plum, tag = "kawaii_praise", stroke = 10.dp, shadow = null,
                modifier = Modifier.graphicsLayer { translationY = bob.value * 10.dp.toPx(); rotationZ = bob.value * 3f })
        }
    }
}

@Composable
private fun FailScene(loop: InfiniteTransition) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shake.animateTo(1f, tween(900)) }
    val drip = animateFloatLoop(loop, "drip", 0f, 1f, 1400, RepeatMode.Restart, FastOutSlowInEasing)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box {
            StickerText(stringResource(R.string.kawaii_fail), dpSp(84.dp), Kawaii.Lilac, tag = "kawaii_headline", stroke = 13.dp, shadow = Kawaii.Plum,
                modifier = Modifier.graphicsLayer {
                    val v = shake.value
                    translationX = sin(v * 2f * PI.toFloat() * 3f) * (1f - v) * 14.dp.toPx()
                })
            Canvas(Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = (-8).dp).size(30.dp, 40.dp)
                .graphicsLayer { translationY = drip.value * 18.dp.toPx(); alpha = 1f - drip.value }) {
                scale(size.width / 24f, size.height / 32f, pivot = Offset.Zero) {
                    drawPath(Kawaii.Drop, Kawaii.Sky)
                    drawPath(Kawaii.Drop, Color.White, style = Stroke(2f))
                }
            }
        }
        Pill(stringResource(R.string.kawaii_fail_sub))
        Text(stringResource(R.string.kawaii_fail_skip),
            Modifier.graphicsLayer { rotationZ = -3f }
                .background(Kawaii.Plum, RoundedCornerShape(18.dp)).padding(bottom = 5.dp)
                .background(Kawaii.Lilac, RoundedCornerShape(18.dp))
                .border(BorderStroke(4.dp, Color.White), RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            color = Color.White, fontFamily = Kawaii.Pop, fontSize = 22.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DoneScene(state: SelfieUiState, loop: InfiniteTransition) {
    val session = state.session
    val pulse = loop.wave("done_pulse", 1f, 1.06f, 800)
    val total = if (session.targetCycles == SelfieSettings.INFINITE) session.attempts else session.targetCycles
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StickerText(stringResource(R.string.kawaii_done), dpSp(54.dp), Kawaii.Pink, tag = "kawaii_headline", stroke = 13.dp,
            modifier = Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value })
        StickerCard(Modifier.graphicsLayer { rotationZ = -2f }) {
            Text(stringResource(R.string.kawaii_done_result, total, session.shots), Modifier.testTag("kawaii_result"),
                color = Kawaii.Pink, fontFamily = Kawaii.Pop, fontSize = 26.sp, lineHeight = 34.sp, textAlign = TextAlign.Center)
            if (session.skipped > 0) {
                Text(stringResource(R.string.kawaii_done_skips, session.skipped), color = Kawaii.Plum,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun MessageScene(title: String, body: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        StickerText(title, dpSp(52.dp), color, tag = "kawaii_headline", stroke = 12.dp, shadow = null)
        Pill(body)
    }
}

@Composable
private fun rememberBob(loop: InfiniteTransition): State<Float> = loop.wave("bob", -1f, 1f, 1200)

/** Purikura sticker lettering: a white outline with a drop shadow behind the fill. */
@Composable
private fun StickerText(text: String, size: TextUnit, color: Color, modifier: Modifier = Modifier, tag: String? = null,
                        stroke: Dp = 10.dp, shadow: Color? = Kawaii.DeepPlum, lineHeight: Float = 1.1f) {
    val strokePx = with(LocalDensity.current) { stroke.toPx() }
    val style = TextStyle(fontFamily = Kawaii.Pop, fontSize = size, lineHeight = size * lineHeight, textAlign = TextAlign.Center)
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(text, Modifier.clearAndSetSemantics {}, style = style.copy(color = Color.White,
            drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round),
            shadow = shadow?.let { Shadow(it, Offset(0f, strokePx * 0.55f), 0f) }))
        Text(text, if (tag != null) Modifier.testTag(tag) else Modifier, style = style.copy(color = color))
    }
}

@Composable
private fun StickerCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(modifier.background(Kawaii.Plum, shape).padding(bottom = 6.dp)
        .background(Color.White, shape).border(BorderStroke(5.dp, Kawaii.Plum), shape)
        .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, content = content)
}

@Composable
private fun Pill(text: String) {
    Text(text, Modifier.background(Color.White.copy(alpha = .88f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 7.dp),
        color = Kawaii.Plum, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Center)
}

@Composable
private fun Chip(text: String, color: Color, tilt: Float) {
    val shape = RoundedCornerShape(18.dp)
    Text(text, Modifier.graphicsLayer { rotationZ = tilt }
        .background(Kawaii.Plum, shape).padding(bottom = 4.dp)
        .background(color, shape).border(BorderStroke(4.dp, Color.White), shape)
        .padding(horizontal = 14.dp, vertical = 8.dp),
        color = Color.White, fontFamily = Kawaii.Pop, fontSize = 17.sp)
}

@Composable
private fun CameraPill(connected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(Modifier.heightIn(min = 44.dp).clip(shape).background(Color.White, shape)
        .border(BorderStroke(3.dp, Kawaii.Plum), shape)
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = stringResource(R.string.kawaii_open_connection), onClick = onClick)
        .testTag("camera_settings").padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(if (connected) Kawaii.Green else Kawaii.Missed, CircleShape))
        Text(stringResource(if (connected) R.string.kawaii_camera_ok else R.string.kawaii_camera_missing),
            Modifier.testTag("connection_status"), color = Kawaii.Plum, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

@Composable
private fun CounterSticker(round: Int, target: Int) {
    val shape = RoundedCornerShape(16.dp)
    val text = if (target == SelfieSettings.INFINITE) stringResource(R.string.kawaii_counter_infinite, round)
        else stringResource(R.string.kawaii_counter, round, target)
    Text(text, Modifier.testTag("progress").graphicsLayer { rotationZ = 4f }
        .background(Kawaii.DeepPlum, shape).padding(bottom = 4.dp)
        .background(Kawaii.Pink, shape).border(BorderStroke(3.dp, Color.White), shape)
        .padding(horizontal = 14.dp, vertical = 6.dp),
        color = Color.White, fontFamily = Kawaii.Pop, fontSize = 17.sp)
}

@Composable
private fun Hearts(results: List<SelfieShotResult>, target: Int, attempts: Int, running: Boolean, loop: InfiniteTransition) {
    if (target == SelfieSettings.INFINITE) return
    val pulse = loop.wave("heart_pulse", 1f, 1.18f, 300)
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        repeat(target) { i ->
            val fill = when {
                i < results.size -> if (results[i] == SelfieShotResult.SHOT) Kawaii.Pink else Kawaii.Missed
                i < attempts -> Kawaii.Pink
                else -> Color.White
            }
            val current = running && i == attempts
            HeartIcon(if (target > 5) 26.dp else 34.dp, fill, Kawaii.Plum,
                if (current) Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value } else Modifier)
        }
    }
}

@Composable
private fun HeartIcon(size: Dp, fill: Color, outline: Color?, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, pivot = Offset.Zero) {
            drawPath(Kawaii.Heart, fill)
            if (outline != null) drawPath(Kawaii.Heart, outline, style = Stroke(1.8f))
        }
    }
}

@Composable
private fun KawaiiStartButton(label: String, loop: InfiniteTransition, onClick: () -> Unit) {
    val pulse = loop.wave("start_pulse", 1f, 1.05f, 700)
    val shape = RoundedCornerShape(48.dp)
    Box(Modifier.fillMaxWidth().graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
        .background(Kawaii.DeepPlum, shape).padding(bottom = 8.dp)) {
        Button(onClick, Modifier.fillMaxWidth().heightIn(min = 84.dp).testTag("start_stop"), shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = Kawaii.Pink, contentColor = Color.White),
            border = BorderStroke(6.dp, Color.White)) {
            HeartIcon(26.dp, Color.White, null)
            Text(label, Modifier.padding(horizontal = 10.dp), fontFamily = Kawaii.Pop, fontSize = 32.sp, maxLines = 1)
            HeartIcon(26.dp, Color.White, null)
        }
    }
}

/** STOP is the one control while shooting: plum on white, the strongest contrast on every background. */
@Composable
private fun KawaiiStopButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(40.dp)
    Box(Modifier.fillMaxWidth().background(Kawaii.Plum, shape).padding(bottom = 6.dp)) {
        Button(onClick, Modifier.fillMaxWidth().heightIn(min = 76.dp).testTag("start_stop"), shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Kawaii.Plum),
            border = BorderStroke(5.dp, Kawaii.Plum)) {
            Box(Modifier.size(22.dp).background(Kawaii.Plum, RoundedCornerShape(5.dp)))
            Text(stringResource(R.string.kawaii_stop), Modifier.padding(start = 12.dp), fontFamily = Kawaii.Pop, fontSize = 28.sp, maxLines = 1)
        }
    }
}

@Composable
private fun Ribbon(loop: InfiniteTransition, modifier: Modifier = Modifier) {
    val shift = animateFloatLoop(loop, "marquee", 0f, 1f, 9000, RepeatMode.Restart, LinearEasing)
    var half by remember { mutableIntStateOf(0) }
    val text = stringResource(R.string.kawaii_ribbon)
    Box(modifier.fillMaxWidth().height(34.dp).graphicsLayer { rotationZ = -3f; scaleX = 1.1f }
        .background(Kawaii.Plum).clearAndSetSemantics {}, contentAlignment = Alignment.CenterStart) {
        Row(Modifier.wrapContentWidth(Alignment.Start, unbounded = true).onSizeChanged { half = it.width / 2 }
            .graphicsLayer { translationX = -shift.value * half }) {
            repeat(2) { Text(text, color = Color.White, fontFamily = Kawaii.Pop, fontSize = 15.sp, maxLines = 1, softWrap = false) }
        }
    }
}

@Composable
private fun StitchedFrame() {
    Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
        val inset = 12.dp.toPx()
        drawRoundRect(Color.White, topLeft = Offset(inset, inset), size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius(34.dp.toPx()),
            style = Stroke(4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 10.dp.toPx()))))
    }
}

@Composable
private fun Sparkles(loop: InfiniteTransition) {
    val spin = animateFloatLoop(loop, "spin", 0f, 360f, 6000, RepeatMode.Restart, LinearEasing)
    val twinkle = loop.wave("twinkle", 0.55f, 1.15f, 800)
    Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
        val spots = listOf(Triple(.08f, .12f, 13), Triple(.9f, .16f, 17), Triple(.06f, .58f, 15), Triple(.94f, .52f, 11),
            Triple(.12f, .74f, 9), Triple(.88f, .72f, 14))
        spots.forEachIndexed { i, (x, y, r) ->
            val center = Offset(size.width * x, size.height * y)
            val radius = r.dp.toPx() * (if (i % 2 == 0) twinkle.value else 1f)
            val color = when (i % 3) { 0 -> Color.White; 1 -> Kawaii.Yellow; else -> Kawaii.Sky }
            rotate(if (i % 2 == 1) spin.value else 0f, pivot = center) { star(center, radius, color) }
        }
    }
}

@Composable
private fun Confetti(loop: InfiniteTransition) {
    val fall = animateFloatLoop(loop, "confetti", 0f, 1f, 3200, RepeatMode.Restart, LinearEasing)
    val colors = listOf(Kawaii.Pink, Kawaii.Yellow, Kawaii.Sky, Kawaii.Purple, Kawaii.Green, Color.White)
    Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
        repeat(26) { i ->
            val speed = 0.7f + (i % 5) * 0.15f
            val phase = (i * 0.137f) % 1f
            val progress = (fall.value * speed + phase) % 1f
            val x = size.width * ((i * 37) % 100) / 100f
            val y = progress * (size.height + 80.dp.toPx()) - 60.dp.toPx()
            val w = (8 + (i % 3) * 4).dp.toPx()
            val h = (12 + (i % 4) * 4).dp.toPx()
            rotate(progress * 760f, pivot = Offset(x, y)) {
                drawRoundRect(colors[i % colors.size], Offset(x - w / 2, y - h / 2), Size(w, h), CornerRadius(3.dp.toPx()))
            }
        }
    }
}

@Composable
private fun Flash() {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) { alpha.animateTo(0f, tween(700)) }
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value }.background(Color.White).clearAndSetSemantics {})
}

private fun DrawScope.polkaDots(color: Color) {
    drawRect(color)
    val step = 40.dp.toPx()
    val radius = step * 0.17f
    val dot = Color.White.copy(alpha = .6f)
    var row = 0
    var y = 0f
    while (y < size.height + step) {
        var x = if (row % 2 == 0) 0f else step / 2
        while (x < size.width + step) {
            drawCircle(dot, radius, Offset(x, y))
            x += step
        }
        y += step / 2
        row++
    }
}

private fun DrawScope.star(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
    drawPath(path, Color.White, style = Stroke(1.2.dp.toPx()))
}
