package com.wakeup.schedule

import com.wakeup.domain.RepeatDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NextTriggerTest {
    private val zone = ZoneId.of("America/New_York")
    // Wednesday 2026-05-27, 08:00 local.
    private fun now() = ZonedDateTime.of(2026, 5, 27, 8, 0, 0, 0, zone)

    private fun expected(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun oneShotLaterToday() {
        val result = NextTrigger.compute(now(), hour = 9, minute = 0, repeatDays = RepeatDays.NONE)
        assertEquals(expected(2026, 5, 27, 9, 0), result)
    }

    @Test fun oneShotTimeAlreadyPassedRollsToTomorrow() {
        val result = NextTrigger.compute(now(), hour = 7, minute = 0, repeatDays = RepeatDays.NONE)
        assertEquals(expected(2026, 5, 28, 7, 0), result)
    }

    @Test fun repeatingPicksNextMatchingWeekday() {
        // Repeat Mon(0) & Fri(4). Now is Wednesday; next match is Friday 2026-05-29.
        val mask = RepeatDays.toggle(RepeatDays.toggle(RepeatDays.NONE, 0), 4)
        val result = NextTrigger.compute(now(), hour = 6, minute = 30, repeatDays = mask)
        assertEquals(expected(2026, 5, 29, 6, 30), result)
    }

    @Test fun repeatingTodayButTimePassedSkipsToNextWeek() {
        // Repeat Wednesday(2) only, at 07:00 — already passed today, so next Wednesday.
        val mask = RepeatDays.toggle(RepeatDays.NONE, 2)
        val result = NextTrigger.compute(now(), hour = 7, minute = 0, repeatDays = mask)
        assertEquals(expected(2026, 6, 3, 7, 0), result)
    }

    @Test fun repeatingTodayTimeStillAheadFiresToday() {
        val mask = RepeatDays.toggle(RepeatDays.NONE, 2) // Wednesday
        val result = NextTrigger.compute(now(), hour = 23, minute = 0, repeatDays = mask)
        assertEquals(expected(2026, 5, 27, 23, 0), result)
    }
}
