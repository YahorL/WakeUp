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

class AccelerometerSquatDetector(context: Context) : SquatDetector {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // We feed the DEVIATION of total-acceleration magnitude from gravity (~9.81 m/s², which is
    // orientation-independent) into the counter. A real squat = a descent (dip below downThreshold)
    // followed by the sharp deceleration spike at the bottom (peak above upThreshold). Tuned from
    // on-device traces: real squats spike to +4..+6.5, while merely standing up only reaches ~+2.6,
    // so upThreshold = +3.0 rejects non-squat movement and the spike occurs once per squat.
    private val counter = SquatCounter(downThreshold = -1.5f, upThreshold = 3.0f, minRepIntervalMs = 700)

    override fun repCounts(): Flow<Int> = callbackFlow {
        counter.reset()
        trySend(0)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                val deviation = magnitude - SensorManager.GRAVITY_EARTH // ~9.80665
                val tsMs = event.timestamp / 1_000_000L // ns → ms
                if (counter.onSample(deviation, tsMs)) trySend(counter.count)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
