package org.staacks.alpharemote.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import org.staacks.alpharemote.MainActivity
import org.staacks.alpharemote.R
import org.staacks.alpharemote.camera.CameraState
import org.staacks.alpharemote.camera.CameraStateConnecting
import org.staacks.alpharemote.camera.CameraStateError
import org.staacks.alpharemote.camera.CameraStateReady
import org.staacks.alpharemote.selfie.SelfiePhase
import org.staacks.alpharemote.selfie.SelfieState

/** Connection status at rest; selfie progress and STOP during a session. */
class NotificationUI(private val context: Context) {
    private val channelId = "AlphaRemote"
    val notificationId = 1
    private var notificationManager: NotificationManager? = null
    private var cameraState: CameraState? = null
    private var selfieState = SelfieState()
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = Runnable {
        synchronized(this) {
            notificationManager?.notify(notificationId, buildNotification())
        }
    }

    @Synchronized
    fun start(): Notification {
        notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(NotificationChannel(
            channelId, context.getText(R.string.app_name), NotificationManager.IMPORTANCE_LOW))
        return buildNotification()
    }

    @Synchronized
    fun stop() {
        handler.removeCallbacks(refresh)
        notificationManager = null
    }

    @Synchronized
    fun updateNotification() {
        handler.removeCallbacks(refresh)
        handler.postDelayed(refresh, 200)
    }

    private fun buildNotification(): Notification {
        val status = if (selfieState.running) {
            when (selfieState.phase) {
                SelfiePhase.COUNTDOWN -> context.getString(R.string.selfie_countdown_notification, selfieState.remainingSeconds)
                SelfiePhase.FOCUSING -> context.getString(R.string.selfie_focusing)
                SelfiePhase.SHOOTING -> context.getString(R.string.selfie_shooting)
                else -> context.getString(R.string.selfie_af_failed)
            }
        } else {
            when (val state = cameraState) {
                is CameraStateReady -> context.getString(R.string.settings_camera_connected,
                    state.name ?: context.getString(R.string.settings_camera_unknown_name))
                is CameraStateConnecting -> context.getString(R.string.status_connecting)
                is CameraStateError -> context.getString(R.string.settings_camera_error, state.description)
                else -> context.getString(R.string.selfie_offline)
            }
        }
        val progress = context.getString(R.string.selfie_progress,
            selfieState.attempts, selfieState.shots, selfieState.skipped)
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentIntent(openAppIntent())
            .setSmallIcon(R.drawable.ic_camera_black_24dp)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(status)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        if (selfieState.running) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText("$status\n$progress"))
                .addAction(R.drawable.ca_stop, context.getString(R.string.selfie_stop),
                    PendingIntent.getService(context, 1001,
                        Intent(context, AlphaRemoteService::class.java).setAction(AlphaRemoteService.SELFIE_STOP),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        }
        return builder.build()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(context, 42,
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    @Synchronized
    fun onSelfieStateUpdate(state: SelfieState) {
        selfieState = state
        if (state.disconnected) {
            notificationManager?.notify(2, NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_camera_black_24dp)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.selfie_disconnected))
                .setContentIntent(openAppIntent())
                .setAutoCancel(true).build())
        } else if (state.running) {
            notificationManager?.cancel(2)
        }
        updateNotification()
    }

    @Synchronized
    fun onCameraStateUpdate(state: CameraState) {
        cameraState = state
        updateNotification()
    }
}
