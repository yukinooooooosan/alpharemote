package org.staacks.alpharemote.service

import android.Manifest
import android.annotation.SuppressLint
import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import org.staacks.alpharemote.MainActivity
import org.staacks.alpharemote.R
import org.staacks.alpharemote.SettingsStore
import org.staacks.alpharemote.camera.ButtonCode
import org.staacks.alpharemote.camera.CAButton
import org.staacks.alpharemote.camera.CACountdown
import org.staacks.alpharemote.camera.CAJog
import org.staacks.alpharemote.camera.CAWaitFor
import org.staacks.alpharemote.camera.CameraAction
import org.staacks.alpharemote.camera.CameraActionStep
import org.staacks.alpharemote.camera.CameraBLE
import org.staacks.alpharemote.camera.CameraStateIdentified
import org.staacks.alpharemote.camera.CameraStateReady
import org.staacks.alpharemote.camera.WaitTarget
import org.staacks.alpharemote.camera.ReportedBoolean
import org.staacks.alpharemote.selfie.SelfieController
import org.staacks.alpharemote.selfie.SelfieSettings
import org.staacks.alpharemote.selfie.SelfieState
import org.staacks.alpharemote.selfie.SelfieAudio
import org.staacks.alpharemote.selfie.SelfieSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.staacks.alpharemote.camera.JogCode
import org.staacks.alpharemote.ui.settings.CompanionDeviceHelper
import java.io.Serializable
import java.util.LinkedList
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule
import kotlin.math.roundToLong


