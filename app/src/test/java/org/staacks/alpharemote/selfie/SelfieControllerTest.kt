package org.staacks.alpharemote.selfie

import org.junit.Assert.*
import org.junit.Test
import org.staacks.alpharemote.camera.ButtonCode
import org.staacks.alpharemote.camera.CAButton
import org.staacks.alpharemote.camera.CAJog
import org.staacks.alpharemote.camera.CameraActionStep
import org.staacks.alpharemote.camera.JogCode

class SelfieControllerTest {
    private class FakePort : SelfieController.Port {
        data class Task(val due: Long, val action: () -> Unit, var cancelled: Boolean = false)
        var now = 0L
        var online = true
        var wake = false
        val tasks = mutableListOf<Task>()
        val commands = mutableListOf<Pair<Long, CameraActionStep>>()
        val sounds = mutableListOf<SelfieSound>()
        val states = mutableListOf<SelfieState>()
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
        override fun awake(enabled: Boolean) { wake = enabled }
        override fun sound(sound: SelfieSound) { sounds += sound }
        override fun publish(state: SelfieState) { states += state }
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
        fun presses(button: ButtonCode) = commands.filter { (_, step) -> step is CAButton && step.pressed && step.button == button }
    }

    @Test fun countdownCompletesBeforeAFAndNewFocusIsRequired() {
        val port = FakePort()
        val controller = SelfieController(port)
        assertTrue(controller.start(SelfieSettings(cycles = 1)))
        port.advance(4999)
        assertTrue(port.commands.none { (_, step) -> step is CAButton && step.pressed })
        controller.onFocusReport(true, 0) // Cached focus during countdown does nothing.
        port.advance(1)
        assertEquals(SelfiePhase.FOCUSING, controller.state.phase)
        assertEquals(5000L, port.presses(ButtonCode.SHUTTER_HALF).single().first)
        controller.onFocusReport(true, 4999) // A stale report after countdown also does nothing.
        controller.onFocusReport(false, 5000)
        assertTrue(port.presses(ButtonCode.SHUTTER_FULL).isEmpty())
        controller.onFocusReport(true, 5010)
        assertEquals(SelfiePhase.SHOOTING, controller.state.phase)
        port.advance(999)
        assertTrue(port.wake)
        port.advance(1)
        assertEquals(SelfiePhase.COMPLETED, controller.state.phase)
        assertEquals(1, controller.state.shots)
        assertFalse(port.wake)
        assertEquals(listOf(SelfieSound.TICK, SelfieSound.TICK, SelfieSound.TICK, SelfieSound.TICK,
            SelfieSound.LAST_SECOND, SelfieSound.FINISHED), port.sounds)
        assertEquals(listOf(CAButton(false, ButtonCode.SHUTTER_FULL), CAButton(false, ButtonCode.SHUTTER_HALF)),
            port.commands.takeLast(2).map { it.second })
    }

    @Test fun focusTimeoutSkipsAndCountsAsAnAttemptEvenWhenCountdownSoundIsOff() {
        val port = FakePort()
        val controller = SelfieController(port)
        controller.start(SelfieSettings(countdownSeconds = 3, cycles = 5, countdownSound = false))
        repeat(5) {
            port.advance(6000)
            assertEquals(SelfiePhase.ERROR, controller.state.phase)
            assertTrue(port.presses(ButtonCode.SHUTTER_FULL).isEmpty())
            controller.onFocusReport(true, port.now) // Late success cannot shoot in ERROR.
            port.advance(400)
        }
        assertEquals(SelfiePhase.COMPLETED, controller.state.phase)
        assertEquals(5, controller.state.attempts)
        assertEquals(0, controller.state.shots)
        assertEquals(5, controller.state.skipped)
        assertEquals(List(5) { SelfieSound.AF_ERROR } + SelfieSound.FINISHED, port.sounds)
        assertFalse(port.wake)
    }

    @Test fun allHoldOptionsReleaseAtTheConfiguredTime() {
        for (hold in SelfieSettings.HOLDS) {
            val port = FakePort()
            val controller = SelfieController(port)
            controller.start(SelfieSettings(countdownSeconds = 3, holdMillis = hold, cycles = 1))
            port.advance(3000)
            controller.onFocusReport(true, port.now)
            port.advance(hold.toLong() - 1)
            assertEquals(SelfiePhase.SHOOTING, controller.state.phase)
            port.advance(1)
            assertEquals(SelfiePhase.COMPLETED, controller.state.phase)
        }
    }

