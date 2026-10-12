package org.staacks.alpharemote.ui.manual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import org.staacks.alpharemote.ui.appearance.AppSkin
import org.staacks.alpharemote.ui.selfie.SelfieTheme
import org.staacks.alpharemote.R
import org.staacks.alpharemote.ui.selfie.Kawaii
import org.staacks.alpharemote.ui.selfie.KawaiiPill
import org.staacks.alpharemote.ui.selfie.StickerText
import org.staacks.alpharemote.ui.selfie.StitchedFrame
import org.staacks.alpharemote.ui.selfie.animateFloatLoop
import org.staacks.alpharemote.ui.selfie.polkaDots
import org.staacks.alpharemote.ui.selfie.rememberBob
import org.staacks.alpharemote.ui.selfie.star

/**
 * kawaii mode manual ("あそびかた"). Explains only the fixed kawaii flow (5 single shots, 10 s then 5 s,
 * phone facing the subject). Works offline; the only state is which questions are open.
 */
@Composable
fun KawaiiManualScreen() {
    val loop = rememberInfiniteTransition(label = "kawaii_manual")
    val bob = rememberBob(loop)
    val spin = animateFloatLoop(loop, "manual_spin", 0f, 360f, 6000, RepeatMode.Restart, LinearEasing)
    Box(Modifier.fillMaxSize().testTag("manual_screen").drawBehind { polkaDots(Color(0xFFFFD3EA)) }) {
        StitchedFrame()
        Canvas(Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 30.dp).size(30.dp).clearAndSetSemantics {}) {
            rotate(spin.value) { star(center, size.width / 2, Kawaii.Yellow) }
        }
        Column(Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
            StickerText(stringResource(R.string.kawaii_manual_title), dpSp(52.dp), Kawaii.Pink, stroke = 12.dp,
                modifier = Modifier.semantics { heading() }.graphicsLayer { translationY = bob.value * 6.dp.toPx(); rotationZ = bob.value * -3f })
            KawaiiPill(stringResource(R.string.kawaii_manual_subtitle))
            FlowChips()
            Step(1, Kawaii.Pink, -6f, R.string.kawaii_manual_step1_title, R.string.kawaii_manual_step1_body, R.string.kawaii_manual_step1_note)
            Step(2, Kawaii.Purple, 4f, R.string.kawaii_manual_step2_title, R.string.kawaii_manual_step2_body, R.string.kawaii_manual_step2_note)
            Step(3, Kawaii.Teal, -3f, R.string.kawaii_manual_step3_title, R.string.kawaii_manual_step3_body, null, illustration = true)
            Step(4, Kawaii.Orange, 6f, R.string.kawaii_manual_step4_title, R.string.kawaii_manual_step4_body, R.string.kawaii_manual_step4_note)
            StickerText(stringResource(R.string.kawaii_manual_faq_title), dpSp(30.dp), Kawaii.Plum, stroke = 9.dp, shadow = null,
                modifier = Modifier.padding(top = 6.dp).semantics { heading() })
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Faq(0, R.string.kawaii_manual_q1, R.string.kawaii_manual_a1, openByDefault = true)
                Faq(1, R.string.kawaii_manual_q2, R.string.kawaii_manual_a2)
                Faq(2, R.string.kawaii_manual_q3, R.string.kawaii_manual_a3)
                Faq(3, R.string.kawaii_manual_q4, R.string.kawaii_manual_a4)
                Faq(4, R.string.kawaii_manual_q5, R.string.kawaii_manual_a5)
            }
            Text(stringResource(R.string.kawaii_manual_footer),
                Modifier.fillMaxWidth().background(Color.White.copy(alpha = .85f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                color = Kawaii.Plum, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun dpSp(value: Dp): TextUnit = with(LocalDensity.current) { value.toSp() }

@Composable
private fun FlowChips() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowChip(R.string.kawaii_manual_flow_wait, Kawaii.Teal, Color.White, -3f)
            FlowArrow()
            FlowChip(R.string.kawaii_manual_flow_pose, Kawaii.Purple, Color.White, 2f)
            FlowArrow()
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowChip(R.string.kawaii_manual_flow_count, Kawaii.Yellow, Kawaii.Plum, -2f)
            FlowArrow()
            FlowChip(R.string.kawaii_manual_flow_shot, Kawaii.Pink, Color.White, 3f)
        }
    }
}

@Composable
private fun FlowChip(text: Int, fill: Color, ink: Color, tilt: Float) {
    val shape = RoundedCornerShape(14.dp)
    Text(stringResource(text), Modifier.graphicsLayer { rotationZ = tilt }
        .background(Kawaii.Plum, shape).padding(bottom = 3.dp)
        .background(fill, shape).border(BorderStroke(3.dp, Color.White), shape)
        .padding(horizontal = 11.dp, vertical = 5.dp),
        color = ink, fontFamily = Kawaii.Pop, fontSize = 15.sp)
}

@Composable
private fun FlowArrow() {
    Text("›", Modifier.clearAndSetSemantics {}, color = Kawaii.Plum, fontFamily = Kawaii.Pop, fontSize = 18.sp)
}

@Composable
private fun Step(number: Int, color: Color, tilt: Float, title: Int, body: Int, note: Int?, illustration: Boolean = false) {
    val shape = RoundedCornerShape(28.dp)
    Box(Modifier.fillMaxWidth().padding(top = 14.dp).testTag("kawaii_step_$number")) {
        Column(Modifier.fillMaxWidth().background(Kawaii.Plum, shape).padding(bottom = 6.dp)
            .background(Color.White, shape).border(BorderStroke(5.dp, Kawaii.Plum), shape)
            .padding(start = 74.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)
            .semantics(mergeDescendants = true) {}) {
            Text(stringResource(title), Modifier.padding(bottom = 6.dp).semantics { heading() },
                color = color, fontFamily = Kawaii.Pop, fontSize = 20.sp, lineHeight = 27.sp)
            Text(stringResource(body), color = Kawaii.Plum, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 24.sp)
            if (note != null) {
                Text(stringResource(note), Modifier.padding(top = 8.dp).fillMaxWidth()
                    .background(Color(0xFFFFE9F4), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Kawaii.DeepPlum, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            if (illustration) PlacementIllustration(Modifier.padding(top = 12.dp).offset(x = (-40).dp))
        }
        Box(Modifier.offset(x = (-10).dp, y = (-14).dp).size(66.dp).graphicsLayer { rotationZ = tilt }
            .background(Kawaii.Plum, CircleShape).padding(bottom = 4.dp)
            .background(color, CircleShape).border(BorderStroke(5.dp, Color.White), CircleShape)
            .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Text(number.toString(), color = Color.White, fontFamily = Kawaii.Pop, fontSize = 30.sp)
        }
    }
}

/** Camera on a tripod, phone beside it with its screen facing the person 2–3 m away (250×120 design units). */
@Composable
private fun PlacementIllustration(modifier: Modifier) {
    val description = stringResource(R.string.kawaii_manual_illustration)
    Box(modifier.size(250.dp, 120.dp).semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxSize()) {
            scale(size.width / 250f, pivot = Offset.Zero) {
                val ink = Kawaii.Plum
                val line = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawLine(ink, Offset(36f, 64f), Offset(20f, 116f), 4f, StrokeCap.Round)
                drawLine(ink, Offset(44f, 64f), Offset(44f, 116f), 4f, StrokeCap.Round)
                drawLine(ink, Offset(52f, 64f), Offset(68f, 116f), 4f, StrokeCap.Round)
                drawRoundRect(Kawaii.Pink, Offset(26f, 20f), Size(18f, 12f), CornerRadius(4f))
                drawRoundRect(ink, Offset(26f, 20f), Size(18f, 12f), CornerRadius(4f), style = line)
                drawRoundRect(Kawaii.Pink, Offset(14f, 28f), Size(60f, 40f), CornerRadius(10f))
                drawRoundRect(ink, Offset(14f, 28f), Size(60f, 40f), CornerRadius(10f), style = line)
                drawCircle(Color.White, 12f, Offset(44f, 48f))
                drawCircle(ink, 12f, Offset(44f, 48f), style = line)
                drawRoundRect(Color.White, Offset(84f, 46f), Size(26f, 44f), CornerRadius(6f))
                drawRoundRect(ink, Offset(84f, 46f), Size(26f, 44f), CornerRadius(6f), style = line)
                drawRoundRect(Color(0xFFFFD3EA), Offset(89f, 52f), Size(16f, 28f), CornerRadius(3f))
                translate(89f, 55f) { scale(8f / 24f, pivot = Offset.Zero) { drawPath(Kawaii.Heart, Kawaii.Pink) } }
                drawLine(ink, Offset(76f, 90f), Offset(118f, 90f), 4f, StrokeCap.Round)
                drawLine(ink, Offset(126f, 70f), Offset(186f, 70f), 3f, StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                drawPath(Path().apply { moveTo(180f, 64f); lineTo(190f, 70f); lineTo(180f, 76f) }, ink,
                    style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(Path().apply { moveTo(196f, 116f); cubicTo(196f, 84f, 236f, 84f, 236f, 116f); close() }, Kawaii.Purple)
                drawPath(Path().apply { moveTo(196f, 116f); cubicTo(196f, 84f, 236f, 84f, 236f, 116f) }, ink, style = line)
                drawCircle(Color(0xFFFFE0C2), 14f, Offset(216f, 44f))
                drawCircle(ink, 14f, Offset(216f, 44f), style = line)
                drawLine(ink, Offset(232f, 68f), Offset(246f, 54f), 4f, StrokeCap.Round)
                drawPath(Path().apply { moveTo(240f, 50f); lineTo(246f, 54f); lineTo(250f, 46f) }, ink,
                    style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Text(stringResource(R.string.kawaii_manual_distance), Modifier.offset(x = 136.dp, y = 42.dp).clearAndSetSemantics {},
            color = Kawaii.Plum, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
private fun Faq(index: Int, question: Int, answer: Int, openByDefault: Boolean = false) {
    var open by rememberSaveable("kawaii_faq_$index") { mutableStateOf(openByDefault) }
    val turn by animateFloatAsState(if (open) 45f else 0f, label = "faq_plus")
    val actionLabel = stringResource(if (open) R.string.ui_collapse else R.string.ui_expand)
    val description = stringResource(if (open) R.string.ui_expanded else R.string.ui_collapsed)
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Color.White, shape).border(BorderStroke(4.dp, Kawaii.Plum), shape)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("kawaii_faq_$index")
            .semantics { stateDescription = description }
            .clickable(role = Role.Button, onClickLabel = actionLabel) { open = !open }
            .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).background(Kawaii.Pink, CircleShape).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
                Text("Q", color = Color.White, fontFamily = Kawaii.Pop, fontSize = 16.sp)
            }
            Text(stringResource(question), Modifier.weight(1f), color = Kawaii.Plum, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("+", Modifier.graphicsLayer { rotationZ = turn }.clearAndSetSemantics {}, color = Kawaii.Pink, fontFamily = Kawaii.Pop, fontSize = 22.sp)
        }
        AnimatedVisibility(open) {
            Text(stringResource(answer), Modifier.padding(start = 54.dp, end = 16.dp, bottom = 14.dp).testTag("kawaii_faq_${index}_body"),
                color = Kawaii.Plum, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 24.sp)
        }
    }
}

@Preview(name = "あそびかた", group = "取説・kawaii", widthDp = 393, heightDp = 1700, apiLevel = 36, locale = "ja")
@Composable
fun KawaiiManualPreview() {
    SelfieTheme(skin = AppSkin.KAWAII) { KawaiiManualScreen() }
}
