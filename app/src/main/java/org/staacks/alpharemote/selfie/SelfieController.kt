package org.staacks.alpharemote.selfie

import org.staacks.alpharemote.camera.CameraActionPreset
import org.staacks.alpharemote.camera.CameraActionStep

enum class SelfiePhase { IDLE, COUNTDOWN, FOCUSING, SHOOTING, ERROR, COMPLETED, STOPPED }
enum class SelfieSound { TICK, LAST_SECOND, AF_ERROR, FINISHED }

data class SelfieState(
    val phase: SelfiePhase = SelfiePhase.IDLE,
    val remainingSeconds: Int = 0,
    val attempts: Int = 0,
    val shots: Int = 0,
    val skipped: Int = 0,
    val targetCycles: Int = 5,
    val disconnected: Boolean = false,
) {
    val running: Boolean
        get() = phase in setOf(SelfiePhase.COUNTDOWN, SelfiePhase.FOCUSING, SelfiePhase.SHOOTING) ||
            (phase == SelfiePhase.ERROR && !disconnected)
}

/** Service-owned sequence. UI lifetime never controls this object.
 * All entry points and timer callbacks share the service's lock, so STOP wins over queued work.
 */
class SelfieController(private val port: Port, private val lock: Any = Any()) {
    fun interface Cancellation { fun cancel() }

    interface Port {
        fun nowMillis(): Long
        fun connected(): Boolean
        fun schedule(delayMillis: Long, callback: () -> Unit): Cancellation
        fun send(step: CameraActionStep)
        fun releaseAll()
        fun awake(enabled: Boolean)
        fun sound(sound: SelfieSound)
        fun publish(state: SelfieState)
    }

    var state = SelfieState()
        private set
    private var settings = SelfieSettings()
    private var pending: Cancellation? = null
    private var generation = 0L
    private var focusStartedAt = 0L

    fun start(options: SelfieSettings): Boolean = synchronized(lock) {
        if (state.running || !port.connected()) return false
        invalidateTimer()
        settings = options
        state = SelfieState(phase = SelfiePhase.COUNTDOWN, targetCycles = options.cycles)
        port.releaseAll()
        port.awake(true)
        countdown(options.countdownSeconds)
        true
    }

    // Only new camera focus reports are forwarded here; cached focus must never trigger a shot.
    fun onFocusReport(acquired: Boolean, reportedAt: Long): Unit = synchronized(lock) {
        if (state.phase != SelfiePhase.FOCUSING) return
        if (port.nowMillis() >= focusStartedAt + SelfieSettings.AF_TIMEOUT_MILLIS) {
            focusTimeout()
            return
        }
        if (!acquired || reportedAt < focusStartedAt) return
        if (!port.connected()) { disconnected(); return }
        invalidateTimer()
        publish(state.copy(phase = SelfiePhase.SHOOTING))
        port.send(CameraActionPreset.TRIGGER_ON_FOCUS.template.press.last())
        later(settings.holdMillis.toLong()) {
            releaseShutter()
            finishCycle(success = true)
        }
    }

    fun stop(): Unit = synchronized(lock) {
        if (!state.running) return
        invalidateTimer()
        port.releaseAll()
        port.awake(false)
        publish(state.copy(phase = SelfiePhase.STOPPED, remainingSeconds = 0))
    }

    fun disconnected(): Unit = synchronized(lock) {
        if (!state.running) return
        invalidateTimer()
        port.releaseAll()
        port.awake(false)
        port.sound(SelfieSound.AF_ERROR)
        publish(state.copy(phase = SelfiePhase.ERROR, remainingSeconds = 0, disconnected = true))
    }

    private fun countdown(seconds: Int) {
        if (!port.connected()) { disconnected(); return }
        publish(state.copy(phase = SelfiePhase.COUNTDOWN, remainingSeconds = seconds))
        if (settings.countdownSound) {
            port.sound(if (seconds == 1) SelfieSound.LAST_SECOND else SelfieSound.TICK)
        }
        later(1000) {
            if (seconds > 1) countdown(seconds - 1) else focus()
        }
    }

    private fun focus() {
        if (!port.connected()) { disconnected(); return }
        focusStartedAt = port.nowMillis()
        publish(state.copy(phase = SelfiePhase.FOCUSING, remainingSeconds = 0))
        // Reuse the upstream TRIGGER_ON_FOCUS half-press / FOCUS wait / full-press protocol.
        // The wait is bounded here; full-press is sent only on a new Focus Acquired report.
        later(SelfieSettings.AF_TIMEOUT_MILLIS, ::focusTimeout)
        port.send(CameraActionPreset.TRIGGER_ON_FOCUS.template.press.first())
    }

    private fun focusTimeout() {
        invalidateTimer()
        releaseShutter()
        publish(state.copy(phase = SelfiePhase.ERROR))
        port.sound(SelfieSound.AF_ERROR)
        later(400) { finishCycle(success = false) }
    }

    private fun releaseShutter() {
        CameraActionPreset.TRIGGER_ON_FOCUS.template.release.forEach(port::send)
    }

    private fun finishCycle(success: Boolean) {
        val updated = state.copy(
            attempts = state.attempts + 1,
            shots = state.shots + if (success) 1 else 0,
            skipped = state.skipped + if (success) 0 else 1,
        )
        if (settings.cycles != SelfieSettings.INFINITE && updated.attempts >= settings.cycles) {
            invalidateTimer()
            port.awake(false)
            publish(updated.copy(phase = SelfiePhase.COMPLETED, remainingSeconds = 0))
            port.sound(SelfieSound.FINISHED)
        } else {
            state = updated
            countdown(settings.countdownSeconds)
        }
    }

    private fun publish(value: SelfieState) {
        state = value
        port.publish(value)
    }

    private fun invalidateTimer() {
        generation++
        pending?.cancel()
        pending = null
    }

    private fun later(delayMillis: Long, action: () -> Unit) {
        invalidateTimer()
        val token = generation
        pending = port.schedule(delayMillis) {
            synchronized(lock) {
                if (token == generation && state.running) {
                    pending = null
                    if (port.connected()) action() else disconnected()
                }
            }
        }
    }
}
