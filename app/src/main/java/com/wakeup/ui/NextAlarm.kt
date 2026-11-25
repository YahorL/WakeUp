package com.wakeup.ui

import com.wakeup.domain.Alarm
import com.wakeup.schedule.NextTrigger
import java.time.ZonedDateTime

/** The enabled alarm that rings soonest, and when. */
data class NextAlarm(val alarm: Alarm, val triggerAtMillis: Long)

fun nextAlarm(alarms: List<Alarm>, now: ZonedDateTime): NextAlarm? =
    alarms.filter { it.enabled }
        .map { NextAlarm(it, NextTrigger.compute(now, it.hour, it.minute, it.repeatDays)) }
        .minByOrNull { it.triggerAtMillis }
