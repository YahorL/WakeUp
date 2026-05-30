package com.wakeup.schedule

import com.wakeup.domain.Alarm

class NoopScheduler : AlarmScheduler {
    override fun schedule(alarm: Alarm) {}
    override fun cancel(alarmId: Long) {}
    override fun scheduleSnooze(alarmId: Long, triggerAtMillis: Long, snoozesUsed: Int) {}
}
