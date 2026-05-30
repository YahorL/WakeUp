package com.wakeup.mission

import org.junit.Assert.*
import org.junit.Test

class SquatDetectorCoreTest {
    private val dtMs = 20L // 50 Hz

    private fun feed(c: SquatDetectorCore, accel: Float, durationMs: Long, startMs: Long): Long {
        var t = startMs
        val end = startMs + durationMs
        while (t <= end) { c.onSample(accel, t); t += dtMs }
        return t
    }

    /** One squat: accelerate down, sharp decelerate at the bottom (the spike), up, settle at top. */
    private fun squat(c: SquatDetectorCore, startMs: Long, a: Float = 4.0f, phaseMs: Long = 400): Long {
        var t = startMs
        t = feed(c, -a, phaseMs, t)
        t = feed(c, +a, phaseMs, t)
        t = feed(c, +a, phaseMs, t)
        t = feed(c, -a, phaseMs, t)
        return t
    }

    @Test fun realSquatsAreCounted() {
        val c = SquatDetectorCore(minDescentMeters = 0.10f)
        var t = 0L
        repeat(3) { t = squat(c, t + 300) }
        assertTrue("expected >= 2 reps for 3 squats, got ${c.count}", c.count >= 2)
    }

    @Test fun violentJerksAreNotCounted() {
        // Each jerk: a single-sample down spike then up spike — exceeds the dip/peak thresholds
        // (so it IS a spike candidate) but integrates to almost no displacement -> vetoed.
        val c = SquatDetectorCore(minDescentMeters = 0.10f)
        var t = feed(c, 0f, 400, 0)
        repeat(8) {
            c.onSample(-45f, t); t += dtMs
            c.onSample(45f, t); t += dtMs
            t = feed(c, 0f, 250, t)
        }
        assertEquals("count=${c.count} lastDescent=${c.lastDescentMeters}", 0, c.count)
    }

    @Test fun resetClears() {
        val c = SquatDetectorCore(minDescentMeters = 0.10f)
        squat(c, 0)
        c.reset()
        assertEquals(0, c.count)
    }
}
