package com.wakeup.schedule

import com.wakeup.domain.Alarm

interface AlarmScheduler {
    /** Schedules the alarm's next occurrence. No-op (and cancels) if disabled. */
    fun schedule(alarm: Alarm)
    fun cancel(alarmId: Long)
    /** Schedules a one-off snooze fire for [alarmId] at [triggerAtMillis] carrying [snoozesUsed]. */
    fun scheduleSnooze(alarmId: Long, triggerAtMillis: Long, snoozesUsed: Int)
}
