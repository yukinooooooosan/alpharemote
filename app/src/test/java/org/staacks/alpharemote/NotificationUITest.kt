package org.staacks.alpharemote

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.staacks.alpharemote.selfie.SelfiePhase
import org.staacks.alpharemote.selfie.SelfieState
import org.staacks.alpharemote.service.AlphaRemoteService
import org.staacks.alpharemote.service.NotificationUI
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class NotificationUITest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val notifications = shadowOf(context.getSystemService(NotificationManager::class.java))

    @Test fun notificationOnlyOffersStopDuringSelfie() {
        val ui = NotificationUI(context)
        assertEquals(0, ui.start().actions.orEmpty().size)
        ui.onSelfieStateUpdate(SelfieState(phase = SelfiePhase.COUNTDOWN, remainingSeconds = 5))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        val running = notifications.getNotification(1)
        assertEquals(context.getString(R.string.selfie_countdown_notification, 5),
            running.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(1, running.actions.size)
        assertEquals(AlphaRemoteService.SELFIE_STOP,
            shadowOf(running.actions.single().actionIntent).savedIntent.action)
        ui.onSelfieStateUpdate(SelfieState(phase = SelfiePhase.STOPPED))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertEquals(0, notifications.getNotification(1).actions.orEmpty().size)
        ui.stop()
    }

    @Test fun disconnectNoticeSurvivesStopAndQueuedRefreshIsCancelled() {
        val ui = NotificationUI(context)
        ui.start()
        ui.onSelfieStateUpdate(SelfieState(phase = SelfiePhase.ERROR, disconnected = true))
        ui.stop()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertNotNull(notifications.getNotification(2))
        assertNull(notifications.getNotification(1))
    }
}
