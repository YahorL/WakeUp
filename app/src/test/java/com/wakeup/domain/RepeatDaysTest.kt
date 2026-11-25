package com.wakeup.domain

import org.junit.Assert.*
import org.junit.Test

class RepeatDaysTest {
    @Test fun noneIsOneShot() {
        assertTrue(RepeatDays.isOneShot(RepeatDays.NONE))
    }

    @Test fun toggleSetsAndClearsBit() {
        var mask = RepeatDays.NONE
        mask = RepeatDays.toggle(mask, 0) // Monday
        assertTrue(RepeatDays.isSet(mask, 0))
        assertFalse(RepeatDays.isSet(mask, 1))
        mask = RepeatDays.toggle(mask, 0)
        assertFalse(RepeatDays.isSet(mask, 0))
    }

    @Test fun anySetDayIsNotOneShot() {
        assertFalse(RepeatDays.isOneShot(RepeatDays.toggle(RepeatDays.NONE, 6)))
    }

    @Test fun weekdaysIsMondayToFriday() {
        assertEquals(listOf(0, 1, 2, 3, 4), (0..6).filter { RepeatDays.isSet(RepeatDays.WEEKDAYS, it) })
    }
}
