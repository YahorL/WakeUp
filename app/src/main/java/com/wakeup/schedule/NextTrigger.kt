package com.wakeup.schedule

import com.wakeup.domain.RepeatDays
import java.time.ZonedDateTime

object NextTrigger {
    /**
     * Epoch millis of the next time an alarm with the given time-of-day and repeat mask fires,
     * strictly after [now]. One-shot (mask == 0) fires today if the time is still ahead, else tomorrow.
     */
    fun compute(now: ZonedDateTime, hour: Int, minute: Int, repeatDays: Int): Long {
        val todayAtTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (RepeatDays.isOneShot(repeatDays)) {
            val fire = if (todayAtTime.isAfter(now)) todayAtTime else todayAtTime.plusDays(1)
            return fire.toInstant().toEpochMilli()
        }

        // Scan 0..7 days ahead for a matching weekday whose candidate time is after now.
        for (offset in 0..7) {
            val candidate = todayAtTime.plusDays(offset.toLong())
            val dayIndex = candidate.dayOfWeek.value - 1 // MONDAY(1)->0 .. SUNDAY(7)->6
            if (RepeatDays.isSet(repeatDays, dayIndex) && candidate.isAfter(now)) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        error("Unreachable: a repeating alarm must match within 7 days")
    }
}
