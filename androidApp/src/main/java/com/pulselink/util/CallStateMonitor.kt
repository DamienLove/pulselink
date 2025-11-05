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

    @Volatile private var telephonyCallback: TelephonyCallback? = null
    @Volatile private var legacyListener: PhoneStateListener? = null
    private var awaitingResult = false

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun monitorOutgoingCall(onCompleted: (Long) -> Unit) {
        synchronized(lock) {
            stopLocked()
            completion = onCompleted
            awaitingResult = true
            registerLocked()
        }
    }

    fun cancel() {
        synchronized(lock) {
            stopLocked()
        }
    }

    private fun registerLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleState(state)
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
                    handleState(state)
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

    private fun handleState(state: Int) {
        val callback: ((Long) -> Unit)?
        val duration: Long
        synchronized(lock) {
            if (!active || !awaitingResult) return
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (startTimestamp == 0L) {
                        startTimestamp = System.currentTimeMillis()
                    }
                    return
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    duration = if (startTimestamp == 0L) 0L
                    else (System.currentTimeMillis() - startTimestamp).coerceAtLeast(0L)
                    callback = completion
                    stopLocked()
                }
                else -> return
            }
        }
        callback?.let { mainExecutor.execute { it(duration) } }
    }

    private fun dispatchFailure() {
        val callback = completion
        stopLocked()
        if (callback != null) {
            mainExecutor.execute { callback.invoke(0L) }
        }
    }

    private fun stopLocked() {
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
        awaitingResult = false
        startTimestamp = 0L
        completion = null
    }
}
