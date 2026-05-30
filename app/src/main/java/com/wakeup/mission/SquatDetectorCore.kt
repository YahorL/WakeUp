package com.wakeup.mission

import kotlin.math.min

/**
 * Counts squats from a stream of world-vertical (up-positive) linear-acceleration samples.
 *
 * Combines two signals so it counts real squats accurately AND ignores cheats:
 *  1. **Spike timing** — a squat bottoms out with a downward dip then a sharp upward deceleration
 *     spike; that dip→peak transition reliably marks one candidate per squat (and also fires on a
 *     jerk).
 *  2. **Displacement veto** — the sample stream is integrated (accel → velocity → displacement) to
 *     the actual vertical distance travelled in the cycle. A genuine squat moves ~knee-bend depth
 *     (~0.12–0.27 m on test traces) while a violent jerk barely moves the phone (≤~0.07 m despite
 *     huge acceleration), so a candidate is only counted when its descent ≥ [minDescentMeters].
 *
 * Drift is bounded by a leaky velocity integrator ([velocityDecay]) and a high-pass of the
 * displacement against a slow [baselineAlpha] baseline.
 */
class SquatDetectorCore(
    private val minDescentMeters: Float = 0.10f,
    private val dipThreshold: Float = -1.5f,
    private val peakThreshold: Float = 3.0f,
    private val minRepIntervalMs: Long = 700,
    private val velocityDecay: Float = 0.995f,
    private val baselineAlpha: Float = 0.01f,
) {
    private var lastT = Long.MIN_VALUE
    private var velocity = 0f
    private var displacement = 0f
    private var baseline = 0f
    private var cycleMin = 0f // most-negative high-passed displacement since the last candidate
    private var sawDip = false
    private var lastCandidateMs = Long.MIN_VALUE / 2

    var count: Int = 0
        private set

    /** Descent distance (m, positive) the last candidate measured — for tuning/inspection. */
    var lastDescentMeters: Float = 0f
        private set

    /** @return true if this sample completed a (genuine) squat rep. */
    fun onSample(verticalAccel: Float, timestampMs: Long): Boolean {
        if (lastT == Long.MIN_VALUE) { lastT = timestampMs; return false }
        val dt = min((timestampMs - lastT) / 1000f, 0.05f)
        lastT = timestampMs
        if (dt <= 0f) return false

        // integrate to a high-passed vertical displacement
        velocity = (velocity + verticalAccel * dt) * velocityDecay
        displacement += velocity * dt
        baseline += baselineAlpha * (displacement - baseline)
        val hp = displacement - baseline
        if (hp < cycleMin) cycleMin = hp

        // spike timing: a downward dip then a sharp upward spike marks a candidate (= squat bottom)
        if (verticalAccel < dipThreshold) {
            sawDip = true
        } else if (verticalAccel > peakThreshold && sawDip) {
            sawDip = false
            if (timestampMs - lastCandidateMs >= minRepIntervalMs) {
                lastCandidateMs = timestampMs
                lastDescentMeters = -cycleMin
                cycleMin = 0f // start measuring the next cycle's descent
                if (lastDescentMeters >= minDescentMeters) {
                    count++
                    return true
                }
            }
        }
        return false
    }

    fun reset() {
        lastT = Long.MIN_VALUE
        velocity = 0f
        displacement = 0f
        baseline = 0f
        cycleMin = 0f
        sawDip = false
        lastCandidateMs = Long.MIN_VALUE / 2
        count = 0
        lastDescentMeters = 0f
    }
}
