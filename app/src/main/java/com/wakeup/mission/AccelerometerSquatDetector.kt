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

    // Tuned on-device later; rest magnitude ~9.81.
    private val counter = SquatCounter(downThreshold = 7.5f, upThreshold = 11.5f, minRepIntervalMs = 600)

    override fun repCounts(): Flow<Int> = callbackFlow {
        counter.reset()
        trySend(0)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                val tsMs = event.timestamp / 1_000_000L // ns → ms
                if (counter.onSample(magnitude, tsMs)) trySend(counter.count)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
