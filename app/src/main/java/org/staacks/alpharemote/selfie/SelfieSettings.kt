package org.staacks.alpharemote.selfie

import java.io.Serializable

data class SelfieSettings(
    val countdownSeconds: Int = 5,
    val holdMillis: Int = 1000,
    val cycles: Int = 5,
    val countdownSound: Boolean = true,
) : Serializable {
    init {
        require(countdownSeconds in COUNTDOWNS)
        require(holdMillis in HOLDS)
        require(cycles in CYCLES)
    }

    companion object {
        const val AF_TIMEOUT_MILLIS = 3000L
        const val SINGLE_SHOT_MILLIS = 100
        const val INFINITE = 0
        val COUNTDOWNS = setOf(3, 5, 10)
        val HOLDS = setOf(SINGLE_SHOT_MILLIS, 500, 1000, 2000)
        val CYCLES = setOf(1, 5, 10, INFINITE)
    }
}
