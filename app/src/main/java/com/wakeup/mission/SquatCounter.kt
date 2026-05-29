package com.wakeup.mission

/**
 * Counts squat reps from accelerometer-magnitude samples.
 * A rep = magnitude dips below [downThreshold] then rises above [upThreshold],
 * with at least [minRepIntervalMs] between completed reps.
 */
class SquatCounter(
    private val downThreshold: Float,
    private val upThreshold: Float,
    private val minRepIntervalMs: Long,
) {
    private var sawDip = false
    private var lastRepMs = Long.MIN_VALUE / 2

    var count: Int = 0
        private set

    /** @return true if this sample completed a rep. */
    fun onSample(magnitude: Float, timestampMs: Long): Boolean {
        if (magnitude < downThreshold) {
            sawDip = true
        } else if (magnitude > upThreshold && sawDip) {
            sawDip = false
            if (timestampMs - lastRepMs >= minRepIntervalMs) {
                lastRepMs = timestampMs
                count++
                return true
            }
        }
        return false
    }

    fun reset() {
        count = 0
        sawDip = false
        lastRepMs = Long.MIN_VALUE / 2
    }
}
