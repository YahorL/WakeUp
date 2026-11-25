package com.wakeup.ui

import com.wakeup.domain.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NextAlarmTest {
    private val zone = ZoneId.of("America/New_York")
    // Wednesday 2026-05-27, 08:00 local.
    private val now = ZonedDateTime.of(2026, 5, 27, 8, 0, 0, 0, zone)

    @Test fun noAlarmsMeansNoNextAlarm() = assertNull(nextAlarm(emptyList(), now))

    @Test fun disabledAlarmsAreIgnored() =
        assertNull(nextAlarm(listOf(Alarm(id = 1, hour = 9, minute = 0, enabled = false)), now))

    @Test fun picksTheAlarmThatRingsSoonest() {
        val tomorrowMorning = Alarm(id = 1, hour = 6, minute = 30)
        val thisEvening = Alarm(id = 2, hour = 17, minute = 45)
        val disabledSooner = Alarm(id = 3, hour = 8, minute = 30, enabled = false)

        val next = nextAlarm(listOf(tomorrowMorning, thisEvening, disabledSooner), now)

        assertEquals(thisEvening, next?.alarm)
        assertEquals(ZonedDateTime.of(2026, 5, 27, 17, 45, 0, 0, zone).toInstant().toEpochMilli(), next?.triggerAtMillis)
    }
}