class AlphaRemoteService : CompanionDeviceService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var timer: TimerTask? = null
    private var notificationUI: NotificationUI? = null

    private val selfieHandler = Handler(Looper.getMainLooper())
    private lateinit var selfieController: SelfieController
    private var selfieAudio: SelfieAudio? = null
    private var lastFocusReport: ReportedBoolean? = null

    private lateinit var pendingActionsWakeLock: PowerManager.WakeLock

    companion object {

        private var cameraBLE: CameraBLE? = null
        private var deviceAppearedCount = 0

        private val _serviceState = MutableStateFlow<ServiceState>(ServiceStateGone())
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

        private val _selfieState = MutableStateFlow(SelfieState())
        val selfieState: StateFlow<SelfieState> = _selfieState.asStateFlow()
        const val SELFIE_START = "SELFIE_START"
        const val SELFIE_STOP = "SELFIE_STOP"
        const val SELFIE_SETTINGS = "selfie_settings"

        fun startSelfie(context: Context, settings: SelfieSettings): Boolean {
            if ((serviceState.value as? ServiceRunning)?.cameraState !is CameraStateReady) return false
            context.startService(Intent(context, AlphaRemoteService::class.java).apply {
                action = SELFIE_START
                putExtra(SELFIE_SETTINGS, settings)
            })
            return true
        }

        fun stopSelfie(context: Context) {
            if (serviceState.value !is ServiceRunning) return
            context.startService(Intent(context, AlphaRemoteService::class.java).setAction(SELFIE_STOP))
        }

        fun disconnect() {
            cameraBLE?.disconnectFromDevice()
        }

        fun sendCameraAction(context: Context, cameraAction: CameraAction, down: Boolean = true, up: Boolean = true) {
            if (serviceState.value !is ServiceRunning)
                return
            val intent = Intent(context, AlphaRemoteService::class.java).apply {
                action = BUTTON_INTENT_ACTION
                putExtra(BUTTON_INTENT_CAMERA_ACTION_EXTRA, cameraAction as Serializable)
                putExtra(BUTTON_INTENT_CAMERA_ACTION_DOWN_EXTRA, down)
                putExtra(BUTTON_INTENT_CAMERA_ACTION_UP_EXTRA, up)
            }
            context.startService(intent)
        }

        const val BUTTON_INTENT_ACTION = "NOTIFICATION_BUTTON"
        const val BUTTON_INTENT_CAMERA_ACTION_EXTRA = "camera_action"
        const val BUTTON_INTENT_CAMERA_ACTION_DOWN_EXTRA = "down"
        const val BUTTON_INTENT_CAMERA_ACTION_UP_EXTRA = "up"

        const val ADVANCED_SEQUENCE_INTENT_ACTION = "ADVANCED_SEQUENCE"
        const val ADVANCED_SEQUENCE_INTENT_BULB_DURATION_EXTRA = "duration"
        const val ADVANCED_SEQUENCE_INTENT_INTERVAL_DURATION_EXTRA = "interval"
        const val ADVANCED_SEQUENCE_INTENT_INTERVAL_COUNT_EXTRA = "count"
        const val ADVANCED_SEQUENCE_INTENT_FOCUS_BRACKETING_AMOUNT_EXTRA = "focus_step"

        private var pendingActionSteps = LinkedList<CameraActionStep>()
    }

    override fun onCreate() {
        super.onCreate()
        pendingActionsWakeLock = (getSystemService(POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlphaRemoteService::PendingActionsWakeLock")
        }
        selfieController = SelfieController(object : SelfieController.Port {
            override fun nowMillis() = SystemClock.elapsedRealtime()
            override fun connected() = cameraBLE?.cameraState?.value is CameraStateReady
            override fun schedule(delayMillis: Long, callback: () -> Unit): SelfieController.Cancellation {
                val runnable = Runnable(callback)
                selfieHandler.postDelayed(runnable, delayMillis)
                return SelfieController.Cancellation { selfieHandler.removeCallbacks(runnable) }
            }
            override fun send(step: CameraActionStep) {
                if (step is CAButton && step.pressed && step.button == ButtonCode.SHUTTER_HALF) {
                    lastFocusReport = (cameraBLE?.cameraState?.value as? CameraStateReady)?.focus
                }
                cameraBLE?.executeCameraActionStep(step)
            }
            override fun releaseAll() { releaseAllControls() }
            @SuppressLint("WakelockTimeout")
            override fun awake(enabled: Boolean) {
                if (enabled && !pendingActionsWakeLock.isHeld) pendingActionsWakeLock.acquire()
                if (!enabled && pendingActionsWakeLock.isHeld) pendingActionsWakeLock.release()
            }
            override fun sound(sound: SelfieSound) {
                if (selfieAudio == null) selfieAudio = SelfieAudio()
                selfieAudio?.play(sound)
            }
            override fun publish(state: SelfieState) {
                _selfieState.value = state
                notificationUI?.onSelfieStateUpdate(state)
            }
        }, this)
    }

    override fun onDeviceAppeared(address: String) {
        Log.d(MainActivity.TAG, "Device appeared: $address")
        try {
            super.onDeviceAppeared(address) //This is abstract on Android 12
        } catch (_: AbstractMethodError) {}

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w(MainActivity.TAG, "Missing Bluetooth permission. Launching activity instead.")
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.NAVIGATE_TO_INTENT_EXTRA, R.id.navigation_settings)
            }
            startActivity(intent)
            stopSelf()
            return
        }

        val settingsStore = SettingsStore(application)
        notificationUI = notificationUI ?: (NotificationUI(applicationContext).also { notificationUI ->
            scope.launch {
                settingsStore.permissions.collectLatest {
                    if (it.notification) //Refresh notification if notification permission has been granted after it was not granted previously
                        notificationUI.updateNotification()
                }
            }
        })

        if (cameraBLE == null) {
            cancelPendingActionSteps()
            deviceAppearedCount = 1
            cameraBLE = CameraBLE(scope, application, address, ::onConnect, ::onDisconnect).apply {
                scope.launch {
                    cameraState.collect { state ->
                        synchronized(this@AlphaRemoteService) {
                            _serviceState.update {
                                (it as? ServiceRunning)?.copy(cameraState = state) ?: ServiceRunning(state, null, null)
                            }
                            notificationUI?.onCameraStateUpdate(state)
                            when (state) {
                                is CameraStateReady -> {
                                    checkWaitAction(state)
                                    if (state.focus !== lastFocusReport) {
                                        lastFocusReport = state.focus
                                        state.focus.lastChange?.let {
                                            selfieController.onFocusReport(state.focus.state, it)
                                        }
                                    }
                                }
                                is CameraStateIdentified -> Unit
                                else -> {
                                    selfieController.disconnected()
                                    cancelPendingActionSteps()
                                }
                            }
                        }
                        if (state is CameraStateIdentified) settingsStore.setCameraId(state.name, state.address)
                    }
                }
            }
        } else {
            Log.w(MainActivity.TAG, "onDeviceAppeared ignored as cameraBLE has already been instantiated.")
            deviceAppearedCount++
            return
        }
    }

    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        super.onDeviceAppeared(associationInfo)
        Log.d(MainActivity.TAG, "API33 onDeviceAppeared: $associationInfo")
    }

    override fun onDeviceDisappeared(address: String) {
        Log.d(MainActivity.TAG, "Device disappeared: $address")
        try {
            super.onDeviceDisappeared(address) //This is abstract on Android 12
        } catch (_: AbstractMethodError) {}
        deviceAppearedCount--

        if (deviceAppearedCount == 0) {
            selfieController.disconnected()
            cancelPendingActionSteps()
            cameraBLE?.disconnectFromDevice()
            cameraBLE = null
            if (pendingActionsWakeLock.isHeld) {
                pendingActionsWakeLock.release()
            }
            job.cancelChildren()
            stopSelf()
        }
    }

    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        super.onDeviceDisappeared(associationInfo)
        Log.d(MainActivity.TAG, "API33 onDeviceDisappeared: $associationInfo")
    }

    private fun onConnect() {
        Log.d(MainActivity.TAG, "onConnect")
        notificationUI?.let {
            startForeground(
                it.notificationId,
                it.start(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        }
    }

    @Synchronized
    private fun onDisconnect() {
        Log.d(MainActivity.TAG, "onDisconnect")
        selfieController.disconnected()
        _serviceState.value = ServiceStateGone()
        cancelPendingActionSteps()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationUI?.stop()
        cameraBLE = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(MainActivity.TAG, "onStartCommand: $intent")
        when (intent?.action) {
            SELFIE_START -> synchronized(this) {
                val options = intent.getSerializableExtra(SELFIE_SETTINGS) as? SelfieSettings
                if (options != null && !selfieController.state.running && cameraBLE?.cameraState?.value is CameraStateReady) {
                    cancelPendingActionSteps()
                    // Mark cached focus before half-press; only subsequent reports can trigger capture.
                    lastFocusReport = (cameraBLE?.cameraState?.value as? CameraStateReady)?.focus
                    selfieController.start(options)
                }
            }
            SELFIE_STOP -> cancelPendingActionSteps()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        cancelPendingActionSteps()
        selfieHandler.removeCallbacksAndMessages(null)
        selfieAudio?.release()
        selfieAudio = null
        cameraBLE?.disconnectFromDevice()
        cameraBLE = null
        _serviceState.value = ServiceStateGone()
        notificationUI?.stop()
        job.cancelChildren()
        super.onDestroy()
    }

    private fun releaseAllControls() {
        // Always release, including manually toggled controls when there is no pending sequence.
        for (button in ButtonCode.entries) cameraBLE?.executeCameraActionStep(CAButton(false, button))
        for (jog in JogCode.entries) cameraBLE?.executeCameraActionStep(CAJog(false, -1, jog))
    }

    @Synchronized
    fun cancelPendingActionSteps(): Boolean {
        val selfieWasRunning = selfieController.state.running
        val pendingStepsCancelled = pendingActionSteps.isNotEmpty() || selfieWasRunning
        timer?.cancel()
        timer = null
        pendingActionSteps.clear()
        selfieController.stop()
        if (!selfieController.state.disconnected) selfieAudio?.stop()
        if (!selfieWasRunning) releaseAllControls()
        updatePendingActionStatistics()
        _serviceState.update {
            (it as? ServiceRunning)?.copy(countdown = null, countdownLabel = null) ?: it
        }
        if (pendingActionsWakeLock.isHeld) pendingActionsWakeLock.release()
        return pendingStepsCancelled
    }

    private fun isLongRunningSequence(steps: List<CameraActionStep>): Boolean {
        for (step in steps) {
            when (step) {
                is CACountdown -> return true
                is CAWaitFor -> return true
                else -> {}
            }
        }
        return false
    }

    @Synchronized
    @SuppressLint("WakelockTimeout")
    fun startCameraAction(steps: List<CameraActionStep>) {
        val needsCancel = steps.isEmpty() || pendingActionSteps.isNotEmpty() || selfieController.state.running
        if (needsCancel && cancelPendingActionSteps() && isLongRunningSequence(steps))
            return //If this is more than a simple button press and there were pending action, this button press is only used as a cancellation of the previous sequence
        pendingActionSteps.addAll(steps)
        if (!pendingActionsWakeLock.isHeld) {
            pendingActionsWakeLock.acquire()
        }
        executeNextCameraActionStep()
    }

    @Synchronized
    fun updatePendingActionStatistics() {
        var pendingTriggerCount = 0
        for (actionStep in pendingActionSteps) {
            if (actionStep is CAButton && actionStep.isSequenceTrigger)
                pendingTriggerCount++
        }
        if (pendingActionSteps.isEmpty() && !selfieController.state.running && pendingActionsWakeLock.isHeld) {
            pendingActionsWakeLock.release()
        }
        _serviceState.update {
            (it as? ServiceRunning)?.copy(pendingTriggerCount = pendingTriggerCount) ?: it
        }
    }

    @Synchronized
    fun executeNextCameraActionStep() {
        updatePendingActionStatistics()
        while ((pendingActionSteps.peek() is CAButton || pendingActionSteps.peek() is CAJog)) {
            pendingActionSteps.poll()?.let {
                updatePendingActionStatistics()
                cameraBLE?.executeCameraActionStep(it)
            }
        }
        (pendingActionSteps.peek() as? CACountdown)?.let { step ->
            val time = (step.duration * 1000).roundToLong()
            timer?.cancel()
            timer = Timer().schedule(time) {
                countdownActionComplete()
            }
            val targetTime = SystemClock.elapsedRealtime() + time
            _serviceState.update {
                (it as? ServiceRunning)?.copy(countdown = targetTime, countdownLabel = step.label) ?: it
            }
            return
        }
        (serviceState.value as? ServiceRunning)?.let {
            (it.cameraState as? CameraStateReady)?.let { cameraState ->
                checkWaitAction(cameraState)
            }
        }
    }

    @Synchronized
    fun countdownActionComplete() {
        _serviceState.update {
            (it as? ServiceRunning)?.copy(countdown = null, countdownLabel = null) ?: it
        }
        val nextAction = pendingActionSteps.peek()
        if (nextAction is CACountdown) {
            pendingActionSteps.removeFirst()
            executeNextCameraActionStep()
        }
    }

    @Synchronized
    fun checkWaitAction(state: CameraStateReady) {
        val nextAction = pendingActionSteps.peek()
        if (nextAction is CAWaitFor) {
            when (nextAction.target) {
                WaitTarget.FOCUS -> if (state.focus.state != nextAction.invert) {
                    pendingActionSteps.removeFirst()
                    executeNextCameraActionStep()
                }
                WaitTarget.SHUTTER -> if (state.shutter.state != nextAction.invert) {
                    pendingActionSteps.removeFirst()
                    executeNextCameraActionStep()
                }
                WaitTarget.RECORDING -> if (state.recording.state != nextAction.invert) {
                    pendingActionSteps.removeFirst()
                    executeNextCameraActionStep()
                }
            }
        }
    }

}