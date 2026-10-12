package org.staacks.alpharemote.selfie

import java.io.Serializable

data class SelfieSettings(
    val countdownSeconds: Int = 5,
    val holdMillis: Int = 1000,
    val cycles: Int = 5,
    val countdownSound: Boolean = true,
    /** Countdown of the first cycle only (time to walk into position). Null uses [countdownSeconds]. */
    val firstCountdownSeconds: Int? = null,
    /** kawaii show: poses, praise and voice cues. Never changes camera timing. */
    val show: Boolean = false,
) : Serializable {
    init {
        require(countdownSeconds in COUNTDOWNS)
        require(holdMillis in HOLDS)
        require(cycles in CYCLES)
        require(firstCountdownSeconds == null || firstCountdownSeconds in COUNTDOWNS)
    }

    val firstCountdown: Int get() = firstCountdownSeconds ?: countdownSeconds

    companion object {
        const val AF_TIMEOUT_MILLIS = 3000L
        const val SINGLE_SHOT_MILLIS = 100
        const val INFINITE = 0
        val COUNTDOWNS = setOf(3, 5, 10)
        val HOLDS = setOf(SINGLE_SHOT_MILLIS, 500, 1000, 2000)
        val CYCLES = setOf(1, 5, 10, INFINITE)

        /** kawaii mode is fixed: single shot, 5 attempts, 10 s to walk in, then 5 s. Not persisted. */
        val KAWAII = SelfieSettings(countdownSeconds = 5, holdMillis = SINGLE_SHOT_MILLIS, cycles = 5,
            countdownSound = true, firstCountdownSeconds = 10, show = true)
    }
}
