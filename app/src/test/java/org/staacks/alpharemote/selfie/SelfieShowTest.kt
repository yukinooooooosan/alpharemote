package org.staacks.alpharemote.selfie

import org.junit.Assert.*
import org.junit.Test
import org.staacks.alpharemote.camera.ButtonCode
import org.staacks.alpharemote.camera.CAButton
import org.staacks.alpharemote.camera.CAJog
import org.staacks.alpharemote.camera.CameraActionStep
import org.staacks.alpharemote.camera.JogCode
import kotlin.random.Random

/** The show only observes published states: it must follow the controller's timing, never drive it. */
class SelfieShowTest {
    private class Rig(seed: Int = 7) : SelfieController.Port {
        data class Task(val due: Long, val action: () -> Unit, var cancelled: Boolean = false)
        var now = 0L
        var online = true
        val tasks = mutableListOf<Task>()
        val commands = mutableListOf<Pair<Long, CameraActionStep>>()
        val cues = mutableListOf<Pair<Long, SelfieCue>>()
        val show = SelfieShow(Random(seed)) { cues += now to it }
        var showState = SelfieShowState()
        val controller = SelfieController(this)
        override fun nowMillis() = now
        override fun connected() = online
        override fun schedule(delayMillis: Long, callback: () -> Unit): SelfieController.Cancellation {
            val task = Task(now + delayMillis, callback)
            tasks += task
            return SelfieController.Cancellation { task.cancelled = true }
        }
        override fun send(step: CameraActionStep) { commands += now to step }
        override fun releaseAll() {
            ButtonCode.entries.forEach { send(CAButton(false, it)) }
            JogCode.entries.forEach { send(CAJog(false, -1, it)) }
        }
        override fun awake(enabled: Boolean) {}
        override fun sound(sound: SelfieSound) {}
        override fun publish(state: SelfieState) { showState = show.onState(state) }
        fun start(options: SelfieSettings): Boolean {
            show.prepare(options)
            return controller.start(options)
        }
        fun advance(millis: Long) {
            val end = now + millis
            while (true) {
                val task = tasks.filter { !it.cancelled && it.due <= end }.minByOrNull { it.due } ?: break
                tasks.remove(task)
                now = task.due
                task.action()
            }
            tasks.removeAll { it.cancelled }
            now = end
        }
        fun advanceUntil(phase: SelfiePhase) {
            repeat(1000) {
                if (controller.state.phase == phase) return
                advance(100)
            }
            fail("never reached $phase")
        }
        fun cycle(shoot: Boolean) {
            advanceUntil(SelfiePhase.FOCUSING)
            if (shoot) {
                controller.onFocusReport(true, now)
                advance(SelfieSettings.SINGLE_SHOT_MILLIS.toLong())
            } else {
                advance(SelfieSettings.AF_TIMEOUT_MILLIS + 400)
            }
        }
        fun halfPressTimes() = commands.filter { (_, step) ->
            step is CAButton && step.pressed && step.button == ButtonCode.SHUTTER_HALF }.map { it.first }
    }

    @Test fun kawaiiFirstCycleWaitsTenSecondsAndCuesFollowTheTimeline() {
        val rig = Rig()
        assertTrue(rig.start(SelfieSettings.KAWAII))
        assertEquals(10, rig.controller.state.remainingSeconds)
        assertTrue(rig.showState.active)
        val firstPose = rig.showState.poseId
        assertTrue(firstPose in 1..SelfieShow.POSE_COUNT)
        rig.advance(10000)
        assertEquals(SelfiePhase.FOCUSING, rig.controller.state.phase)
        assertEquals(listOf(10000L), rig.halfPressTimes())
        assertEquals(listOf(0L to SelfieCue.Intro, 3000L to SelfieCue.PoseLeadFirst, 4000L to SelfieCue.Pose(firstPose),
            7000L to SelfieCue.Count(3), 8000L to SelfieCue.Count(2), 9000L to SelfieCue.Count(1),
            10000L to SelfieCue.Cheese), rig.cues)

        rig.cues.clear()
        rig.controller.onFocusReport(true, rig.now)
        rig.advance(100)
        assertEquals(5, rig.controller.state.remainingSeconds)
        val praise = rig.showState.praiseId
        val secondPose = rig.showState.poseId
        assertTrue(praise in 1..SelfieShow.PRAISE_COUNT)
        assertNotEquals(firstPose, secondPose)
        rig.advance(5000)
        assertEquals(listOf(10000L to SelfieCue.Shutter, 10100L to SelfieCue.Praise(praise), 11100L to SelfieCue.Pose(secondPose),
            12100L to SelfieCue.Count(3), 13100L to SelfieCue.Count(2), 14100L to SelfieCue.Count(1),
            15100L to SelfieCue.Cheese), rig.cues)
        assertEquals(listOf(10000L, 15100L), rig.halfPressTimes())
        assertEquals(listOf(SelfieShotResult.SHOT), rig.showState.results)
    }

