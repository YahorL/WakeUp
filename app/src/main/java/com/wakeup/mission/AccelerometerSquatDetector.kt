package com.wakeup.mission

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/**
 * Derives the world-vertical (up-positive) linear acceleration by projecting the gravity-removed
 * linear-acceleration vector onto the gravity direction, and feeds it to [SquatMotionCounter],
 * which integrates to a vertical displacement so only a genuine knee-bend distance counts (a brief
 * jerk has a big acceleration spike but almost no displacement).
 */
class AccelerometerSquatDetector(context: Context) : SquatDetector {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccel: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val core = SquatDetectorCore()

    override fun repCounts(): Flow<Int> = callbackFlow {
        core.reset()
        trySend(0)
        val gravity = FloatArray(3)
        var haveGravity = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GRAVITY -> {
                        gravity[0] = event.values[0]; gravity[1] = event.values[1]; gravity[2] = event.values[2]
                        haveGravity = true
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        if (!haveGravity) return
                        val gMag = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
                        if (gMag < 1e-3f) return
                        // up = -gravity/|gravity|; vertical accel (up positive) = linear · up
                        val dot = event.values[0] * gravity[0] + event.values[1] * gravity[1] + event.values[2] * gravity[2]
                        val verticalAccel = -dot / gMag
                        val tsMs = event.timestamp / 1_000_000L
                        if (core.onSample(verticalAccel, tsMs)) trySend(core.count)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (linearAccel != null) sensorManager.registerListener(listener, linearAccel, SensorManager.SENSOR_DELAY_GAME)
        if (gravitySensor != null) sensorManager.registerListener(listener, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
