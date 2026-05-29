package com.wakeup.mission

import org.junit.Assert.assertEquals
import org.junit.Test

class SquatCounterTest {
    private fun counter() = SquatCounter(downThreshold = 7f, upThreshold = 11f, minRepIntervalMs = 500)

    /** Emit one down-up cycle starting at [startMs]; returns ms after the cycle. */
    private fun cycle(c: SquatCounter, startMs: Long): Long {
        c.onSample(9.8f, startMs)        // rest
        c.onSample(6.0f, startMs + 100)  // dip (below down)
        c.onSample(9.8f, startMs + 200)  // mid
        c.onSample(12.0f, startMs + 300) // peak (above up) → rep completes here
        return startMs + 300
    }

    @Test fun countsThreeWellSpacedReps() {
        val c = counter()
        var t = 0L
        repeat(3) { t = cycle(c, t + 600) }
        assertEquals(3, c.count)
    }

    @Test fun ignoresRepsFasterThanDebounce() {
        val c = counter()
        cycle(c, 0)
        cycle(c, 350)
        assertEquals(1, c.count)
    }

    @Test fun jitterWithoutFullDipDoesNotCount() {
        val c = counter()
        c.onSample(9.8f, 0)
        c.onSample(8.0f, 100)
        c.onSample(12.0f, 200)
        assertEquals(0, c.count)
    }

    @Test fun resetClearsCount() {
        val c = counter()
        cycle(c, 0)
        c.reset()
        assertEquals(0, c.count)
    }
}
