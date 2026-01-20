package com.pemalang.roaddamage.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class AccelerometerHandler(private val sensorManager: SensorManager, private val delay: Int) :
        SensorEventListener {
    val readings =
            MutableSharedFlow<FloatArray>(
                    replay = 0,
                    extraBufferCapacity = 64,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
    private var sensor: Sensor? = null
    private var lastMag: Float = 0f
    private val alpha = 0.2f

    fun start() {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val s = sensor ?: return
        sensorManager.registerListener(this, s, delay)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val rawMag = sqrt(x * x + y * y + z * z).toFloat()
        val m = alpha * lastMag + (1 - alpha) * rawMag
        lastMag = m
        readings.tryEmit(floatArrayOf(x, y, z, m))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
