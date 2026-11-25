package com.wakeup.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OrbDriftTest {
    private val glows = 0..2
    private val samples = (0..200).map { it / 200f }

    @Test fun everyGlowStartsAtItsDesignedPosition() {
        for (i in glows) {
            assertEquals(0f, orbDrift(i, 0f).x, 1e-4f)
            assertEquals(0f, orbDrift(i, 0f).y, 1e-4f)
        }
    }

    @Test fun theLoopRestartsWithoutAJump() {
        for (i in glows) {
            assertEquals(orbDrift(i, 0f).x, orbDrift(i, 1f).x, 1e-4f)
            assertEquals(orbDrift(i, 0f).y, orbDrift(i, 1f).y, 1e-4f)
        }
    }

    @Test fun aGlowNeverLeavesItsDriftDistance() {
        for (i in glows) for (t in samples) {
            val d = orbDrift(i, t)
            assertTrue("glow $i at $t: $d", abs(d.x) <= 1f && abs(d.y) <= 1f)
        }
    }

    @Test fun everyGlowTravelsOnBothAxes() {
        for (i in glows) {
            assertTrue("glow $i barely moves sideways", samples.maxOf { abs(orbDrift(i, it).x) } > 0.9f)
            assertTrue("glow $i barely moves up and down", samples.maxOf { abs(orbDrift(i, it).y) } > 0.9f)
        }
    }

    @Test fun glowsDoNotMoveInUnison() {
        for (a in glows) for (b in glows) {
            if (a >= b) continue
            val apart = samples.maxOf { t -> (orbDrift(a, t) - orbDrift(b, t)).getDistance() }
            assertTrue("glows $a and $b move together", apart > 0.5f)
        }
    }
}
