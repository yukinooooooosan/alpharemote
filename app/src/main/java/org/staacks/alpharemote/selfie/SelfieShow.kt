package org.staacks.alpharemote.selfie

import java.util.Locale
import kotlin.random.Random

/** One voice line. [clip] is the res/raw name (docs/KAWAII-VOICE-SCRIPT.md). */
sealed class SelfieCue(val clip: String, val interrupt: Boolean = false, val maxWaitMillis: Long = 600) {
    data object Intro : SelfieCue("kw_intro")
    data object PoseLeadFirst : SelfieCue("kw_pose_lead_first")
    data class Pose(val id: Int) : SelfieCue(String.format(Locale.ROOT, "kw_pose_%02d", id))
    data class Count(val n: Int) : SelfieCue("kw_count_$n", interrupt = true)
    data object Cheese : SelfieCue("kw_cheese", interrupt = true)
    data object Shutter : SelfieCue("kw_shutter")
    data class Praise(val id: Int) : SelfieCue(String.format(Locale.ROOT, "kw_praise_%02d", id))
    data object Last : SelfieCue("kw_last")
    data object AfFail : SelfieCue("kw_af_fail")
    data object Done : SelfieCue("kw_done")
    data class Shots(val n: Int) : SelfieCue("kw_shots_$n", maxWaitMillis = 3000)
    data object Stop : SelfieCue("kw_stop", interrupt = true)
}

enum class SelfieShotResult { SHOT, SKIPPED }

/** What the kawaii screen shows besides the plain session. Ids are 1-based; 0 = none yet. */
data class SelfieShowState(
    val active: Boolean = false,
    val poseId: Int = 0,
    val praiseId: Int = 0,
    val results: List<SelfieShotResult> = emptyList(),
) {
    val lastResult: SelfieShotResult? get() = results.lastOrNull()
}

/**
 * Turns published session states into poses, praise and voice cues for the kawaii show.
 * It only observes; camera timing stays entirely in [SelfieController].
 * The service owns it, so the screen and the voice always use the same pose and praise ids.
 */
class SelfieShow(private val random: Random = Random.Default, private val cue: (SelfieCue) -> Unit) {
    var state = SelfieShowState()
        private set
    private var settings: SelfieSettings? = null
    private var previous: SelfieState? = null
    private var poseOrder: List<Int> = emptyList()

    /** Call before [SelfieController.start]; a non-show setting turns the show off. */
    fun prepare(options: SelfieSettings) {
        settings = options.takeIf { it.show }
        previous = null
        poseOrder = emptyList()
        state = SelfieShowState()
    }

    fun onState(session: SelfieState): SelfieShowState {
        val options = settings ?: return state
        val prev = previous
        previous = session
        val entered = prev == null || prev.phase != session.phase
        when (session.phase) {
            SelfiePhase.COUNTDOWN -> countdown(options, prev, session, entered)
            SelfiePhase.FOCUSING -> if (entered) cue(SelfieCue.Cheese)
            SelfiePhase.SHOOTING -> if (entered) cue(SelfieCue.Shutter)
            SelfiePhase.ERROR -> if (entered && !session.disconnected) cue(SelfieCue.AfFail)
            SelfiePhase.COMPLETED -> if (entered && prev != null) {
                state = state.copy(results = state.results + result(prev, session))
                cue(SelfieCue.Done)
                cue(SelfieCue.Shots(session.shots.coerceIn(0, MAX_SHOTS_CUE)))
            }
            SelfiePhase.STOPPED -> if (entered) cue(SelfieCue.Stop)
            SelfiePhase.IDLE -> Unit
        }
        return state
    }

    private fun countdown(options: SelfieSettings, prev: SelfieState?, session: SelfieState, entered: Boolean) {
        if (poseOrder.isEmpty()) poseOrder = (1..POSE_COUNT).shuffled(random)
        if (session.attempts == 0 && (prev == null || !prev.running)) {
            poseOrder = (1..POSE_COUNT).shuffled(random)
            state = SelfieShowState(active = true, poseId = poseOrder.first())
            cue(SelfieCue.Intro)
        } else if (entered && prev != null && session.attempts > prev.attempts) {
            val outcome = result(prev, session)
            val finalRound = options.cycles != SelfieSettings.INFINITE && session.attempts == options.cycles - 1
            val praise = if (outcome == SelfieShotResult.SHOT) nextPraise() else state.praiseId
            state = state.copy(poseId = poseOrder[session.attempts % poseOrder.size], praiseId = praise,
                results = state.results + outcome)
            if (outcome == SelfieShotResult.SHOT) cue(if (finalRound) SelfieCue.Last else SelfieCue.Praise(praise))
        } else if (!entered && prev?.remainingSeconds == session.remainingSeconds) {
            return
        }
        val total = if (session.attempts == 0) options.firstCountdown else options.countdownSeconds
        val elapsed = total - session.remainingSeconds
        if (session.attempts == 0 && total >= 7) {
            if (elapsed == 3) cue(SelfieCue.PoseLeadFirst)
            if (elapsed == 4) cue(SelfieCue.Pose(state.poseId))
        } else if (elapsed == 1) {
            cue(SelfieCue.Pose(state.poseId))
        }
        if (elapsed > 0 && session.remainingSeconds in 1..3) cue(SelfieCue.Count(session.remainingSeconds))
    }

    private fun result(prev: SelfieState, now: SelfieState) =
        if (now.shots > prev.shots) SelfieShotResult.SHOT else SelfieShotResult.SKIPPED

    private fun nextPraise(): Int {
        var next = state.praiseId
        while (next == state.praiseId) next = random.nextInt(PRAISE_COUNT) + 1
        return next
    }

    companion object {
        const val POSE_COUNT = 32
        const val PRAISE_COUNT = 20
        const val MAX_SHOTS_CUE = 5
    }
}
