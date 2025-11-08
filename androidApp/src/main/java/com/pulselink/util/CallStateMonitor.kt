package com.pulselink.util

import android.Manifest
import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallStateMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager
) {

    private val lock = Any()
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    @Volatile private var active = false
    @Volatile private var startTimestamp = 0L
    @Volatile private var completion: ((Long) -> Unit)? = null
    @Volatile private var incomingCallback: ((String?) -> Unit)? = null
    @Volatile private var incomingFinishedCallback: (() -> Unit)? = null

    @Volatile private var telephonyCallback: TelephonyCallback? = null
    @Volatile private var legacyListener: PhoneStateListener? = null
    private var awaitingResult = false
    private var monitoringOutgoing = false
    private var monitoringIncoming = false
    private var ringingActive = false
    private var lastIncomingNumber: String? = null

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun monitorOutgoingCall(onCompleted: (Long) -> Unit) {
        synchronized(lock) {
            monitoringOutgoing = true
            awaitingResult = true
            completion = onCompleted
            startTimestamp = 0L
            ensureRegisteredLocked()
        }
    }

    fun cancel() {
        synchronized(lock) {
            monitoringOutgoing = false
            awaitingResult = false
            completion = null
            startTimestamp = 0L
            releaseIfPossibleLocked()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG])
    fun monitorIncomingCalls(onRinging: (String?) -> Unit, onCallFinished: () -> Unit = {}) {
        synchronized(lock) {
            incomingCallback = onRinging
            incomingFinishedCallback = onCallFinished
            monitoringIncoming = true
            ensureRegisteredLocked()
        }
    }

    fun stopIncomingMonitoring() {
        synchronized(lock) {
            monitoringIncoming = false
            incomingCallback = null
            incomingFinishedCallback = null
            ringingActive = false
            lastIncomingNumber = null
            releaseIfPossibleLocked()
        }
    }

    private fun ensureRegisteredLocked() {
        if (active) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleState(state, null)
                }
            }
            telephonyCallback = callback
            try {
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                active = true
            } catch (error: SecurityException) {
                dispatchFailure()
            }
        } else {
            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleState(state, phoneNumber)
                }
            }
            legacyListener = listener
            try {
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                active = true
            } catch (error: SecurityException) {
                dispatchFailure()
            }
        }
    }

    private fun releaseIfPossibleLocked() {
        if (!monitoringIncoming && !monitoringOutgoing && active) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let {
                    runCatching { telephonyManager.unregisterTelephonyCallback(it) }
                }
                telephonyCallback = null
            } else {
                legacyListener?.let {
                    runCatching { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
                }
                legacyListener = null
            }
            active = false
        }
    }

    private fun handleState(state: Int, phoneNumber: String?) {
        var outgoingCallback: ((Long) -> Unit)? = null
        var outgoingDuration = 0L
        var ringingCallback: ((String?) -> Unit)? = null
        var ringingNumber: String? = phoneNumber
        var endCallback: (() -> Unit)? = null
        synchronized(lock) {
            if (!active) return
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    if (!monitoringIncoming) return
                    if (!phoneNumber.isNullOrBlank()) {
                        lastIncomingNumber = phoneNumber
                        ringingNumber = phoneNumber
                    } else if (ringingNumber.isNullOrBlank()) {
                        ringingNumber = lastIncomingNumber
                    }
                    ringingActive = true
                    ringingCallback = incomingCallback
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (monitoringOutgoing && awaitingResult && startTimestamp == 0L) {
                        startTimestamp = System.currentTimeMillis()
                    }
                    if (ringingActive) {
                        ringingActive = false
                        endCallback = incomingFinishedCallback
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (monitoringOutgoing && awaitingResult) {
                        outgoingDuration = if (startTimestamp == 0L) 0L
                        else (System.currentTimeMillis() - startTimestamp).coerceAtLeast(0L)
                        outgoingCallback = completion
                        completion = null
                        monitoringOutgoing = false
                        awaitingResult = false
                        startTimestamp = 0L
                    }
                    if (ringingActive || monitoringIncoming) {
                        ringingActive = false
                        endCallback = incomingFinishedCallback
                    }
                    if (!monitoringIncoming && !monitoringOutgoing) {
                        releaseIfPossibleLocked()
                    }
                }
                else -> {}
            }
        }
        ringingCallback?.let { callback ->
            mainExecutor.execute { callback(ringingNumber) }
        }
        outgoingCallback?.let { callback ->
            val durationCopy = outgoingDuration
            mainExecutor.execute { callback(durationCopy) }
        }
        endCallback?.let { callback ->
            mainExecutor.execute { callback() }
        }
    }

    private fun dispatchFailure() {
        val callback = completion
        completion = null
        monitoringOutgoing = false
        awaitingResult = false
        monitoringIncoming = false
        incomingCallback = null
        incomingFinishedCallback = null
        releaseIfPossibleLocked()
        if (callback != null) {
            mainExecutor.execute { callback.invoke(0L) }
        }
    }
}
