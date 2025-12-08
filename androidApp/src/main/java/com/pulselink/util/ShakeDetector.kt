package com.pulselink.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    private val onShake: () -> Unit,
    private val shakeThresholdGravity: Float = 2.7f,
    private val debounceMs: Long = 1_500L
) : SensorEventListener {

    private var lastShakeTimestamp: Long = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        val gX = values[0] / SensorManager.GRAVITY_EARTH
        val gY = values[1] / SensorManager.GRAVITY_EARTH
        val gZ = values[2] / SensorManager.GRAVITY_EARTH

        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
        if (gForce < shakeThresholdGravity) return

        val now = System.currentTimeMillis()
        if (now - lastShakeTimestamp < debounceMs) return
        lastShakeTimestamp = now
        onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
