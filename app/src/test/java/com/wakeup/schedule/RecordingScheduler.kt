package com.wakeup.schedule

import com.wakeup.domain.Alarm

/** Remembers every call so tests can assert on what was (not) scheduled. */
class RecordingScheduler : AlarmScheduler {
    data class Snooze(val alarmId: Long, val triggerAtMillis: Long, val snoozesUsed: Int)

    val scheduled = mutableListOf<Long>()
    val cancelled = mutableListOf<Long>()
    val snoozes = mutableListOf<Snooze>()

    override fun schedule(alarm: Alarm) { scheduled += alarm.id }
    override fun cancel(alarmId: Long) { cancelled += alarmId }
    override fun scheduleSnooze(alarmId: Long, triggerAtMillis: Long, snoozesUsed: Int) {
        snoozes += Snooze(alarmId, triggerAtMillis, snoozesUsed)
    }
}
