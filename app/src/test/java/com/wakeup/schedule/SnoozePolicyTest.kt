package com.wakeup.schedule

import org.junit.Assert.*
import org.junit.Test

class SnoozePolicyTest {
    @Test fun disabledNeverSnoozes() {
        assertFalse(SnoozePolicy.canSnooze(snoozeEnabled = false, maxSnoozes = 3, snoozesUsed = 0))
    }
    @Test fun unlimitedWhenMaxIsZero() {
        assertTrue(SnoozePolicy.canSnooze(snoozeEnabled = true, maxSnoozes = 0, snoozesUsed = 99))
    }
    @Test fun blockedOnceMaxReached() {
        assertTrue(SnoozePolicy.canSnooze(snoozeEnabled = true, maxSnoozes = 2, snoozesUsed = 1))
        assertFalse(SnoozePolicy.canSnooze(snoozeEnabled = true, maxSnoozes = 2, snoozesUsed = 2))
    }
    @Test fun nextSnoozeTimeAddsMinutes() {
        assertEquals(1_000_000L + 5 * 60_000L, SnoozePolicy.nextSnoozeTime(now = 1_000_000L, snoozeMinutes = 5))
    }
}
