package org.staacks.alpharemote.selfie

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/** STREAM_ALARM keeps screen-off countdowns audible independently of media volume. */
class SelfieAudio {
    private val tone = try {
        ToneGenerator(AudioManager.STREAM_ALARM, 80)
    } catch (e: RuntimeException) {
        Log.w("SelfieAudio", "Audio unavailable", e)
        null
    }

    fun play(sound: SelfieSound) {
        when (sound) {
            SelfieSound.TICK -> tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            SelfieSound.LAST_SECOND -> tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            SelfieSound.AF_ERROR -> tone?.startTone(ToneGenerator.TONE_PROP_NACK, 300)
            SelfieSound.FINISHED -> tone?.startTone(ToneGenerator.TONE_PROP_ACK, 700)
        }
    }

    fun stop() { tone?.stopTone() }
    fun release() { tone?.release() }
}
