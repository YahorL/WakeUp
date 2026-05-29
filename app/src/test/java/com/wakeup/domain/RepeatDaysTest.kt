package com.wakeup.domain

import org.junit.Assert.*
import org.junit.Test

class RepeatDaysTest {
    @Test fun noneIsOneShot() {
        assertTrue(RepeatDays.isOneShot(RepeatDays.NONE))
        assertEquals(emptyList<Int>(), RepeatDays.activeDays(RepeatDays.NONE))
    }

    @Test fun toggleSetsAndClearsBit() {
        var mask = RepeatDays.NONE
        mask = RepeatDays.toggle(mask, 0) // Monday
        assertTrue(RepeatDays.isSet(mask, 0))
        assertFalse(RepeatDays.isSet(mask, 1))
        mask = RepeatDays.toggle(mask, 0)
        assertFalse(RepeatDays.isSet(mask, 0))
    }

    @Test fun activeDaysReturnsSortedIndices() {
        var mask = RepeatDays.NONE
        mask = RepeatDays.toggle(mask, 6) // Sunday
        mask = RepeatDays.toggle(mask, 0) // Monday
        mask = RepeatDays.toggle(mask, 4) // Friday
        assertEquals(listOf(0, 4, 6), RepeatDays.activeDays(mask))
        assertFalse(RepeatDays.isOneShot(mask))
    }
}
