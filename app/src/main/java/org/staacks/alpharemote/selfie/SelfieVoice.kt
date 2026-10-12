package org.staacks.alpharemote.selfie

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * Plays kawaii voice clips from res/raw on one channel (docs/KAWAII-VOICE-SCRIPT.md, 再生のルール).
 * Fire-and-forget: callers never wait, so camera timing is unaffected.
 * Without clips in the APK every call is a no-op and the regular tones keep playing.
 */
class SelfieVoice(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    /** Waiting cues with their deadlines; at most [MAX_QUEUE], oldest dropped first. */
    private val queued = ArrayDeque<Pair<SelfieCue, Long>>()

    /** True once the voice pack is bundled; checked with a clip every pack must contain. */
    val available: Boolean by lazy { rawId(SelfieCue.Count(3).clip) != 0 }

    fun play(cue: SelfieCue) {
        if (!available) return
        handler.post { playNow(cue) }
    }

    fun release() {
        handler.post {
            queued.clear()
            stopPlayer()
        }
    }

    @SuppressLint("DiscouragedApi") // Clip names come from the voice script ids, not from R constants.
    private fun rawId(name: String): Int = context.resources.getIdentifier(name, "raw", context.packageName)

    private fun playNow(cue: SelfieCue) {
        val id = rawId(cue.clip)
        if (id == 0) return
        val busy = runCatching { player?.isPlaying == true }.getOrDefault(false)
        if (busy && !cue.interrupt) {
            queued.addLast(cue to SystemClock.elapsedRealtime() + cue.maxWaitMillis)
            while (queued.size > MAX_QUEUE) queued.removeFirst()
            return
        }
        if (cue.interrupt) queued.clear()
        start(id)
    }

    private fun start(id: Int) {
        stopPlayer()
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            context.resources.openRawResourceFd(id).use { file ->
                next.setDataSource(file.fileDescriptor, file.startOffset, file.length)
            }
            next.setOnCompletionListener { finished(it) }
            next.prepare()
            next.start()
            player = next
        } catch (e: Exception) {
            Log.w("SelfieVoice", "Voice clip failed", e)
            next.release()
        }
    }

    private fun finished(done: MediaPlayer) {
        if (player === done) stopPlayer()
        val now = SystemClock.elapsedRealtime()
        while (queued.isNotEmpty()) {
            val (cue, until) = queued.removeFirst()
            if (now <= until) {
                playNow(cue)
                return
            }
        }
    }

    private fun stopPlayer() {
        player?.let {
            it.setOnCompletionListener(null)
            runCatching { if (it.isPlaying) it.stop() }
            it.release()
        }
        player = null
    }

    private companion object {
        const val MAX_QUEUE = 2
    }
}