    @Test fun stopCancelsEveryPhaseAndRejectsAlreadyQueuedCallbacks() {
        for (phase in listOf(SelfiePhase.COUNTDOWN, SelfiePhase.FOCUSING, SelfiePhase.SHOOTING, SelfiePhase.ERROR)) {
            val port = FakePort()
            val controller = SelfieController(port)
            controller.start(SelfieSettings(countdownSeconds = 3))
            if (phase != SelfiePhase.COUNTDOWN) port.advance(3000)
            if (phase == SelfiePhase.SHOOTING) controller.onFocusReport(true, port.now)
            if (phase == SelfiePhase.ERROR) port.advance(3000)
            assertEquals(phase, controller.state.phase)
            val queued = port.tasks.filter { !it.cancelled }.map { it.action }
            controller.stop()
            val count = port.commands.size
            queued.forEach { it() } // Simulate a runnable dispatched just before cancellation.
            controller.onFocusReport(true, port.now)
            port.advance(100000)
            assertEquals(SelfiePhase.STOPPED, controller.state.phase)
            assertFalse(port.wake)
            assertEquals(count, port.commands.size)
            assertEquals(ButtonCode.entries.toSet(), port.commands.takeLast(9).mapNotNull {
                (it.second as? CAButton)?.takeIf { !it.pressed }?.button
            }.toSet())
        }
    }

    @Test fun infiniteLoopSchedulesOnlyOneStepAndRefocusesEachCycle() {
        val port = FakePort()
        val controller = SelfieController(port)
        controller.start(SelfieSettings(countdownSeconds = 3, holdMillis = 100, cycles = 0))
        repeat(1000) {
            port.advance(3000)
            assertEquals(SelfiePhase.FOCUSING, controller.state.phase)
            controller.onFocusReport(true, port.now)
            controller.onFocusReport(true, port.now) // Duplicate report must not trigger twice.
            port.advance(100)
            assertEquals(1, port.tasks.count { !it.cancelled })
        }
        assertEquals(1000, controller.state.attempts)
        assertEquals(1000, port.presses(ButtonCode.SHUTTER_HALF).size)
        assertEquals(1000, port.presses(ButtonCode.SHUTTER_FULL).size)
        controller.stop()
    }

    @Test fun disconnectStopsAndReconnectRequiresExplicitStart() {
        val port = FakePort()
        val controller = SelfieController(port)
        controller.start(SelfieSettings())
        port.advance(5000)
        controller.onFocusReport(true, port.now)
        port.online = false
        controller.disconnected()
        assertTrue(controller.state.disconnected)
        assertFalse(controller.state.running)
        assertFalse(port.wake)
        port.online = true
        controller.onFocusReport(true, port.now)
        port.advance(100000)
        assertEquals(1, port.presses(ButtonCode.SHUTTER_FULL).size)
        assertTrue(controller.start(SelfieSettings()))
        assertEquals(SelfiePhase.COUNTDOWN, controller.state.phase)
    }

    @Test fun offlineStartAndRepeatedStartDoNotScheduleAdditionalWork() {
        val port = FakePort()
        val controller = SelfieController(port)
        port.online = false
        assertFalse(controller.start(SelfieSettings()))
        assertTrue(port.tasks.isEmpty())
        assertFalse(port.wake)
        port.online = true
        assertTrue(controller.start(SelfieSettings()))
        assertFalse(controller.start(SelfieSettings()))
        assertEquals(1, port.tasks.size)
    }

    @Test fun focusAfterDeadlineCannotShootEvenIfTimeoutRunnableIsDelayed() {
        val port = FakePort()
        val controller = SelfieController(port)
        controller.start(SelfieSettings(countdownSeconds = 3, cycles = 1))
        port.advance(3000)
        val timeout = port.tasks.single().action
        port.now = 6500 // Camera callback arrives on IO while the main looper has been busy.
        controller.onFocusReport(true, port.now)
        assertEquals(SelfiePhase.ERROR, controller.state.phase)
        assertTrue(port.presses(ButtonCode.SHUTTER_FULL).isEmpty())
        timeout() // The delayed, cancelled runnable must not change the state or play another error.
        assertEquals(1, port.sounds.count { it == SelfieSound.AF_ERROR })
        port.advance(400)
        assertEquals(SelfiePhase.COMPLETED, controller.state.phase)
        assertEquals(1, controller.state.skipped)
    }
}