    @Test fun afFailureSkipsPraiseAndLastShotIsAnnouncedOnce() {
        val rig = Rig(seed = 3)
        rig.start(SelfieSettings.KAWAII)
        listOf(true, true, false, true, true).forEach { rig.cycle(it) }
        assertEquals(SelfiePhase.COMPLETED, rig.controller.state.phase)
        assertEquals(listOf(SelfieShotResult.SHOT, SelfieShotResult.SHOT, SelfieShotResult.SKIPPED,
            SelfieShotResult.SHOT, SelfieShotResult.SHOT), rig.showState.results)
        val cues = rig.cues.map { it.second }
        assertEquals(1, cues.count { it == SelfieCue.AfFail })
        assertEquals(1, cues.count { it == SelfieCue.Last })
        assertEquals(2, cues.count { it is SelfieCue.Praise })
        val spoken = cues.filter { it !is SelfieCue.Count }
        assertTrue(spoken[spoken.indexOf(SelfieCue.AfFail) + 1] is SelfieCue.Pose)
        assertTrue(spoken[spoken.indexOf(SelfieCue.Last) + 1] is SelfieCue.Pose)
        assertEquals(5, cues.filterIsInstance<SelfieCue.Pose>().map { it.id }.distinct().size)
        val praises = cues.filterIsInstance<SelfieCue.Praise>().map { it.id }
        assertNotEquals(praises[0], praises[1])
        assertEquals(listOf(SelfieCue.Done, SelfieCue.Shots(4)), cues.takeLast(2))
    }

    @Test fun stopAndDisconnectAreHandledAndPlainSettingsStaySilent() {
        val plain = Rig()
        plain.start(SelfieSettings())
        plain.cycle(true)
        plain.cycle(false)
        plain.controller.stop()
        assertTrue(plain.cues.isEmpty())
        assertFalse(plain.showState.active)
        assertEquals(0, plain.showState.poseId)
        assertEquals(listOf(SelfieShotResult.SHOT, SelfieShotResult.SKIPPED), plain.showState.results)

        val stopped = Rig()
        stopped.start(SelfieSettings.KAWAII)
        stopped.advance(2000)
        stopped.controller.stop()
        assertEquals(SelfieCue.Stop, stopped.cues.last().second)

        val lost = Rig()
        lost.start(SelfieSettings.KAWAII)
        lost.advance(2000)
        lost.online = false
        lost.controller.disconnected()
        assertTrue(lost.controller.state.disconnected)
        assertEquals(listOf(SelfieCue.Intro), lost.cues.map { it.second })
    }

    @Test fun cueClipNamesMatchTheVoiceScript() {
        assertEquals("kw_pose_07", SelfieCue.Pose(7).clip)
        assertEquals("kw_praise_20", SelfieCue.Praise(20).clip)
        assertEquals("kw_count_3", SelfieCue.Count(3).clip)
        assertEquals("kw_shots_0", SelfieCue.Shots(0).clip)
        assertTrue(SelfieCue.Count(1).interrupt && SelfieCue.Cheese.interrupt && SelfieCue.Stop.interrupt)
        assertFalse(SelfieCue.Praise(1).interrupt)
    }
}
